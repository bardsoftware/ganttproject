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

import java.util.List;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;

class AssignmentSaver extends SaverBase {
  void save(IGanttProject project, TransformerHandler handler) throws SAXException {
    startElement("allocations", handler);
    List<HumanResource> resources = project.getHumanResourceManager().getResources();
    for (int i = 0; i < resources.size(); i++) {
      HumanResource resource = resources.get(i);
      ResourceAssignment[] assignments = resource.getAssignments();
      for (int j = 0; j < assignments.length; j++) {
        saveAssignment(handler, assignments[j]);
      }
    }
    endElement("allocations", handler);
  }

  void saveAssignment(TransformerHandler handler, ResourceAssignment next) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    Role roleForAssignment = next.getRoleForAssignment();
    if (roleForAssignment == null) {
      roleForAssignment = next.getResource().getRole();
    }
    addAttribute("task-id", String.valueOf(next.getTask().getTaskID()), attrs);
    addAttribute("resource-id", String.valueOf(next.getResource().getId()), attrs);
    addAttribute("function", roleForAssignment.getPersistentID(), attrs);
    addAttribute("responsible", String.valueOf(next.isCoordinator()), attrs);
    addAttribute("load", String.valueOf(next.getLoad()), attrs);
    emptyElement("allocation", attrs, handler);
  }
}
