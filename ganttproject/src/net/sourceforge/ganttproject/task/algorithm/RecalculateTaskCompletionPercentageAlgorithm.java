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
package net.sourceforge.ganttproject.task.algorithm;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;

public abstract class RecalculateTaskCompletionPercentageAlgorithm extends AlgorithmBase {
  @Override
  public void run() {
    if (!isEnabled()) {
      return;
    }
    TaskContainmentHierarchyFacade facade = createContainmentFacade();
    for (Task t : facade.getTasksInDocumentOrder()) {
      if (facade.getDepth(t) == 0) {
        run(t);
      }
    }
  }

  public void run(Task task) {
    if (!isEnabled()) {
      return;
    }
    TaskContainmentHierarchyFacade facade = createContainmentFacade();
    recalculateSupertaskCompletionPercentageBottomUp(task, facade);
  }

  private class PropagatedDays {
    public int myCompletedDays;
    public long myPlannedDays;

    PropagatedDays() {
      myCompletedDays = 0;
      myPlannedDays = 0;
    };

    PropagatedDays(int completedDays, long plannedDays) {
      myCompletedDays = completedDays;
      myPlannedDays = plannedDays;
    };
  }

  private void recalculateSupertaskCompletionPercentageBottomUp(Task task, TaskContainmentHierarchyFacade facade) {
     Task root = facade.getRootTask();
     recalculateSupertaskCompletionPercentage(root, facade);
  }

  private PropagatedDays recalculateSupertaskCompletionPercentage(Task task, TaskContainmentHierarchyFacade facade) {

    Task[] nestedTasks = facade.getNestedTasks(task);

    int completedDays = 0;
    long plannedDays = 0;

    if (nestedTasks.length > 0) {
      for (int i = 0; i < nestedTasks.length; i++) {
        Task next = nestedTasks[i];
        PropagatedDays propagatedDays = recalculateSupertaskCompletionPercentage(next, facade);
        completedDays += propagatedDays.myCompletedDays;
        plannedDays += propagatedDays.myPlannedDays;
      }
      int completionPercentage = plannedDays == 0 ? 0 : (int) (completedDays / plannedDays);
      task.setCompletionPercentage(completionPercentage);
    } else {
      long nextDuration = task.getDuration().getLength();
      completedDays += nextDuration * task.getCompletionPercentage();
      plannedDays += nextDuration;
    }

    return new PropagatedDays(completedDays, plannedDays);
  }


  protected abstract TaskContainmentHierarchyFacade createContainmentFacade();

}
