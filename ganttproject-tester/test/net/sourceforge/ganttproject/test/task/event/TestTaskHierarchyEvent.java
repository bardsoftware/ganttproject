package net.sourceforge.ganttproject.test.task.event;

import net.sourceforge.ganttproject.test.task.TaskTestCase;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;

public class TestTaskHierarchyEvent extends TaskTestCase {
    public void testEventIsSentOnCreatingNewTask() {
        TaskManager taskManager = getTaskManager();
        TaskListenerImpl listener = new TaskListenerImpl() {
            @Override
            public void taskAdded(TaskHierarchyEvent e) {
                setHasBeenCalled(true);
            }
        };
        taskManager.addTaskListener(listener);
        taskManager.createTask();
        assertTrue("Event taskAdded() is expected to be sent", listener
                .hasBeenCalled());
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
