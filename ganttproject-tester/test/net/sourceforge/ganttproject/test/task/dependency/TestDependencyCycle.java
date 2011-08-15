/*
 * Created on 12.11.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject.test.task.dependency;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

public class TestDependencyCycle extends TaskTestCase {
    public void testSimpleCycle() throws Exception {
        Task dependant = getTaskManager().createTask();
        Task dependee = getTaskManager().createTask();
        getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
        assertIsLooping(dependee, dependant);
    }

    public void testLoopingDependencyTargetedAtSupertask() throws Exception {
        Task supertask = getTaskManager().createTask(); supertask.setName("supertask");
        Task nestedTask = getTaskManager().createTask(); nestedTask.setName("nestedtask");
        nestedTask.move(supertask);
        Task dependantTask = getTaskManager().createTask();
        dependantTask.setName("dependanttask");
        getTaskManager().getDependencyCollection().createDependency(dependantTask, nestedTask);
        assertIsLooping(supertask, dependantTask);
    }

    public void testDependencyTargetedToNestedTask() throws Exception {
        Task supertask = getTaskManager().createTask();
        Task nestedTask = getTaskManager().createTask();
        nestedTask.move(supertask);
        assertIsLooping(supertask, nestedTask);
    }

    private void assertIsLooping(Task dependant, Task dependee) {
        boolean loopCreated = true;
        try {
            TaskDependency loopingDependency = getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
            assertNotNull("Either exception is thrown, or result is not null", loopingDependency);
        }
        catch (TaskDependencyException e) {
            // An exception is thrown if the loop is prevented/detected (which is good behavior)
            loopCreated = false;
        }
        assertFalse("Dependency loop has been successfully created...", loopCreated);
    }
}
