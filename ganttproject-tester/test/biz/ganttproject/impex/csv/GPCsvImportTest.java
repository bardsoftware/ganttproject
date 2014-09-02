/*
Copyright 2012 GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.impex.csv;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.TestSetupHelper.TaskManagerBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import biz.ganttproject.core.model.task.TaskDefaultColumn;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;

/**
 * Tests CSV import with GP semantics.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GPCsvImportTest extends TestCase {
  private Supplier<Reader> createSupplier(String data) {
    return Suppliers.<Reader> ofInstance(new StringReader(data));
  }

  private static Map<String, Task> buildTaskMap(TaskManager taskManager) {
    return Maps.uniqueIndex(Arrays.asList(taskManager.getTasks()), new Function<Task, String>() {
      @Override
      public String apply(Task input) {
        return input.getName();
      }
    });
  }

  private static void assertDependency(Task dependant, Task dependee) {
    for (TaskDependency dep : dependant.getDependenciesAsDependant().toArray()) {
      if (dep.getDependee() == dependee) {
        return;
      }
    }
    fail("Can't find " + dependee + " in the list of predecessors of " + dependant);
  }

  @Override
  protected void setUp() throws Exception {
    TaskDefaultColumn.setLocaleApi(new TaskDefaultColumn.LocaleApi() {
      @Override
      public String i18n(String key) {
        return GanttLanguage.getInstance().getText(key);
      }
    });
  }

  public void testImportAssignments() throws Exception {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();
    HumanResourceManager resourceManager = builder.getResourceManager();

    String header1 = "Name,Begin date,End date,Resources,Duration,Completion,Web Link,Notes,Predecessors,ID";
    String data1 = "t1,23/07/12,25/07/12,Joe;John,,,,,,";

    String header2 = "Name,ID,e-mail,Phone,Default role";
    String data2 = "Joe,1,,,\nJohn,2,,,\nJack,3,,,";
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header1, data1, "", header2, data2)), taskManager, resourceManager);
    importer.load();

    Map<String, Task> taskMap = buildTaskMap(taskManager);
    Task t1 = taskMap.get("t1");
    assertNotNull(t1);
    Map<String, HumanResource> resourceMap = Maps.uniqueIndex(resourceManager.getResources(), new Function<HumanResource, String>() {
      @Override
      public String apply(HumanResource input) {
        return input.getName();
      }
    });
    assertNotNull(t1.getAssignmentCollection().getAssignment(resourceMap.get("Joe")));
    assertNotNull(t1.getAssignmentCollection().getAssignment(resourceMap.get("John")));
  }

  public void testCustomFields() throws Exception {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();
    String header1 = "Field1,ID,Name,Begin date,End date,Predecessors,Resources,Duration,Completion,Web Link,Notes,Field2";
    String data1 = "value1,,t1,23/07/12,25/07/12,,,,,,,value2";

    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header1, data1)), taskManager, null);
    importer.load();
    Map<String, Task> taskMap = buildTaskMap(taskManager);
    Task t1 = taskMap.get("t1");
    assertNotNull(t1);

    CustomPropertyDefinition def1 = taskManager.getCustomPropertyManager().getCustomPropertyDefinition("Field1");
    CustomPropertyDefinition def2 = taskManager.getCustomPropertyManager().getCustomPropertyDefinition("Field2");
    assertNotNull(def1);
    assertNotNull(def2);

    assertEquals("value1", t1.getCustomValues().getValue(def1));
    assertEquals("value2", t1.getCustomValues().getValue(def2));
  }

  public void testDependencies() throws Exception {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();

    String header1 = "ID,Name,Begin date,End date,Resources,Duration,Completion,Web Link,Notes,Predecessors";
    String data1 = "1,t1,23/07/12,25/07/12,,,,,,";
    String data2 = "2,t2,26/07/12,27/07/12,,,,,,1";
    String data3 = "3,t3,26/07/12,30/07/12,,,,,,1";

    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header1, data1, data2, data3)), taskManager, null);
    importer.load();
    Map<String, Task> taskMap = buildTaskMap(taskManager);
    Task t1 = taskMap.get("t1");
    Task t2 = taskMap.get("t2");
    Task t3 = taskMap.get("t3");

    assertDependency(t2, t1);
    assertDependency(t3, t1);
  }

  public void testMilestone() throws Exception {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();

    String header1 = "ID,Name,Begin date,End date,Duration,Resources,Completion,Web Link,Notes,Predecessors";
    String data1 = "1,t1,23/07/12,24/07/12,1,,,,,";
    String data2 = "2,t2,26/07/12,26/07/12,0,,,,,";

    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header1, data1, data2)), taskManager, null);
    importer.load();
    Map<String, Task> taskMap = buildTaskMap(taskManager);
    Task t1 = taskMap.get("t1");
    Task t2 = taskMap.get("t2");
    assertFalse(t1.isMilestone());
    assertTrue(t2.isMilestone());
  }

  public void testHierarchy() throws IOException {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();

    String header1 = "ID,Name,Begin date,End date,Duration,Outline number";
    String data1 = "1,t1,23/07/12,26/07/12,1,1";
    String data2 = "2,t2,23/07/12,24/07/12,1,1.1";
    String data3 = "3,t3,24/07/12,26/07/12,1,1.2";
    String data4 = "4,t4,24/07/12,25/07/12,1,1.2.1";
    String data5 = "5,t5,25/07/12,26/07/12,1,1.2.2";

    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header1, data1, data2, data3, data4, data5)), taskManager, null);
    importer.load();
    Map<String, Task> taskMap = buildTaskMap(taskManager);
    TaskContainmentHierarchyFacade hierarchy = taskManager.getTaskHierarchy();
    Task t1 = taskMap.get("t1");
    Task t2 = taskMap.get("t2");
    Task t3 = taskMap.get("t3");
    Task t4 = taskMap.get("t4");
    Task t5 = taskMap.get("t5");
    assertEquals(t3, hierarchy.getContainer(t5));
    assertEquals(t3, hierarchy.getContainer(t4));
    assertEquals(t1, hierarchy.getContainer(t3));
    assertEquals(t1, hierarchy.getContainer(t2));
  }
}
