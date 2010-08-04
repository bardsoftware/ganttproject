package net.sourceforge.ganttproject.test.task.hierarchy;

import net.sourceforge.ganttproject.test.task.TaskTestCase;
import net.sourceforge.ganttproject.task.Task;

import java.util.Arrays;

public class TestTaskHierarchy extends TaskTestCase {
    public void testCreateSimpleHierarchy() {
        Task task1 = getTaskManager().createTask();
        Task task2 = getTaskManager().createTask();
        task2.move(task1);
        assertEquals("Unexpected supertask of task=" + task2, task1, task2
                .getSupertask());
        assertEquals("Unexpected nested tasks of task=" + task1, Arrays
                .asList(new Task[] { task2 }), Arrays.asList(task1
                .getNestedTasks()));
    }
}
