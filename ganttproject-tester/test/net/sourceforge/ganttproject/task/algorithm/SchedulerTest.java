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
package net.sourceforge.ganttproject.task.algorithm;

import com.google.common.base.Suppliers;

import biz.ganttproject.core.time.GanttCalendar;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

/**
 * Tests scheduling algorithm
 *
 * @author dbarashev
 */
public class SchedulerTest extends TaskTestCase {

  public void testSimpleChain() throws Exception {
    getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);

    Task[] tasks = new Task[] {createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday())};
    TaskDependency[] deps = new TaskDependency[] {createDependency(tasks[2], tasks[1]), createDependency(tasks[1], tasks[0])};
    DependencyGraph graph = createGraph(tasks, deps);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newMonday(), tasks[0].getStart());
    assertEquals(TestSetupHelper.newTuesday(), tasks[1].getStart());
    assertEquals(TestSetupHelper.newWendesday(), tasks[2].getStart());
  }

  public void testIncomingFork() throws Exception {
    getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);
    Task[] tasks = new Task[] {
        createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday()),
        createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday())};
    TaskDependency[] deps = new TaskDependency[] {
        createDependency(tasks[1], tasks[0]), createDependency(tasks[3], tasks[2]), createDependency(tasks[3], tasks[1])};
    DependencyGraph graph = createGraph(tasks, deps);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newMonday(), tasks[0].getStart());
    assertEquals(TestSetupHelper.newMonday(), tasks[2].getStart());
    assertEquals(TestSetupHelper.newTuesday(), tasks[1].getStart());
    assertEquals(TestSetupHelper.newWendesday(), tasks[3].getStart());
  }

  public void testRhombus() throws Exception {
    getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);
    Task[] tasks = new Task[] {
        createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday()),
        createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday())};
    TaskDependency[] deps = new TaskDependency[] {
        createDependency(tasks[4], tasks[3]),
        createDependency(tasks[4], tasks[2]),
        createDependency(tasks[2], tasks[1]),
        createDependency(tasks[1], tasks[0]),
        createDependency(tasks[3], tasks[0])
    };
    DependencyGraph graph = createGraph(tasks, deps);
    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newMonday(), tasks[0].getStart());
    assertEquals(TestSetupHelper.newTuesday(), tasks[1].getStart());
    assertEquals(TestSetupHelper.newTuesday(), tasks[3].getStart());
    assertEquals(TestSetupHelper.newWendesday(), tasks[2].getStart());
    assertEquals(TestSetupHelper.newThursday(), tasks[4].getStart());
  }

  public void testChainOfSubtasks() throws Exception {
    getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);
    Task[] tasks = new Task[] {
        createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday())};
    getTaskManager().getTaskHierarchy().move(tasks[1], tasks[0]);
    getTaskManager().getTaskHierarchy().move(tasks[2], tasks[0]);
    TaskDependency[] deps = new TaskDependency[] {createDependency(tasks[2], tasks[1])};
    DependencyGraph graph = createGraph(tasks, deps);
    graph.move(tasks[1], tasks[0]);
    graph.move(tasks[2], tasks[0]);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newMonday(), tasks[0].getStart());
    assertEquals(TestSetupHelper.newMonday(), tasks[1].getStart());
    assertEquals(TestSetupHelper.newTuesday(), tasks[2].getStart());
    assertEquals(TestSetupHelper.newWendesday(), tasks[2].getEnd());
    assertEquals(TestSetupHelper.newWendesday(), tasks[0].getEnd());
  }

  private Task createTask(GanttCalendar start) {
    Task result = createTask();
    result.setStart(start);
    result.setDuration(getTaskManager().createLength(1));
    return result;
  }

  private DependencyGraph createGraph(Task[] tasks, TaskDependency[] deps) {
    DependencyGraph graph = new DependencyGraph(Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    initGraph(graph, tasks, deps);
    return graph;
  }

  private static void initGraph(DependencyGraph graph, Task[] tasks, TaskDependency[] deps) {
    if (tasks != null) {
      for (Task t : tasks) {
        graph.addTask(t);
      }
    }
    if (deps != null) {
      for (TaskDependency d : deps) {
        graph.addDependency(d);
      }
    }
  }

}
