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

import com.google.common.collect.ImmutableList;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.algorithm.DependencyGraphTest;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

/**
 * Tests for predicate which enables or disables task move
 *
 * @author dbarashev
 */
public class TaskMoveEnabledPredicateTest extends TaskTestCase {

  public void testSimpleIndent() {
    Task[] tasks = new Task[] {createTask(), createTask()};
    TaskMoveEnabledPredicate predicate = new TaskMoveEnabledPredicate(getTaskManager(), new IndentTargetFunctionFactory(getTaskManager()));
    assertTrue(predicate.apply(ImmutableList.of(tasks[1])));
    assertFalse(predicate.apply(ImmutableList.of(tasks[0])));
    assertFalse(predicate.apply(ImmutableList.of(tasks[0], tasks[1])));
  }

  public void testIndentLinkedTasks() {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask()};
    getTaskManager().getDependencyGraph().addDependency(createDependency(tasks[2], tasks[1]));

    TaskMoveEnabledPredicate predicate = new TaskMoveEnabledPredicate(getTaskManager(), new IndentTargetFunctionFactory(getTaskManager()));
    assertTrue(predicate.apply(ImmutableList.of(tasks[1], tasks[2])));
    assertFalse(predicate.apply(ImmutableList.of(tasks[2])));
  }

  public void testSimpleOutdent() {
    Task[] tasks = new Task[] {createTask(), createTask()};
    DependencyGraphTest.move(tasks[1], tasks[0], getTaskManager().getDependencyGraph());

    TaskMoveEnabledPredicate predicate = new TaskMoveEnabledPredicate(getTaskManager(), new OutdentTargetFunctionFactory(getTaskManager()));
    assertTrue(predicate.apply(ImmutableList.of(tasks[1])));
    assertFalse(predicate.apply(ImmutableList.of(tasks[0])));
    assertFalse(predicate.apply(ImmutableList.of(tasks[0], tasks[1])));
  }
}
