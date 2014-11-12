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
import java.util.List;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.algorithm.DependencyGraph;
import net.sourceforge.ganttproject.task.algorithm.RetainRootsAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

/**
 * Predicate which enables or disables move of a collection of tasks (for indent and ourdent operations).
 * Since move targets are different for indent and outdent, this predicate requires a function resolving
 * move target.
 *
 * @author dbarashev
 */
class TaskMoveEnabledPredicate implements Predicate<List<Task>> {
  private final RetainRootsAlgorithm<Task> myRetainRootsAlgorithm = new RetainRootsAlgorithm<Task>();
  private final TaskManager myTaskManager;
  private final Function<Collection<Task>, Function<Task, Task>> myGetMoveTargetFxnFactory;

  TaskMoveEnabledPredicate(TaskManager taskManager, Function<Collection<Task>, Function<Task, Task>> getMoveTargetFxnFactory) {
    myTaskManager = taskManager;
    myGetMoveTargetFxnFactory = getMoveTargetFxnFactory;
  }

  @Override
  public boolean apply(List<Task> selection) {
    if (selection.isEmpty()) {
      return false;
    }
    final TaskContainmentHierarchyFacade taskHierarchy = getTaskManager().getTaskHierarchy();
    Function<Task, Task> getParent = new Function<Task, Task>() {
      @Override
      public Task apply(Task task) {
        return taskHierarchy.getContainer(task);
      }
    };

    // If there are tasks in selection which are in ancestor-descendant relationship,
    // we'll retain only topmost ones.
    List<Task> indentRoots = Lists.newArrayList();
    myRetainRootsAlgorithm.run(selection, getParent, indentRoots);

    // We use dependency graph transaction to test if we get a loop after move.
    // THROWING_LOGGER will throw TaskDependencyException if graph finds a loop.
    DependencyGraph dependencyGraph = getTaskManager().getDependencyGraph();
    DependencyGraph.Logger oldLogger = dependencyGraph.getLogger();
    dependencyGraph.setLogger(DependencyGraph.THROWING_LOGGER);
    dependencyGraph.startTransaction();
    try {
      Function<Task, Task> getParentFxn = myGetMoveTargetFxnFactory.apply(indentRoots);
      for (Task task : indentRoots) {
        Task moveTarget = getParentFxn.apply(task);
        if (moveTarget == null) {
          return false;
        }
        if (moveTarget.isMilestone()) {
          return false;
        }
        dependencyGraph.move(task, moveTarget);
      }
    } catch (TaskDependencyException e) {
      return false;
    } finally {
      dependencyGraph.rollbackTransaction();
      dependencyGraph.setLogger(oldLogger);
    }
    return true;
  }

  private TaskManager getTaskManager() {
    return myTaskManager;
  }
}
