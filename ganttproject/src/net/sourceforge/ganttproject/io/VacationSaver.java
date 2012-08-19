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
package net.sourceforge.ganttproject.io;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import biz.ganttproject.core.calendar.GanttDaysOff;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.resource.HumanResource;

class VacationSaver extends SaverBase {
  void save(IGanttProject project, TransformerHandler handler) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    startElement("vacations", handler);
    HumanResource[] resources = project.getHumanResourceManager().getResourcesArray();
    for (int i = 0; i < resources.length; i++) {
      HumanResource p = resources[i];
      if (p.getDaysOff() != null)
        for (int j = 0; j < p.getDaysOff().size(); j++) {
          GanttDaysOff gdo = (GanttDaysOff) p.getDaysOff().getElementAt(j);
          addAttribute("start", gdo.getStart().toXMLString(), attrs);
          addAttribute("end", gdo.getFinish().toXMLString(), attrs);
          addAttribute("resourceid", p.getId(), attrs);
          emptyElement("vacation", attrs, handler);
        }
    }
    endElement("vacations", handler);
  }
}
