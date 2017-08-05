/*
Copyright 2017 Christoph Schneider, BarD Software s.r.o

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
package net.sourceforge.ganttproject.task.algorithm;

import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;

/**
 * Algorithm for calculating task load
 *
 * @author schch (Christoph Schneider)
 */
public class LoadAlgorithmImpl {
  public Double getCalculatedLoad(Task t) {
    Double total = new Double(0.0);
    TaskContainmentHierarchyFacade taskHierarchy = t.getManager().getTaskHierarchy();
    if (taskHierarchy.hasNestedTasks(t)) {
      for (Task child : taskHierarchy.getNestedTasks(t)) {
        total = total + child.getLoad().getValue();
      }
    }
    for (ResourceAssignment assignment : t.getAssignments()) {
      total = total + assignment.getLoad() * t.getDuration().getLength() / 100.0;
    }
    return total;
  }
}
