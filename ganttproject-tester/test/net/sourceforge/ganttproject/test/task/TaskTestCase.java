package net.sourceforge.ganttproject.test.task;

import biz.ganttproject.core.time.GanttCalendar;
import junit.framework.TestCase;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public abstract class TaskTestCase extends TestCase {
    private TaskManager myTaskManager;

    protected TaskManager getTaskManager() {
        return myTaskManager;
    }

    protected void setTaskManager(TaskManager taskManager) {
      myTaskManager = taskManager;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myTaskManager = newTaskManager();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myTaskManager = null;
    }

    protected TaskManager newTaskManager() {
        return TestSetupHelper.newTaskManagerBuilder().build();
    }

    protected Task createTask() {
        Task result = getTaskManager().createTask();
        result.move(getTaskManager().getRootTask());
        result.setName(String.valueOf(result.getTaskID()));
        return result;
    }

    protected Task createTask(GanttCalendar start) {
      return createTask(start, 1);
    }

    protected Task createTask(GanttCalendar start, int duration) {
      Task result = createTask();
      result.setStart(start);
      result.setDuration(getTaskManager().createLength(duration));
      return result;
    }

    protected TaskDependency createDependency(Task dependant, Task dependee) throws TaskDependencyException {
        return getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
    }
}