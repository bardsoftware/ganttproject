/*
Copyright 2014 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.calendar;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.xml.sax.Attributes;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.io.XmlParser;
import net.sourceforge.ganttproject.parser.AbstractTagHandler;
import net.sourceforge.ganttproject.parser.HolidayTagHandler;
import net.sourceforge.ganttproject.parser.ParsingListener;
import net.sourceforge.ganttproject.parser.TagHandler;
import net.sourceforge.ganttproject.util.FileUtil;
import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.calendar.WeekendCalendarImpl;

/**
 * Reads calendars in XML format from GanttProject's installation directory.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GPCalendarProvider {
  private static class CalendarTagHandler extends AbstractTagHandler {
    private WeekendCalendarImpl myCalendar;

    CalendarTagHandler(WeekendCalendarImpl calendar) {
      super("calendar");
      myCalendar = calendar;
    }

    @Override
    public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) {
      if ("calendar".equals(qName)) {
        myCalendar.setName(attrs.getValue("name"));
      }
    }
  }

  private static GPCalendarProvider ourInstance;

  private static GPCalendar readCalendar(File resource) {
    WeekendCalendarImpl calendar = new WeekendCalendarImpl();

    CalendarTagHandler calendarHandler = new CalendarTagHandler(calendar);
    HolidayTagHandler holidayHandler = new HolidayTagHandler(calendar);
    XmlParser parser = new XmlParser(
        ImmutableList.<TagHandler>of(calendarHandler, holidayHandler),
        ImmutableList.<ParsingListener>of(holidayHandler));
    try {
      parser.parse(new BufferedInputStream(new FileInputStream(resource)));
      return calendar;
    } catch (IOException e) {
      GPLogger.logToLogger(e);
      return null;
    }
  }

  private static List<GPCalendar> readCalendars() {
    try {
      URL resolved = Platform.resolve(GPCalendarProvider.class.getResource("/calendar"));
      if (resolved == null) {
        return Collections.emptyList();
      }
      File dir = new File(resolved.toURI());
      if (dir.exists() && dir.isDirectory() && dir.canRead()) {
        List<GPCalendar> calendars = Lists.newArrayList();
        for (File f : dir.listFiles()) {
          if ("calendar".equalsIgnoreCase(FileUtil.getExtension(f))) {
            try {
              GPCalendar calendar = readCalendar(f);
              if (calendar != null) {
                calendars.add(calendar);
              }
            } catch (Throwable e) {
              GPLogger.logToLogger(String.format("Failure when reading calendar file %s", f.getAbsolutePath()));
              GPLogger.logToLogger(e);
            }
          }
        }
        return calendars;
      }
    } catch (IOException | URISyntaxException e) {
      GPLogger.logToLogger(e);
    }
    return Collections.emptyList();
  }

  public static synchronized GPCalendarProvider getInstance() {
    if (ourInstance == null) {
      ourInstance = new GPCalendarProvider(readCalendars());
    }
    return ourInstance;
  }
  private final List<GPCalendar> myCalendars;

  private GPCalendarProvider(List<GPCalendar> calendars) {
    myCalendars = calendars;
  }

  public List<GPCalendar> getCalendars() {
    return myCalendars;
  }
}
