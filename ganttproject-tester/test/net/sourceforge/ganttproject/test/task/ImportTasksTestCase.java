/*
 * Created on 24.02.2005
 */
package net.sourceforge.ganttproject.test.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.CustomPropertyClass;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
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
            Task[] nestedTasks = taskManager.getTaskHierarchy().getNestedTasks(root);
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

        taskManager.importData(importFrom, Collections.<CustomPropertyDefinition, CustomPropertyDefinition>emptyMap());
        {
            Task root = taskManager.getTaskHierarchy().getRootTask();
            Task[] nestedTasks = taskManager.getTaskHierarchy().getNestedTasks(
                    root);
            assertEquals(
                    "Unexpected count of the root's children AFTER importing. root="
                            + root, 2, nestedTasks.length);
            List<Integer> expectedIDs = Arrays.asList(new Integer[] { new Integer(2),
                    new Integer(3) });
            List<Integer> actualIds = new ArrayList<Integer>(2);
            actualIds.add(new Integer(nestedTasks[0].getTaskID()));
            actualIds.add(new Integer(nestedTasks[1].getTaskID()));
            assertEquals("Unexpected IDs of the imported tasks", new HashSet<Integer>(
                    expectedIDs), new HashSet<Integer>(actualIds));
        }
    }

    private static CustomPropertyDefinition findCustomPropertyByName(CustomPropertyManager mgr, String name) {
        for (CustomPropertyDefinition def : mgr.getDefinitions()) {
            if (def.getName().equals(name)) {
                return def;
            }
        }
        return null;
    }
    public void testImportCustomColumns() {
        TaskManager importTo = getTaskManager();
        {
            CustomPropertyDefinition def = importTo.getCustomPropertyManager().createDefinition(
                    "col1", CustomPropertyClass.TEXT.getID(), "foo", "foo");
            Task root = importTo.getTaskHierarchy().getRootTask();
            importTo.createTask(1).move(root);
            assertEquals("foo", importTo.getTask(1).getCustomValues().getValue(def));
        }
        TaskManager importFrom = newTaskManager();
        {
            CustomPropertyDefinition def = importFrom.getCustomPropertyManager().createDefinition(
                    "col1", CustomPropertyClass.TEXT.getID(), "bar", "bar");
            Task root = importTo.getTaskHierarchy().getRootTask();
            importFrom.createTask(1).move(root);
            assertEquals("bar", importFrom.getTask(1).getCustomValues().getValue(def));
        }
        Map<CustomPropertyDefinition, CustomPropertyDefinition> customDefsMapping =
                importTo.getCustomPropertyManager().importData(importFrom.getCustomPropertyManager());
        importTo.importData(importFrom, customDefsMapping);

        CustomPropertyDefinition fooDef = findCustomPropertyByName(importTo.getCustomPropertyManager(), "foo");
        assertNotNull(fooDef);
        assertEquals("foo", importTo.getTask(1).getCustomValues().getValue(fooDef));

        CustomPropertyDefinition barDef = findCustomPropertyByName(importTo.getCustomPropertyManager(), "bar");
        assertNotNull(barDef);
        assertEquals("bar", importTo.getTask(1).getCustomValues().getValue(barDef));
    }
}
