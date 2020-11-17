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

import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

import org.xml.sax.Attributes;

import biz.ganttproject.core.calendar.GanttDaysOff;
import biz.ganttproject.core.time.GanttCalendar;

/**
 * @author nbohn
 */
public class VacationTagHandler extends AbstractTagHandler {
  private HumanResourceManager myResourceManager;

  public VacationTagHandler(HumanResourceManager resourceManager) {
    super("vacation");
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
      hr = myResourceManager.getById(Integer.parseInt(resourceIdAsString));
      hr.addDaysOff(new GanttDaysOff(GanttCalendar.parseXMLDate(startAsString), GanttCalendar.parseXMLDate(endAsString)));
    } catch (NumberFormatException e) {
      System.out.println("ERROR in parsing XML File year is not numeric: " + e.toString());
      return;
    }
  }

  @Override
  protected boolean onStartElement(Attributes attrs) {
    loadResource(attrs);
    return true;
  }
}
