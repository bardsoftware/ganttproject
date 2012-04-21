/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.parser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import net.sourceforge.ganttproject.calendar.GPCalendar;

import org.xml.sax.Attributes;

public class DefaultWeekTagHandler implements TagHandler {

  private GPCalendar myGPCalendar;

  private Calendar myCalendar = GregorianCalendar.getInstance(Locale.ENGLISH);

  private SimpleDateFormat myShortFormat = new SimpleDateFormat("EEE", Locale.ENGLISH);

  public DefaultWeekTagHandler(GPCalendar calendar) {
    myGPCalendar = calendar;
    for (int i = 1; i <= 7; i++) {
      myGPCalendar.setWeekDayType(i, GPCalendar.DayType.WORKING);
    }
  }

  @Override
  public void startElement(String namespaceURI, String sName, String qName, Attributes attrs)
      throws FileFormatException {
    if ("default-week".equals(qName)) {
      loadCalendar(attrs);
    }
  }

  private void loadCalendar(Attributes attrs) {
    for (int i = 1; i <= 7; i++) {
      String nextDayName = getShortDayName(i);
      String nextEncodedType = attrs.getValue(nextDayName);
      if ("1".equals(nextEncodedType)) {
        myGPCalendar.setWeekDayType(i, GPCalendar.DayType.WEEKEND);
      }
    }

  }

  private String getShortDayName(int i) {
    myCalendar.set(Calendar.DAY_OF_WEEK, i);
    return myShortFormat.format(myCalendar.getTime()).toLowerCase();
  }

  @Override
  public void endElement(String namespaceURI, String sName, String qName) {
  }

}
