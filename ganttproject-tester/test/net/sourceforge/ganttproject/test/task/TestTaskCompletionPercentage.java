/*
 LICENSE:

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Copyright (C) 2004, GanttProject Development Team
 */
package net.sourceforge.ganttproject.test.task;

import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskCompletionPercentageAlgorithm;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TestTaskCompletionPercentage extends TaskTestCase {
  public void testCompletionIs0WhenAllNestedTasksNotStarted() {
    TaskManager taskManager = getTaskManager();
    Task supertask = taskManager.createTask();
    supertask.setCompletionPercentage(50);
    Task task1 = taskManager.createTask();
    Task task2 = taskManager.createTask();
    Task task3 = taskManager.createTask();
    //
    GanttCalendar commonStart = CalendarFactory.createGanttCalendar(2000, 01, 01);
    GanttCalendar commonEnd = CalendarFactory.createGanttCalendar(2000, 01, 05);
    task1.setStart(commonStart);
    task1.setEnd(commonEnd);
    task2.setStart(commonStart);
    task2.setEnd(commonEnd);
    task3.setStart(commonStart);
    task3.setEnd(commonEnd);
    //
    task1.move(supertask);
    task2.move(supertask);
    task3.move(supertask);
    //
    RecalculateTaskCompletionPercentageAlgorithm alg = taskManager.getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm();
    alg.run(supertask);
    assertEquals("Unexpected completion percentage of supertask=" + supertask, 0, supertask.getCompletionPercentage());

  }

  public void testCompletionIs100WhenAllNestedTasksCompleted() {
    TaskManager taskManager = getTaskManager();
    Task supertask = taskManager.createTask();
    supertask.setCompletionPercentage(50);
    Task task1 = taskManager.createTask();
    Task task2 = taskManager.createTask();
    Task task3 = taskManager.createTask();
    //
    GanttCalendar commonStart = CalendarFactory.createGanttCalendar(2000, 01, 01);
    GanttCalendar commonEnd = CalendarFactory.createGanttCalendar(2000, 01, 05);
    task1.setStart(commonStart);
    task1.setEnd(commonEnd);
    task2.setStart(commonStart);
    task2.setEnd(commonEnd);
    task3.setStart(commonStart);
    task3.setEnd(commonEnd);
    //
    task1.move(supertask);
    task2.move(supertask);
    task3.move(supertask);
    //
    task1.setCompletionPercentage(100);
    task2.setCompletionPercentage(100);
    task3.setCompletionPercentage(100);
    //
    RecalculateTaskCompletionPercentageAlgorithm alg = taskManager.getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm();
    alg.run(supertask);
    assertEquals("Unexpected completion percentage of supertask=" + supertask, 100, supertask.getCompletionPercentage());

  }

  public void testCompletionIs50WhenAllNestedTasksHalfCompleted() {
    TaskManager taskManager = getTaskManager();
    Task supertask = taskManager.createTask();
    supertask.setCompletionPercentage(50);
    Task task1 = taskManager.createTask();
    Task task2 = taskManager.createTask();
    Task task3 = taskManager.createTask();
    //
    GanttCalendar commonStart = CalendarFactory.createGanttCalendar(2000, 01, 01);
    GanttCalendar commonEnd = CalendarFactory.createGanttCalendar(2000, 01, 05);
    task1.setStart(commonStart);
    task1.setEnd(commonEnd);
    task2.setStart(commonStart);
    task2.setEnd(commonEnd);
    task3.setStart(commonStart);
    task3.setEnd(commonEnd);
    //
    task1.move(supertask);
    task2.move(supertask);
    task3.move(supertask);
    //
    task1.setCompletionPercentage(50);
    task2.setCompletionPercentage(50);
    task3.setCompletionPercentage(50);
    //
    RecalculateTaskCompletionPercentageAlgorithm alg = taskManager.getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm();
    alg.run(supertask);
    assertEquals("Unexpected completion percentage of supertask=" + supertask, 50, supertask.getCompletionPercentage());

  }

  public void testCompletionWithMilestones() {
    TaskManager taskManager = getTaskManager();
    Task supertask = taskManager.createTask();
    Task task1 = taskManager.createTask();
    Task task2 = taskManager.createTask();
    task2.setMilestone(true);
    //
    task1.move(supertask);
    task2.move(supertask);
    //
    task1.setCompletionPercentage(50);
    task2.setCompletionPercentage(100);
    //
    RecalculateTaskCompletionPercentageAlgorithm alg = taskManager.getAlgorithmCollection().getRecalculateTaskCompletionPercentageAlgorithm();
    alg.run(supertask);
    assertEquals("Unexpected completion percentage of supertask=" + supertask, 75, supertask.getCompletionPercentage());
  }
}
