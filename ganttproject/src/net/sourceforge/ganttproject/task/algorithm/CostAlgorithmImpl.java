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
package net.sourceforge.ganttproject.task.algorithm;

import java.math.BigDecimal;

import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;

/**
 * Algorithm for calculating task cost
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CostAlgorithmImpl {
  public BigDecimal getCalculatedCost(Task t) {
    BigDecimal total = BigDecimal.ZERO;
    TaskContainmentHierarchyFacade taskHierarchy = t.getManager().getTaskHierarchy();
    if (taskHierarchy.hasNestedTasks(t)) {
      for (Task child : taskHierarchy.getNestedTasks(t)) {
        total = total.add(child.getCost().getValue());
      }
    }
    for (ResourceAssignment assignment : t.getAssignments()) {
      HumanResource resource = assignment.getResource();
      total = total.add(resource.getStandardPayRate()
          .multiply(BigDecimal.valueOf(assignment.getLoad()))
          .divide(BigDecimal.valueOf(100))
          .multiply(BigDecimal.valueOf(t.getDuration().getLength())));
    }
    return total;
  }
}
