package net.sourceforge.ganttproject.test.task.event;

import net.sourceforge.ganttproject.test.task.TaskTestCase;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.time.gregorian.GregorianTimeUnitStack;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TestTaskScheduleEvent extends TaskTestCase {
    public void testTaskScheduleEventIsSendOnTaskEndChange() {
        TaskManager taskManager = getTaskManager();
        Task task1 = taskManager.createTask();
        TaskListenerImpl listener = new TaskListenerImpl(taskManager);
        taskManager.addTaskListener(listener);
        GanttCalendar taskEnd = task1.getEnd().Clone();
        taskEnd.add(1);
        TaskMutator mutator = task1.createMutator();
        mutator.setEnd(taskEnd);
        mutator.commit();
        assertTrue("Listener has not been called on task end change", listener
                .hasBeenCalled());
    }

    public void testTaskScheduleEventIsSendOnTaskStartChange() {
        TaskManager taskManager = getTaskManager();
        Task task1 = taskManager.createTask();
        TaskListenerImpl listener = new TaskListenerImpl(taskManager);
        taskManager.addTaskListener(listener);
        GanttCalendar taskStart = task1.getStart().Clone();
        taskStart.add(-1);
        TaskMutator mutator = task1.createMutator();
        mutator.setStart(taskStart);
        mutator.commit();
        assertTrue("Listener has not been called on task start change",
                listener.hasBeenCalled());
    }

    public void testTaskScheduleEventIsSendOnTaskDurationChange() {
        TaskManager taskManager = getTaskManager();
        Task task1 = taskManager.createTask();
        TaskListenerImpl listener = new TaskListenerImpl(taskManager);
        taskManager.addTaskListener(listener);
        TaskMutator mutator = task1.createMutator();
        mutator.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                5));
        mutator.commit();
        assertTrue("Listener has not been called on task duration change",
                listener.hasBeenCalled());
    }

    private static class TaskListenerImpl extends TaskListenerAdapter {
        private boolean hasBeenCalled;

        public TaskListenerImpl(TaskManager taskManager) {
            super();
        }

        public void taskScheduleChanged(TaskScheduleEvent e) {
            hasBeenCalled = true;
        }

        boolean hasBeenCalled() {
            return hasBeenCalled;
        }
    }
}
