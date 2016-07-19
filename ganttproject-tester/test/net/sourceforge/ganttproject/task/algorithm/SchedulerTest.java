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

import biz.ganttproject.core.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskImpl;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
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

  public void test_issue830() throws Exception {
    // The reason of exception being throws was the following task configuration
    //    su mo tu we
    // t0 ==             t0 -> t1 FS
    // t1    ========    t1 is a supertask of t2
    // t2       =====    t2 is a supertask of t3 and t4
    // t3    ==          bounds of t3 and t4 for some reasons are not aligned with t2 bounds
    // t4    ==
    //
    // Scheduler tried to calculate an intersection of t2 dates range and t3+t4 dates range and failed.
    getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);
    Task[] tasks = new Task[] {createTask(TestSetupHelper.newSunday()), createTask(TestSetupHelper.newMonday(), 3), createTask(TestSetupHelper.newWendesday()), createTask(TestSetupHelper.newMonday())};
    TaskDependency[] deps = new TaskDependency[] { createDependency(tasks[1], tasks[0]) };

    DependencyGraph graph = createGraph(tasks, deps);
    DependencyGraphTest.move(tasks[2], tasks[1], graph);
    graph.move(tasks[3], tasks[2]);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();
    assertEquals(TestSetupHelper.newMonday(), tasks[2].getStart());
    assertEquals(TestSetupHelper.newMonday(), tasks[3].getStart());
    assertEquals(TestSetupHelper.newTuesday(), tasks[2].getEnd());
    assertEquals(TestSetupHelper.newTuesday(), tasks[3].getEnd());
  }

  public void testRubberDependency() throws Exception {
    Task[] tasks = new Task[] {createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newWendesday())};
    TaskDependency dep10 = getTaskManager().getDependencyCollection().createDependency(tasks[1], tasks[0], new FinishStartConstraintImpl(), TaskDependency.Hardness.RUBBER);
    TaskDependency dep20 = getTaskManager().getDependencyCollection().createDependency(tasks[2], tasks[0], new FinishStartConstraintImpl(), TaskDependency.Hardness.RUBBER);

    DependencyGraph graph = createGraph(tasks, new TaskDependency[] {dep10, dep20});
    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newMonday(), tasks[0].getStart());
    assertEquals(TestSetupHelper.newTuesday(), tasks[1].getStart());
    assertEquals(TestSetupHelper.newWendesday(), tasks[2].getStart());
  }

  public void testRubberFS_StrongFF() throws Exception {
    // task0->task1 FS rubber, task2->task1 FF strong
    // task0 starts on Mo, task1 initially on Tu, task2 on We
    Task[] tasks = new Task[] {createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newTuesday()), createTask(TestSetupHelper.newWendesday())};
    TaskDependency dep10 = getTaskManager().getDependencyCollection().createDependency(tasks[1], tasks[0], new FinishStartConstraintImpl(), TaskDependency.Hardness.RUBBER);
    TaskDependency dep12 = getTaskManager().getDependencyCollection().createDependency(tasks[1], tasks[2], new FinishFinishConstraintImpl(), TaskDependency.Hardness.STRONG);

    DependencyGraph graph = createGraph(tasks, new TaskDependency[] {dep10, dep12});
    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    // after scheduler run task1 shoud start on We because of strong FF dep
    assertEquals(TestSetupHelper.newMonday(), tasks[0].getStart());
    assertEquals(TestSetupHelper.newWendesday(), tasks[1].getStart());
    assertEquals(TestSetupHelper.newWendesday(), tasks[2].getStart());

    // now shifting task2 to Tu
    tasks[2].shift(getTaskManager().createLength(-1));
    scheduler.run();

    // task1 should follow task2
    assertEquals(TestSetupHelper.newTuesday(), tasks[1].getStart());
    assertEquals(TestSetupHelper.newTuesday(), tasks[2].getStart());
    assertEquals(TestSetupHelper.newWendesday(), tasks[1].getEnd());
  }

  public void testTailHolidayTimeIsIgnored() throws Exception {
    setTaskManager(TestSetupHelper.newTaskManagerBuilder().withCalendar(new WeekendCalendarImpl()).build());
    Task[] tasks = new Task[] {createTask(TestSetupHelper.newFriday()), createTask(TestSetupHelper.newFriday()), createTask(TestSetupHelper.newMonday())};
    tasks[2].setMilestone(true);
    DependencyGraph graph = createGraph(tasks, new TaskDependency[] {createDependency(tasks[2], tasks[1])});
    DependencyGraphTest.move(tasks[1], tasks[0], graph);
    DependencyGraphTest.move(tasks[2], tasks[0], graph);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newSaturday(), tasks[0].getEnd());
    assertEquals(TestSetupHelper.newSaturday(), tasks[1].getEnd());
    assertEquals(TestSetupHelper.newMonday(), tasks[2].getStart());
    assertEquals(TestSetupHelper.newMonday(), tasks[2].getEnd());
  }

  public void testInheritedDependenciesAreWeak() throws Exception {
    // The problem is that if subtasks are not linked with each other then strong
    // dependency drawn to their supertask will create implicit inherited dependencies
    // to subtasks and they will be forced to move to fulfill the dependency.
    //
    // See http://code.google.com/p/ganttproject/issues/detail?id=670
    // for the details, discussion and examples.
    //
    // The solution is to make inherited dependencies weak. This test creates the following structure:
    //
    //    su mo tu
    // t0 ==          t0->t1 FS
    // t1    =====    t1 is a supertask of t2 and t3
    // t2    ==
    // t3       ==
    //
    // and expects that t3 will keep its start date
    Task[] tasks = new Task[] {createTask(TestSetupHelper.newSunday()), createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newTuesday())};
    DependencyGraph graph = createGraph(tasks, new TaskDependency[] {createDependency(tasks[1], tasks[0])});
    DependencyGraphTest.move(tasks[2], tasks[1], graph);
    DependencyGraphTest.move(tasks[3], tasks[1], graph);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newMonday(), tasks[1].getStart());
    assertEquals(TestSetupHelper.newWendesday(), tasks[1].getEnd());
    assertEquals(TestSetupHelper.newMonday(), tasks[2].getStart());
    assertEquals(TestSetupHelper.newTuesday(), tasks[3].getStart());
  }

  public void testEarliestStartLaterThanStartDate() {
    getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);

    // start date on Mo, but earliest start is set to We.
    // task should be shifted forward
    Task[] tasks = new Task[] {createTask(TestSetupHelper.newMonday())};
    tasks[0].setThirdDateConstraint(TaskImpl.EARLIESTBEGIN);
    tasks[0].setThirdDate(TestSetupHelper.newWendesday());
    TaskDependency[] deps = new TaskDependency[0];
    DependencyGraph graph = createGraph(tasks, deps);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newWendesday(), tasks[0].getStart());
  }

  public void testEarliestStartEarlierThanStartDate() {
    getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);

    // start date on Mo, but earliest start is set to Fr (previous week).
    // task should be shifted backwards because there are no other constraints
    Task[] tasks = new Task[] {createTask(TestSetupHelper.newMonday())};
    tasks[0].setThirdDateConstraint(TaskImpl.EARLIESTBEGIN);
    tasks[0].setThirdDate(TestSetupHelper.newFriday());
    TaskDependency[] deps = new TaskDependency[0];
    DependencyGraph graph = createGraph(tasks, deps);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newFriday(), tasks[0].getStart());
  }

  public void testEarliestStartEarlierLosesToDependency() {
    getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);

    // task0 starts on Mo.
    // task1 start date on Tu, earliest start is set to Fr (previous week), and there is
    // a dependency task0->task1 which prevents task1 from moving to the earliest start date
    Task[] tasks = new Task[] {createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newTuesday())};
    tasks[1].setThirdDateConstraint(TaskImpl.EARLIESTBEGIN);
    tasks[1].setThirdDate(TestSetupHelper.newFriday());
    TaskDependency[] deps = new TaskDependency[] {createDependency(tasks[1], tasks[0])};
    DependencyGraph graph = createGraph(tasks, deps);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newTuesday(), tasks[1].getStart());
  }

  public void testEarliestStartLaterWinsToDependency() {
    getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().setEnabled(false);

    // task0 starts on Mo.
    // task1 start date on Tu, earliest start is set to We
    // Despite the dependency task0->task1 earliest start wins and we move task1 forward
    Task[] tasks = new Task[] {createTask(TestSetupHelper.newMonday()), createTask(TestSetupHelper.newTuesday())};
    tasks[1].setThirdDateConstraint(TaskImpl.EARLIESTBEGIN);
    tasks[1].setThirdDate(TestSetupHelper.newWendesday());
    TaskDependency[] deps = new TaskDependency[] {createDependency(tasks[1], tasks[0])};
    DependencyGraph graph = createGraph(tasks, deps);

    SchedulerImpl scheduler = new SchedulerImpl(graph, Suppliers.ofInstance(getTaskManager().getTaskHierarchy()));
    scheduler.run();

    assertEquals(TestSetupHelper.newWendesday(), tasks[1].getStart());
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
