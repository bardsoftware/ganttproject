/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.parser;


import org.xml.sax.Attributes;

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;

/**
 * @author nbohn
 */
public class HolidayTagHandler implements TagHandler, ParsingListener {
  private final GPCalendar myCalendar;

  public HolidayTagHandler(GPCalendar calendar) {
    myCalendar = calendar;
    myCalendar.clearPublicHolidays();
  }

  /**
   * @see net.sourceforge.ganttproject.parser.TagHandler#endElement(String,
   *      String, String)
   */
  @Override
  public void endElement(String namespaceURI, String sName, String qName) {
  }

  /**
   * @see net.sourceforge.ganttproject.parser.TagHandler#startElement(String,
   *      String, String, Attributes)
   */
  @Override
  public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) {
    if (qName.equals("date")) {
      loadHoliday(attrs);
    }
  }

  private void loadHoliday(Attributes atts) {
    try {
      String yearAsString = atts.getValue("year");
      String monthAsString = atts.getValue("month");
      String dateAsString = atts.getValue("date");
      int month = Integer.parseInt(monthAsString);
      int date = Integer.parseInt(dateAsString);
      if (yearAsString.equals("")) {
        myCalendar.setPublicHoliDayType(month, date);
      } else {
        int year = Integer.parseInt(yearAsString);
        myCalendar.setPublicHoliDayType(CalendarFactory.createGanttCalendar(year, month - 1, date).getTime());
      }
    } catch (NumberFormatException e) {
      System.out.println("ERROR in parsing XML File year is not numeric: " + e.toString());
      return;
    }

  }

  @Override
  public void parsingStarted() {
    // TODO Auto-generated method stub
  }

  @Override
  public void parsingFinished() {
    // TODO Auto-generated method stub
  }
}
