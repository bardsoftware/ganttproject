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
import net.sourceforge.ganttproject.calendar.GanttDaysOff;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

import org.xml.sax.Attributes;

/**
 * @author nbohn
 */
public class VacationTagHandler implements TagHandler, ParsingListener {
    private HumanResourceManager myResourceManager;

    public VacationTagHandler(HumanResourceManager resourceManager) {
        myResourceManager = resourceManager;
    }

    private void loadResource(Attributes atts) {
        try {
            // <vacation start="2005-04-14" end="2005-04-14" resourceid="0"/>
            // GanttCalendar.parseXMLDate(attrs.getValue(i)).getTime()

            String startAsString = atts.getValue("start");
            String endAsString = atts.getValue("end");
            String resourceIdAsString = atts.getValue("resourceid");
            HumanResource hr;
            hr = myResourceManager
                    .getById(Integer.parseInt(resourceIdAsString));
            hr.addDaysOff(new GanttDaysOff(GanttCalendar
                    .parseXMLDate(startAsString), GanttCalendar
                    .parseXMLDate(endAsString)));
        } catch (NumberFormatException e) {
            System.out
                    .println("ERROR in parsing XML File year is not numeric: "
                            + e.toString());
            return;
        }
    }

    @Override
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {
        if (qName.equals("vacation")) {
            loadResource(attrs);
        }
    }

    @Override
    public void endElement(String namespaceURI, String sName, String qName) {
    }

    @Override
    public void parsingStarted() {
    }

    @Override
    public void parsingFinished() {
    }

}
