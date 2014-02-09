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

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

import com.google.common.base.Suppliers;

/**
 * Tests dependency graph behavior
 *
 * @author dbarashev
 */
public class DependencyGraphTest extends TaskTestCase {
  public void testSimpleChain() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask()};
    TaskDependency[] deps = new TaskDependency[] {createDependency(tasks[2], tasks[1]), createDependency(tasks[1], tasks[0])};
    DependencyGraph graph = createGraph(tasks, deps);

    assertEquals(2, graph.getNode(tasks[2]).getLevel());
    assertEquals(1, graph.getNode(tasks[1]).getLevel());
    assertEquals(0, graph.getNode(tasks[0]).getLevel());
  }

  public void testIncomingFork() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask(), createTask()};
    TaskDependency[] deps = new TaskDependency[] {
        createDependency(tasks[1], tasks[0]), createDependency(tasks[3], tasks[2]), createDependency(tasks[3], tasks[1])};
    DependencyGraph graph = createGraph(tasks, deps);

    assertEquals(2, graph.getNode(tasks[3]).getLevel());
    assertEquals(1, graph.getNode(tasks[1]).getLevel());
    assertEquals(0, graph.getNode(tasks[0]).getLevel());
    assertEquals(0, graph.getNode(tasks[2]).getLevel());
  }

  public void testRhombus() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask(), createTask(), createTask()};
    TaskDependency[] deps = new TaskDependency[] {
        createDependency(tasks[4], tasks[3]),
        createDependency(tasks[4], tasks[2]),
        createDependency(tasks[2], tasks[1]),
        createDependency(tasks[1], tasks[0]),
        createDependency(tasks[3], tasks[0])
    };
    DependencyGraph graph = createGraph(tasks, deps);

    assertEquals(3, graph.getNode(tasks[4]).getLevel());
    assertEquals(2, graph.getNode(tasks[2]).getLevel());
    assertEquals(1, graph.getNode(tasks[3]).getLevel());
    assertEquals(1, graph.getNode(tasks[1]).getLevel());
    assertEquals(0, graph.getNode(tasks[0]).getLevel());
  }

  public void testRemoveDependencyWithPropagation() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask()};
    TaskDependency[] deps = new TaskDependency[] {createDependency(tasks[2], tasks[1]), createDependency(tasks[1], tasks[0])};
    DependencyGraph graph = createGraph(tasks, deps);
    assertEquals(2, graph.getNode(tasks[2]).getLevel());

    graph.removeDependency(deps[1]);
    assertEquals(0, graph.getNode(tasks[1]).getLevel());
    assertEquals(1, graph.getNode(tasks[2]).getLevel());
  }

  public void testRemoveDependencyWithNoPropagation() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask(), createTask()};
    TaskDependency[] deps = new TaskDependency[] {createDependency(tasks[3], tasks[2]), createDependency(tasks[2], tasks[1]), createDependency(tasks[2], tasks[0])};
    DependencyGraph graph = createGraph(tasks, deps);
    assertEquals(2, graph.getNode(tasks[3]).getLevel());

    graph.removeDependency(deps[2]);
    assertEquals(2, graph.getNode(tasks[3]).getLevel());
  }

  public void testRemoveInheritedDependencies() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask()};
    DependencyGraph graph = createGraph(tasks, null);
    move(tasks[2], tasks[1], graph);
    TaskDependency dep = createDependency(tasks[1], tasks[0]);
    graph.addDependency(dep);
    assertEquals(1, graph.getNode(tasks[2]).getLevel());

    graph.removeDependency(dep);
    assertEquals(0, graph.getNode(tasks[2]).getLevel());
  }

  public static void move(Task what, Task where, DependencyGraph graph) {
    where.getManager().getTaskHierarchy().move(what, where);
    graph.move(what, where);
  }

  public void testRemoveNode() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask()};
    TaskDependency[] deps = new TaskDependency[] {createDependency(tasks[2], tasks[1]), createDependency(tasks[1], tasks[0])};
    DependencyGraph graph = createGraph(tasks, deps);

    assertEquals(2, graph.getNode(tasks[2]).getLevel());

    graph.removeTask(tasks[1]);
    assertEquals(0, graph.getNode(tasks[2]).getLevel());
  }

  public void testSimpleSubtask() {
    Task[] tasks = new Task[] {createTask(), createTask()};
    DependencyGraph graph = createGraph(tasks, null);
    getTaskManager().getTaskHierarchy().move(tasks[1], tasks[0]);
    graph.move(tasks[1], tasks[0]);
    assertEquals(1, graph.getNode(tasks[0]).getLevel());
  }

  public void testChainOfSubtasks() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask()};
    getTaskManager().getTaskHierarchy().move(tasks[1], tasks[0]);
    getTaskManager().getTaskHierarchy().move(tasks[2], tasks[0]);
    TaskDependency[] deps = new TaskDependency[] {createDependency(tasks[2], tasks[1])};
    DependencyGraph graph = createGraph(tasks, deps);

    assertEquals(0, graph.getNode(tasks[0]).getLevel());
    graph.move(tasks[1], tasks[0]);
    assertEquals(1, graph.getNode(tasks[0]).getLevel());
    graph.move(tasks[2], tasks[0]);
    assertEquals(2, graph.getNode(tasks[0]).getLevel());
  }

  public void testInheritedDependencies() throws Exception {
    // initially there is task0->task1 dep
    Task[] tasks = new Task[] {createTask(), createTask(), createTask(), createTask(), createTask()};
    TaskDependency[] initialDeps = new TaskDependency[] { createDependency(tasks[1], tasks[0])};
    DependencyGraph graph = createGraph(tasks, initialDeps);
    assertEquals(1, graph.getNode(tasks[1]).getLevel());

    // lets move task2 into task1. It gets an implicit inherited dependency task0->task2
    getTaskManager().getTaskHierarchy().move(tasks[2], tasks[1]);
    graph.move(tasks[2], tasks[1]);
    assertEquals(1, graph.getNode(tasks[2]).getLevel());
    assertEquals(2, graph.getNode(tasks[1]).getLevel());

    // lets move task4 into task 3
    getTaskManager().getTaskHierarchy().move(tasks[4], tasks[3]);
    graph.move(tasks[4], tasks[3]);
    assertEquals(0, graph.getNode(tasks[4]).getLevel());
    assertEquals(1, graph.getNode(tasks[3]).getLevel());

    // lets create a dependency task2->task3
    // task4 should get an implicit inherited dependency task2->task4
    graph.addDependency(createDependency(tasks[3], tasks[2]));
    assertEquals(2, graph.getNode(tasks[4]).getLevel());
    assertEquals(3, graph.getNode(tasks[3]).getLevel());
  }

  public void testDeepInheritedDependencies() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask(), createTask()};
    DependencyGraph graph = createGraph(tasks, null);
    getTaskManager().getTaskHierarchy().move(tasks[3], tasks[2]);
    getTaskManager().getTaskHierarchy().move(tasks[2], tasks[1]);
    graph.move(tasks[3], tasks[2]);
    graph.move(tasks[2], tasks[1]);
    assertEquals(2, graph.getNode(tasks[1]).getLevel());

    graph.addDependency(createDependency(tasks[1], tasks[0]));
    assertEquals(3, graph.getNode(tasks[1]).getLevel());
    assertEquals(1, graph.getNode(tasks[3]).getLevel());
    assertEquals(2, graph.getNode(tasks[2]).getLevel());

    Task task4 = createTask();
    Task task5 = createTask();
    getTaskManager().getTaskHierarchy().move(task5, task4);
    graph.addTask(task4);
    graph.addTask(task5);
    graph.move(task5, task4);
    assertEquals(0, graph.getNode(task5).getLevel());

    getTaskManager().getTaskHierarchy().move(task4, tasks[1]);
    graph.move(task4, tasks[1]);
    assertEquals(1, graph.getNode(task5).getLevel());
    assertEquals(graph.getNode(tasks[0]), graph.getNode(task5).getIncoming().get(0).getSrc());
  }

  public void testRemoveSubtaskRemovesImplicitSubSuperTaskDependencies() {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask()};
    DependencyGraph graph = createGraph(tasks, null);
    getTaskManager().getTaskHierarchy().move(tasks[2], tasks[1]);
    getTaskManager().getTaskHierarchy().move(tasks[1], tasks[0]);
    graph.move(tasks[2], tasks[1]);
    graph.move(tasks[1], tasks[0]);
    assertEquals(2, graph.getNode(tasks[0]).getLevel());
    assertEquals(1, graph.getNode(tasks[1]).getLevel());
    assertEquals(0, graph.getNode(tasks[2]).getLevel());

    getTaskManager().getTaskHierarchy().move(tasks[1], getTaskManager().getRootTask());
    graph.move(tasks[1], null);

    assertEquals(0, graph.getNode(tasks[0]).getLevel());
    assertEquals(1, graph.getNode(tasks[1]).getLevel());
    assertEquals(0, graph.getNode(tasks[2]).getLevel());
  }

  public void testRemoveSubtaskAndImplicitInheritedDependencies() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask(), createTask(), createTask()};
    DependencyGraph graph = createGraph(tasks, null);
    move(tasks[2], tasks[1], graph);
    move(tasks[1], tasks[0], graph);

    graph.addDependency(createDependency(tasks[0], tasks[3]));
    graph.addDependency(createDependency(tasks[1], tasks[3]));
    assertEquals(3, graph.getNode(tasks[0]).getLevel());

    getTaskManager().getTaskHierarchy().move(tasks[1], getTaskManager().getRootTask());
    graph.move(tasks[1], null);

    assertEquals(1, graph.getNode(tasks[0]).getLevel());
    assertEquals(2, graph.getNode(tasks[1]).getLevel());
    assertEquals(1, graph.getNode(tasks[2]).getLevel());
    assertEquals(0, graph.getNode(tasks[3]).getLevel());
  }

  public void testTransactionRollback() throws Exception {
    Task[] tasks = new Task[] {createTask(), createTask()};
    DependencyGraph graph = createGraph(tasks, null);

    // We start a transaction and create a dependency
    graph.startTransaction();

    graph.addDependency(createDependency(tasks[0], tasks[1]));
    assertEquals(2, graph.checkLayerValidity());
    assertEquals(1, graph.getNode(tasks[0]).getLevel());
    assertEquals(1, graph.getNode(tasks[0]).getIncoming().size());

    assertEquals(0, graph.getNode(tasks[1]).getLevel());
    assertEquals(1, graph.getNode(tasks[1]).getOutgoing().size());

    // Now we rollback transaction and expect that its changes are undone
    graph.rollbackTransaction();

    assertEquals(1, graph.checkLayerValidity());
    assertEquals(0, graph.getNode(tasks[0]).getLevel());
    assertEquals(0, graph.getNode(tasks[1]).getLevel());
    assertTrue(graph.getNode(tasks[0]).getIncoming().isEmpty());
    assertTrue(graph.getNode(tasks[1]).getOutgoing().isEmpty());
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
