/*
Copyright 2012 GanttProject Team

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
package net.sourceforge.ganttproject.action.task;

import java.util.Collection;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;

import com.google.common.base.Function;

/**
 * Creates functions resolving move target for indent operations. Function depends on the set of tasks being moved so
 * it is different every time
 *
 * @author dbarashev
 */
class IndentTargetFunctionFactory implements Function<Collection<Task>, Function<Task, Task>> {
  private final TaskManager myTaskManager;
  IndentTargetFunctionFactory(TaskManager taskManager) {
    myTaskManager = taskManager;
  }

  @Override
  public Function<Task, Task> apply(final Collection<Task> indentRoots) {
    return new Function<Task, Task>() {
      private final TaskContainmentHierarchyFacade myTaskHierarchy = myTaskManager.getTaskHierarchy();

      @Override
      public Task apply(Task whatMove) {
        Task moveTarget = whatMove;
        for (; moveTarget != null && indentRoots.contains(moveTarget);
            moveTarget = myTaskHierarchy.getPreviousSibling(moveTarget));
        return moveTarget;
      }
    };
  }
};