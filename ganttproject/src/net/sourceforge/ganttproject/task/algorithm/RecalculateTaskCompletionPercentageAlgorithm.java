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
    recalculateSupertaskCompletionPercentage(facade.getRootTask(), facade);
  }

  static private class SubtreeCompletion {
    public final long myCompletedDays;
    public final long myPlannedDays;

    SubtreeCompletion(long completedDays, long plannedDays) {
      myCompletedDays = completedDays;
      myPlannedDays = plannedDays;
    };
  }

  private SubtreeCompletion recalculateSupertaskCompletionPercentage(Task task, TaskContainmentHierarchyFacade facade) {

    Task[] nestedTasks = facade.getNestedTasks(task);

    if (nestedTasks.length == 0) {
      long nextDuration = task.getDuration().getLength();
      return new SubtreeCompletion(nextDuration * task.getCompletionPercentage(), nextDuration);
    }

    long completedDays = 0;
    long plannedDays = 0;

    for (int i = 0; i < nestedTasks.length; i++) {
      Task next = nestedTasks[i];
      SubtreeCompletion subtreeCompletion = recalculateSupertaskCompletionPercentage(next, facade);
      completedDays += subtreeCompletion.myCompletedDays;
      plannedDays += subtreeCompletion.myPlannedDays;
    }

    int completionPercentage = (plannedDays == 0) ? 0 : (int) (completedDays / plannedDays);
    task.setCompletionPercentage(completionPercentage);

    return new SubtreeCompletion(completedDays, plannedDays);

  }

  protected abstract TaskContainmentHierarchyFacade createContainmentFacade();

}