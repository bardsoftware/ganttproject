package net.sourceforge.ganttproject.test.task;

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

    protected void setUp() throws Exception {
        super.setUp();
        myTaskManager = newTaskManager();
    }

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

    protected TaskDependency createDependency(Task dependant, Task dependee) throws TaskDependencyException {
        return getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
    }
}