/*
 LICENSE:

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Copyright (C) 2004, GanttProject Development Team
 */
package net.sourceforge.ganttproject.test.task.event;

import net.sourceforge.ganttproject.test.task.TaskTestCase;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskDependencyEvent;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TestTaskDependencyEvent extends TaskTestCase {
    public void testDependencyEventIsSentOnDependencyCreation()
            throws TaskDependencyException {
        TaskManager taskManager = getTaskManager();
        TaskListenerImpl listener = new TaskListenerImpl() {
            @Override
            public void dependencyAdded(TaskDependencyEvent e) {
                setHasBeenCalled(true);
            }
        };
        taskManager.addTaskListener(listener);
        Task task1 = taskManager.createTask();
        Task task2 = taskManager.createTask();
        //
        taskManager.getDependencyCollection().createDependency(task2, task1,
                new FinishStartConstraintImpl());
        assertTrue(
                "Listener is expected to be called when dependency is added",
                listener.hasBeenCalled());
    }

    public void testDependencyEventIsSentOnDependencyRemoval()
            throws TaskDependencyException {
        TaskManager taskManager = getTaskManager();
        TaskListenerImpl listener = new TaskListenerImpl() {
            @Override
            public void dependencyRemoved(TaskDependencyEvent e) {
                setHasBeenCalled(true);
            }
        };
        taskManager.addTaskListener(listener);
        Task task1 = taskManager.createTask();
        Task task2 = taskManager.createTask();
        //
        TaskDependency dep = taskManager
                .getDependencyCollection()
                .createDependency(task2, task1, new FinishStartConstraintImpl());
        dep.delete();
        assertTrue(
                "Listener is expected to be called when dependency is deleted",
                listener.hasBeenCalled());
    }

    private static class TaskListenerImpl extends TaskListenerAdapter {
        private boolean hasBeenCalled;

        boolean hasBeenCalled() {
            return hasBeenCalled;
        }

        protected void setHasBeenCalled(boolean called) {
            hasBeenCalled = called;
        }
    }
}
