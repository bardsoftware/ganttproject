/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.IGanttProject;

import org.xml.sax.Attributes;

/**
 * @author nbohn
 */
public class HolidayTagHandler implements TagHandler, ParsingListener {
    private IGanttProject project;

    public HolidayTagHandler(IGanttProject project) {
        this.project = project;
        project.getActiveCalendar().clearPublicHolidays();
    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#endElement(String,
     *      String, String)
     */
    public void endElement(String namespaceURI, String sName, String qName) {
    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#startElement(String,
     *      String, String, Attributes)
     */
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {
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
                project.getActiveCalendar().setPublicHoliDayType(month, date);
            } else {
                int year = Integer.parseInt(yearAsString);
                project.getActiveCalendar().setPublicHoliDayType(
                        new GanttCalendar(year, month - 1, date).getTime());
            }
        } catch (NumberFormatException e) {
            System.out
                    .println("ERROR in parsing XML File year is not numeric: "
                            + e.toString());
            return;
        }

    }

    public void parsingStarted() {
        // TODO Auto-generated method stub
    }

    public void parsingFinished() {
        // TODO Auto-generated method stub
    }
}
