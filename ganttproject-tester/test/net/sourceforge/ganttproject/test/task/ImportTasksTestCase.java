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

import biz.ganttproject.customproperty.CustomPropertyClass;
import biz.ganttproject.customproperty.CustomPropertyDefinition;
import biz.ganttproject.customproperty.CustomPropertyManager;
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
            List<Integer> expectedIDs = Arrays.asList(2,
                    3);
            List<Integer> actualIds = new ArrayList<Integer>(2);
            actualIds.add(nestedTasks[0].getTaskID());
            actualIds.add(nestedTasks[1].getTaskID());
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

  /**
   * The root cause of the issue 2104: taskDependency::setConstraint method implementation calls
   * constraint::setDependency(this), and if we just pass the same constraint instance from one dependency to another,
   * which is the case when we import a project, we basically make the constraint instance invalid: it is now bound to
   * activities of other tasks.
   *
   * This test case tests that the original constraint is not affected by the importProject call.
   */
  public void testImportDependencies_Issue2104() {
      TaskManager taskManager = getTaskManager();
      Task root = taskManager.getTaskHierarchy().getRootTask();
      TaskManager importFrom = newTaskManager();

      var task1 = importFrom.newTaskBuilder().withName("t1").build();
      var task2 = importFrom.newTaskBuilder().withName("t2").build();
      var dep = importFrom.getDependencyCollection().createDependency(task2, task1);
      taskManager.importData(importFrom, Collections.emptyMap());

      assertEquals(task2, dep.getConstraint().getActivityBinding().getDependantActivity().getOwner());
      assertEquals(task1, dep.getConstraint().getActivityBinding().getDependeeActivity().getOwner());


    }
}
