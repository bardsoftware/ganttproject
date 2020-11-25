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

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.WeekendCalendarImpl;
import com.google.common.collect.ImmutableList;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.io.XmlParser;
import net.sourceforge.ganttproject.parser.AbstractTagHandler;
import net.sourceforge.ganttproject.parser.HolidayTagHandler;
import net.sourceforge.ganttproject.parser.ParsingListener;
import net.sourceforge.ganttproject.parser.TagHandler;
import org.xml.sax.Attributes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Reads calendars in XML format from GanttProject's installation directory.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GPCalendarProvider {
  private static class CalendarTagHandler extends AbstractTagHandler {
    private final GPCalendarCalc myCalendar;
    private final HolidayTagHandler myHolidayHandler;

    CalendarTagHandler(GPCalendarCalc calendar, HolidayTagHandler holidayHandler) {
      super("calendar");
      myCalendar = calendar;
      myHolidayHandler = holidayHandler;
    }

    @Override
    protected boolean onStartElement(Attributes attrs) {
      myCalendar.setName(attrs.getValue("name"));
      myCalendar.setID(attrs.getValue("id"));
      myCalendar.setBaseCalendarID(attrs.getValue("base-id"));
      return true;
    }

    @Override
    protected void onEndElement() {
      myHolidayHandler.onCalendarLoaded();
    }
  }

  private static GPCalendarProvider ourInstance;

  static GPCalendar readCalendar(File resource) {
    WeekendCalendarImpl calendar = new WeekendCalendarImpl();

    HolidayTagHandler holidayHandler = new HolidayTagHandler(calendar);
    CalendarTagHandler calendarHandler = new CalendarTagHandler(calendar, holidayHandler);
    XmlParser parser = new XmlParser(
        ImmutableList.<TagHandler>of(calendarHandler, holidayHandler),
        ImmutableList.<ParsingListener>of());
    try {
      parser.parse(new BufferedInputStream(new FileInputStream(resource)));
      return calendar;
    } catch (IOException e) {
      GPLogger.logToLogger(e);
      return null;
    }
  }

  private static List<GPCalendar> readCalendars() {
    return HolidayCalendarKt.loadCalendars();
  }

  public static synchronized GPCalendarProvider getInstance() {
    if (ourInstance == null) {
      List<GPCalendar> calendars = readCalendars();
      Collections.sort(calendars, new Comparator<GPCalendar>() {
        public int compare(GPCalendar o1, GPCalendar o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      ourInstance = new GPCalendarProvider(calendars);
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
