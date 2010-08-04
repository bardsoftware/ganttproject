/*
 * Created on 24.02.2005
 */
package net.sourceforge.ganttproject.test.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * @author bard
 */
public class ImportTasksTestCase extends TaskTestCase {
    public void testImportingPreservesIDs() {
        TaskManager taskManager = getTaskManager();
        {
            Task root = taskManager.getTaskHierarchy().getRootTask();
            Task[] nestedTasks = taskManager.getTaskHierarchy().getNestedTasks(
                    root);
            assertEquals(
                    "Unexpected count of the root's children BEFORE importing",
                    0, nestedTasks.length);
        }
        TaskManager importFrom = newTaskManager();
        {
            Task importRoot = importFrom.getTaskHierarchy().getRootTask();
            importFrom.createTask(2).move(importRoot);
            importFrom.createTask(3).move(importRoot);
        }
        //
        taskManager.importData(importFrom);
        {
            Task root = taskManager.getTaskHierarchy().getRootTask();
            Task[] nestedTasks = taskManager.getTaskHierarchy().getNestedTasks(
                    root);
            assertEquals(
                    "Unexpected count of the root's children AFTER importing. root="
                            + root, 2, nestedTasks.length);
            List expectedIDs = Arrays.asList(new Integer[] { new Integer(2),
                    new Integer(3) });
            List actualIds = new ArrayList(2);
            actualIds.add(new Integer(nestedTasks[0].getTaskID()));
            actualIds.add(new Integer(nestedTasks[1].getTaskID()));
            assertEquals("Unexpected IDs of the imported tasks", new HashSet(
                    expectedIDs), new HashSet(actualIds));
        }
    }
}
