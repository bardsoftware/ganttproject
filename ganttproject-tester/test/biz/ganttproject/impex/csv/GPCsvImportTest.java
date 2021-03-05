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

import biz.ganttproject.app.DefaultLocalizer;
import biz.ganttproject.app.InternationalizationKt;
import biz.ganttproject.core.model.task.TaskDefaultColumn;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import junit.framework.TestCase;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.ResourceDefaultColumn;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.TestSetupHelper.TaskManagerBuilder;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManagerImpl;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static biz.ganttproject.impex.csv.SpreadsheetFormat.CSV;
import static biz.ganttproject.impex.csv.SpreadsheetFormat.XLS;

/**
 * Tests spreadsheet (CSV and XLS) import with GP semantics.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GPCsvImportTest extends TestCase {

  private Supplier<InputStream> createSupplier(final byte[] data) {
    return () -> new ByteArrayInputStream(data);
  }

  private static Map<String, Task> buildTaskMap(TaskManager taskManager) {
    return Maps.uniqueIndex(Arrays.asList(taskManager.getTasks()), Task::getName);
  }

  private Map<String, HumanResource> buildResourceMap(HumanResourceManager resourceManager) {
    return Maps.uniqueIndex(resourceManager.getResources(), HumanResource::getName);
  }

  private static void assertDependency(Task dependant, Task dependee) {
    for (TaskDependency dep : dependant.getDependenciesAsDependant().toArray()) {
      if (dep.getDependee() == dependee) {
        return;
      }
    }
    fail("Can't find " + dependee + " in the list of predecessors of " + dependant);
  }

  private static void assertDependency(Task dependant, Task dependee, int lag) {
    for (TaskDependency dep : dependant.getDependenciesAsDependant().toArray()) {
      if (dep.getDependee() == dependee) {
        assertEquals("Unexpected lag value", lag, dep.getDifference());
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
    InternationalizationKt.setRootLocalizer(new DefaultLocalizer() {
      @Nullable
      @Override
      public String formatTextOrNull(@NotNull String key, @NotNull Object... args) {
        return key;
      }
    });

    GanttLanguage.getInstance().setShortDateFormat(new SimpleDateFormat("dd/MM/yy"));
  }

  public void testImportResourcesColumn() throws Exception {

    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE,
        TaskRecords.TaskFields.RESOURCES,
        TaskRecords.TaskFields.DURATION,
        TaskRecords.TaskFields.COMPLETION,
        TaskRecords.TaskFields.WEB_LINK,
        TaskRecords.TaskFields.NOTES,
        TaskRecords.TaskFields.PREDECESSORS,
        TaskRecords.TaskFields.ID);
    String data1 = "t1,23/07/12,25/07/12,Joe;John,,,,,,";

    String header2 = buildResourceHeader(ResourceDefaultColumn.NAME, ResourceDefaultColumn.ID, ResourceDefaultColumn.ROLE);
    String data2 = "Joe,1,,,\nJohn,2,,,\nJack,3,,,";

    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(header1, data1, "", header2, data2)) {
      TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
      HumanResourceManager resourceManager = builder.getResourceManager();
      Map<String, Task> taskMap = doTestImportAssignments(pair.second(), pair.first(), builder, null, resourceManager, new RoleManagerImpl());
      Task t1 = taskMap.get("t1");
      assertNotNull(t1);
      Map<String, HumanResource> resourceMap = buildResourceMap(resourceManager);
      assertNotNull(t1.getAssignmentCollection().getAssignment(resourceMap.get("Joe")));
      assertNotNull(t1.getAssignmentCollection().getAssignment(resourceMap.get("John")));
    }
  }

  public void testImportAssignmentsColumn() throws Exception {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();
    HumanResourceManager resourceManager = builder.getResourceManager();
    RoleManager roleManager = new RoleManagerImpl();

    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE,
        TaskRecords.TaskFields.ASSIGNMENTS);
    String data1 = "t1,23/07/12,25/07/12,1:100.00;2:50.00";
    String data2 = "t2,23/07/12,25/07/12,3:100.00";
    String data3 = "t3,23/07/12,25/07/12,";

    String header2 = buildResourceHeader(
        ResourceDefaultColumn.NAME,
        ResourceDefaultColumn.ID,
        ResourceDefaultColumn.EMAIL,
        ResourceDefaultColumn.PHONE,
        ResourceDefaultColumn.ROLE
    );
    String resources = "Joe,1,,,\nJohn,2,,,\nJack,3,,,";
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(
        Joiner.on('\n').join(header1, data1, data2, data3, "", header2, resources).getBytes(Charsets.UTF_8)),
        SpreadsheetFormat.CSV,
        taskManager, resourceManager, roleManager, builder.getTimeUnitStack());
    importer.load();

    Map<String, HumanResource> resourceMap = Maps.uniqueIndex(resourceManager.getResources(), new Function<HumanResource, String>() {
      @Override
      public String apply(HumanResource input) {
        return input.getName();
      }
    });
    Map<String, Task> taskMap = buildTaskMap(taskManager);

    Task t1 = taskMap.get("t1");
    assertNotNull(t1);
    assertNotNull(t1.getAssignmentCollection().getAssignment(resourceMap.get("Joe")));
    assertEquals(100f, t1.getAssignmentCollection().getAssignment(resourceMap.get("Joe")).getLoad());
    assertNotNull(t1.getAssignmentCollection().getAssignment(resourceMap.get("John")));
    assertEquals(50f, t1.getAssignmentCollection().getAssignment(resourceMap.get("John")).getLoad());

    assertEquals(100f, taskMap.get("t2").getAssignmentCollection().getAssignment(resourceMap.get("Jack")).getLoad());
    assertEquals(0, taskMap.get("t3").getAssignmentCollection().getAssignments().length);
  }

  public void testImportResourceRole() throws Exception {
    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE,
        TaskRecords.TaskFields.RESOURCES,
        TaskRecords.TaskFields.DURATION,
        TaskRecords.TaskFields.COMPLETION,
        TaskRecords.TaskFields.WEB_LINK,
        TaskRecords.TaskFields.NOTES,
        TaskRecords.TaskFields.PREDECESSORS,
        TaskRecords.TaskFields.ID);
    String data1 = "";

    String header2 = buildResourceHeader(ResourceDefaultColumn.NAME, ResourceDefaultColumn.ID, ResourceDefaultColumn.ROLE);
    String data2 = "Joe,1,Default:1";

    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(header1, data1, "", header2, data2)) {
      TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
      HumanResourceManager resourceManager = builder.getResourceManager();
      doTestImportAssignments(pair.second(), pair.first(), builder, null, resourceManager, new RoleManagerImpl());
      Map<String, HumanResource> resourceMap = buildResourceMap(resourceManager);
      // resProjectManager because we're using dummy localizer
      assertEquals("resProjectManager", resourceMap.get("Joe").getRole().getName());
    }
  }

  public void testCustomFields() throws Exception {
    String header1 = "Field1," + buildTaskHeader(TaskRecords.TaskFields.ID,
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE,
        TaskRecords.TaskFields.PREDECESSORS,
        TaskRecords.TaskFields.RESOURCES,
        TaskRecords.TaskFields.DURATION,
        TaskRecords.TaskFields.COMPLETION,
        TaskRecords.TaskFields.WEB_LINK,
        TaskRecords.TaskFields.NOTES) + ",Field2";
    String data1 = "value1,,t1,23/07/12,25/07/12,,,,,,,value2";

    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(header1, data1)) {
      TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
      TaskManager taskManager = builder.build();
      Map<String, Task> taskMap = doTestImportAssignments(pair.second(), pair.first(), builder, taskManager, null, null);
      Task t1 = taskMap.get("t1");
      assertNotNull(t1);

      CustomPropertyDefinition def1 = taskManager.getCustomPropertyManager().getCustomPropertyDefinition("Field1");
      CustomPropertyDefinition def2 = taskManager.getCustomPropertyManager().getCustomPropertyDefinition("Field2");
      assertNotNull(def1);
      assertNotNull(def2);

      assertEquals("value1", t1.getCustomValues().getValue(def1));
      assertEquals("value2", t1.getCustomValues().getValue(def2));
    }
  }

  private String buildTaskHeader(TaskRecords.TaskFields... taskFields) {
    return Joiner.on(',').join(Stream.of(taskFields).map(TaskRecords.TaskFields::toString).iterator());
  }

  private String buildResourceHeader(ResourceDefaultColumn... resourceFields) {
    return Joiner.on(',').join(Stream.of(resourceFields).map(ResourceDefaultColumn::toString).iterator());
  }

  public void testDependencies() throws Exception {
    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.ID,
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE,
        TaskRecords.TaskFields.RESOURCES,
        TaskRecords.TaskFields.DURATION,
        TaskRecords.TaskFields.COMPLETION,
        TaskRecords.TaskFields.WEB_LINK,
        TaskRecords.TaskFields.NOTES,
        TaskRecords.TaskFields.PREDECESSORS);
    String data1 = "1,t1,23/07/12,25/07/12,,,,,,";
    String data2 = "2,t2,26/07/12,27/07/12,,,,,,1";
    String data3 = "3,t3,26/07/12,30/07/12,,,,,,1";
    String data4 = "4,t4,26/07/12,30/07/12,,,,,,1-FS=P1D";
    String data5 = "5,t5,26/07/12,30/07/12,,,,,,1-FS=P-1D";

    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(header1, data1, data2, data3, data4, data5)) {
      TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
      Map<String, Task> taskMap = doTestImportAssignments(pair.second(), pair.first(), builder, null, null, null);
      Task t1 = taskMap.get("t1");
      Task t2 = taskMap.get("t2");
      Task t3 = taskMap.get("t3");
      Task t4 = taskMap.get("t4");
      Task t5 = taskMap.get("t5");

      assertDependency(t2, t1);
      assertDependency(t3, t1);
      assertDependency(t4, t1, 1);
      assertDependency(t5, t1, -1);
    }
  }

  public void testMilestone() throws Exception {
    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.ID,
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE,
        TaskRecords.TaskFields.DURATION,
        TaskRecords.TaskFields.RESOURCES,
        TaskRecords.TaskFields.COMPLETION,
        TaskRecords.TaskFields.WEB_LINK,
        TaskRecords.TaskFields.NOTES,
        TaskRecords.TaskFields.PREDECESSORS);
    String data1 = "1,t1,23/07/12,24/07/12,1,,,,,";
    String data2 = "2,t2,26/07/12,26/07/12,0,,,,,";

    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(header1, data1, data2)) {
      TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
      Map<String, Task> taskMap = doTestImportAssignments(pair.second(), pair.first(), builder, null, null, null);
      Task t1 = taskMap.get("t1");
      Task t2 = taskMap.get("t2");
      assertFalse(t1.isMilestone());
      assertTrue(t2.isMilestone());
    }
  }

  public void testHierarchy() throws Exception {
    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.ID,
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE,
        TaskRecords.TaskFields.DURATION,
        TaskRecords.TaskFields.OUTLINE_NUMBER);
    String data1 = "1,t1,23/07/12,26/07/12,1,1";
    String data2 = "2,t2,23/07/12,24/07/12,1,1.1";
    String data3 = "3,t3,24/07/12,26/07/12,1,1.2";
    String data4 = "4,t4,24/07/12,25/07/12,1,1.2.1";
    String data5 = "5,t5,25/07/12,26/07/12,1,1.2.2";
    String data6 = "6,t6,25/07/12,26/07/12,1,3.0";
    String data7 = "7,t7,25/07/12,26/07/12,1,3.1";

    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(header1, data1, data2, data3, data4, data5, data6, data7)) {
      TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
      TaskManager taskManager = builder.build();
      Map<String, Task> taskMap = doTestImportAssignments(pair.second(), pair.first(), builder, taskManager, null, null);
      TaskContainmentHierarchyFacade hierarchy = taskManager.getTaskHierarchy();
      Task t1 = taskMap.get("t1");
      Task t2 = taskMap.get("t2");
      Task t3 = taskMap.get("t3");
      Task t4 = taskMap.get("t4");
      Task t5 = taskMap.get("t5");
      Task t6 = taskMap.get("t6");
      Task t7 = taskMap.get("t7");
      assertEquals(t3, hierarchy.getContainer(t5));
      assertEquals(t3, hierarchy.getContainer(t4));
      assertEquals(t1, hierarchy.getContainer(t3));
      assertEquals(t1, hierarchy.getContainer(t2));
      assertEquals(t6, hierarchy.getContainer(t7));
    }
  }

  public void testUseEndDateInsteadOfDuration() throws Exception {
    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.ID,
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE);
    String data1 = "1,t1,23/07/12,26/07/12";

    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(header1, data1)) {
      TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
      Map<String, Task> taskMap = doTestImportAssignments(pair.second(), pair.first(), builder, null, null, null);
      assertEquals(4.0f, taskMap.get("t1").getDuration().getLength(builder.getTimeUnitStack().getDefaultTimeUnit()));
    }
  }

  public void testEmptyDuration() throws Exception {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();

    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.ID,
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.DURATION,
        TaskRecords.TaskFields.END_DATE
    );
    String data1 = "1,t1,23/07/12,,26/07/12";

    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header1, data1).getBytes(Charsets.UTF_8)),
        SpreadsheetFormat.CSV,
        taskManager, null, null, builder.getTimeUnitStack());
    importer.load();
    Map<String, Task> taskMap = buildTaskMap(taskManager);
    assertEquals(4.0f, taskMap.get("t1").getDuration().getLength(builder.getTimeUnitStack().getDefaultTimeUnit()));
  }

  public void testImportTotalCostAndTotalLoad() throws Exception {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();
    HumanResourceManager resourceManager = builder.getResourceManager();
    RoleManager roleManager = new RoleManagerImpl();

    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE,
        TaskRecords.TaskFields.ASSIGNMENTS);
    String data1 = "t1,23/07/12,25/07/12,1:100.00;2:50.00";
    String data2 = "t2,23/07/12,25/07/12,3:100.00";
    String data3 = "t3,23/07/12,25/07/12,";

    String header2 = buildResourceHeader(
        ResourceDefaultColumn.NAME,
        ResourceDefaultColumn.ID,
        ResourceDefaultColumn.STANDARD_RATE,
        ResourceDefaultColumn.TOTAL_COST,
        ResourceDefaultColumn.TOTAL_LOAD
    );
    String resources = "Joe,1,10.0,20.0,100.0\nJohn,2,10.0,20.0,100.0\nJack,3,10.0,20.0,100.0";
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(
        Joiner.on('\n').join(header1, data1, data2, data3, "", header2, resources).getBytes(Charsets.UTF_8)),
        SpreadsheetFormat.CSV,
        taskManager, resourceManager, roleManager, builder.getTimeUnitStack());
    importer.load();

    Map<String, HumanResource> resourceMap = Maps.uniqueIndex(resourceManager.getResources(), input -> input.getName());
    resourceMap.forEach((key, hr) -> assertEquals(BigDecimal.valueOf(10.0), hr.getStandardPayRate()));
    resourceMap.forEach((key, hr) -> assertEquals(0, hr.getCustomProperties().size()));
    resourceMap.forEach((key, hr) -> assertTrue(hr.getTotalLoad() > 0.0));
    resourceMap.forEach((key, hr) -> assertTrue(hr.getTotalCost().doubleValue() > 0.0));
  }

  private static void assertOrder(String first, String second) {
    assertEquals(-1, TaskRecordsKt.getOUTLINE_NUMBER_COMPARATOR().compare(first, second));
    assertEquals(1, TaskRecordsKt.getOUTLINE_NUMBER_COMPARATOR().compare(second, first));
    assertEquals(0, TaskRecordsKt.getOUTLINE_NUMBER_COMPARATOR().compare(first, first));
    assertEquals(0, TaskRecordsKt.getOUTLINE_NUMBER_COMPARATOR().compare(second, second));
  }

  public void testOutlineNumberComparator() {
    assertOrder("1", "2");
    assertOrder("1", "1.1");
    assertOrder("1.1", "1.2");
    assertOrder("1.1", "2");
    assertOrder("1.2", "1.10");
    assertOrder("2", "10");
  }

  public void testTaskColors() throws IOException {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    TaskManager taskManager = builder.build();

    //String header1 = "ID,Name,Begin date,End date,Task color";
    String header1 = buildTaskHeader(
        TaskRecords.TaskFields.ID,
        TaskRecords.TaskFields.NAME,
        TaskRecords.TaskFields.BEGIN_DATE,
        TaskRecords.TaskFields.END_DATE,
        TaskRecords.TaskFields.COLOR
    );
    String data1 = "1,t1,23/07/12,26/07/12,\"#ff0000\"";
    String data2 = "2,t2,23/07/12,26/07/12,\"#00ff00\"";
    String data3 = "3,t3,23/07/12,26/07/12,\"#2a2a2a\"";
    String data4 = "4,t4,23/07/12,26/07/12,";
    String data5 = "5,t5,23/07/12,26/07/12,red";

    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(
        Joiner.on('\n').join(header1, data1, data2, data3, data4, data5).getBytes(Charsets.UTF_8)),
        SpreadsheetFormat.CSV,
        taskManager, null, null, builder.getTimeUnitStack());
    importer.load();
    Map<String, Task> taskMap = buildTaskMap(taskManager);
    assertEquals(5, taskMap.size());
    assertEquals(Color.RED, taskMap.get("t1").getColor());
    assertEquals(Color.GREEN, taskMap.get("t2").getColor());
    assertEquals(new Color(42, 42, 42), taskMap.get("t3").getColor());
    assertEquals(builder.getDefaultColor(), taskMap.get("t4").getColor());
    assertEquals(builder.getDefaultColor(), taskMap.get("t5").getColor());
  }

  private List<Pair<SpreadsheetFormat, Supplier<InputStream>>> createPairs(String... data) throws Exception {
    List<Pair<SpreadsheetFormat, Supplier<InputStream>>> pairs = new ArrayList<>();
    pairs.add(Pair.create(CSV, createSupplier(Joiner.on('\n').join(data).getBytes(Charsets.UTF_8))));
    pairs.add(Pair.create(XLS, createSupplier(createXls(data))));
    return pairs;
  }

  private Map<String, Task> doTestImportAssignments(Supplier<InputStream> supplier, SpreadsheetFormat format, TaskManagerBuilder builder,
      TaskManager taskManager, HumanResourceManager resourceManager, RoleManager roleManager) throws IOException {
    if (taskManager == null) {
      taskManager = builder.build();
    }
    GanttCSVOpen importer = new GanttCSVOpen(supplier, format, taskManager, resourceManager, roleManager, builder.getTimeUnitStack());
    importer.load();
    return buildTaskMap(taskManager);
  }

  private byte[] createXls(String... rows) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (SpreadsheetWriter writer = new XlsWriterImpl(stream)) {
      for (String row : rows) {
        for (String line : row.split("\n", -1)) {
          for (String cell : line.split(",", -1)) {
            writer.print(cell.trim());
          }
          writer.println();
        }
      }
    }
    return stream.toByteArray();
  }
}
