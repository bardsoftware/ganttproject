// Copyright (C) 2017 BarD Software
package biz.ganttproject.impex.csv;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManagerImpl;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCsvExportTest extends TaskTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TaskDefaultColumn.setLocaleApi(null);
  }

  public void testResourceCustomFields() throws IOException {
    HumanResourceManager hrManager = new HumanResourceManager(null, new CustomColumnsManager());
    TaskManager taskManager = getTaskManager();
    RoleManager roleManager = new RoleManagerImpl();
    CSVOptions csvOptions = enableOnly("id");
    CustomPropertyDefinition prop1 = hrManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "prop1", null);
    CustomPropertyDefinition prop2 = hrManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "prop2", null);
    CustomPropertyDefinition prop3 = hrManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "prop3", null);
    hrManager.add(new HumanResource("HR1", 1, hrManager));
    hrManager.add(new HumanResource("HR2", 2, hrManager));
    hrManager.add(new HumanResource("HR3", 3, hrManager));
    hrManager.getById(1).addCustomProperty(prop3, "1");
    hrManager.getById(2).addCustomProperty(prop2, "2");
    hrManager.getById(3).addCustomProperty(prop1, "3");

    GanttCSVExport exporter = new GanttCSVExport(taskManager, hrManager, roleManager, csvOptions);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    exporter.save(outputStream);
    String[] lines = new String(outputStream.toByteArray(), Charsets.UTF_8.name()).split("\\n");
    assertEquals(7, lines.length);
    assertEquals("ID,prop1,prop2,prop3", lines[3].trim());
    assertEquals("1,,,1", lines[4].trim());
    assertEquals("2,,2,", lines[5].trim());
    assertEquals("3,3,,", lines[6].trim());
  }

  public void testTaskCustomFields() throws IOException {
    HumanResourceManager hrManager = new HumanResourceManager(null, new CustomColumnsManager());
    TaskManager taskManager = getTaskManager();
    RoleManager roleManager = new RoleManagerImpl();
    CSVOptions csvOptions = enableOnly(TaskDefaultColumn.ID.getStub().getID());

    CustomPropertyDefinition prop1 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "prop1", null);
    CustomPropertyDefinition prop2 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "prop2", null);
    CustomPropertyDefinition prop3 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "prop3", null);
    Task task1 = createTask();
    Task task2 = createTask();
    Task task3 = createTask();
    task1.getCustomValues().addCustomProperty(prop3, "a");
    task2.getCustomValues().addCustomProperty(prop2, "b");
    task3.getCustomValues().addCustomProperty(prop1, "c");

    GanttCSVExport exporter = new GanttCSVExport(taskManager, hrManager, roleManager, csvOptions);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    exporter.save(outputStream);
    String[] lines = new String(outputStream.toByteArray(), Charsets.UTF_8.name()).split("\\n");
    assertEquals(4, lines.length);
    assertEquals("tableColID,prop1,prop2,prop3", lines[0].trim());
    assertEquals("0,,,a", lines[1].trim());
    assertEquals("1,,b,", lines[2].trim());
    assertEquals("2,c,,", lines[3].trim());

  }

  public void testResourceAssignments() throws IOException {
    HumanResourceManager hrManager = new HumanResourceManager(null, new CustomColumnsManager());
    TaskManager taskManager = getTaskManager();
    CSVOptions csvOptions = enableOnly(TaskDefaultColumn.ID.getStub().getID(), TaskDefaultColumn.RESOURCES.getStub().getID());

    Task task1 = createTask();
    Task task2 = createTask();
    Task task3 = createTask();

    HumanResource alice = hrManager.create("Alice", 1);
    HumanResource bob = hrManager.create("Bob", 2);

    task1.getAssignmentCollection().addAssignment(alice).setLoad(100f);
    task2.getAssignmentCollection().addAssignment(alice).setLoad(45.457f);
    task2.getAssignmentCollection().addAssignment(bob);

    GanttCSVExport exporter = new GanttCSVExport(taskManager, hrManager, new RoleManagerImpl(), csvOptions);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    exporter.save(outputStream);
    String[] lines = new String(outputStream.toByteArray(), Charsets.UTF_8.name()).split("\\n");

    assertEquals(9, lines.length);
    assertEquals("tableColID,resources,Assignments", lines[0].trim());
    assertEquals("0,Alice,1:100.00", lines[1].trim());
    assertEquals("1,Alice;Bob,1:45.46;2:0.00", lines[2].trim());
    assertEquals("2,,", lines[3].trim());

  }

  public void testTaskColor() throws IOException {
    TaskManager taskManager = getTaskManager();
    GanttTask task0 = taskManager.createTask();
    GanttTask task1 = taskManager.createTask();
    GanttTask task2 = taskManager.createTask();
    GanttTask task3 = taskManager.createTask();
    task0.setColor(Color.RED);
    task1.setColor(Color.GREEN);
    task2.setColor(new Color(42, 42, 42));
    // Leave task3 color default

    CSVOptions csvOptions = enableOnly(TaskDefaultColumn.ID.getStub().getID(), TaskDefaultColumn.COLOR.getStub().getID());
    GanttCSVExport exporter = new GanttCSVExport(
        taskManager,
        new HumanResourceManager(null, new CustomColumnsManager()),
        new RoleManagerImpl(),
        csvOptions
    );
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    exporter.save(outputStream);
    String[] lines = new String(outputStream.toByteArray(), Charsets.UTF_8.name()).split("\\n");
    assertEquals(5, lines.length);
    assertEquals("tableColID,option.taskDefaultColor.label", lines[0].trim());
    assertEquals("0,\"#ff0000\"", lines[1].trim());
    assertEquals("1,\"#00ff00\"", lines[2].trim());
    assertEquals("2,\"#2a2a2a\"", lines[3].trim());
    assertEquals("3,", lines[4].trim());
  }

  public void testBomOption() throws IOException {
    TaskManager taskManager = getTaskManager();
    GanttTask task = taskManager.createTask();
    CSVOptions csvOptions = enableOnly(
        TaskDefaultColumn.NAME.getStub().getID());
    csvOptions.getBomOption().setValue(true);
    {
      GanttCSVExport exporter = new GanttCSVExport(
          taskManager,
          new HumanResourceManager(null, new CustomColumnsManager()),
          new RoleManagerImpl(),
          csvOptions
      );
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      exporter.save(outputStream);

      byte[] bytes = outputStream.toByteArray();
      // Binary representation of Unicode FEFF
      assertEquals((byte) 0xef, bytes[0]);
      assertEquals((byte) 0xbb, bytes[1]);
      assertEquals((byte) 0xbf, bytes[2]);
    }
    csvOptions.getBomOption().setValue(false);
    {
      GanttCSVExport exporter = new GanttCSVExport(
          taskManager,
          new HumanResourceManager(null, new CustomColumnsManager()),
          new RoleManagerImpl(),
          csvOptions
      );
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      exporter.save(outputStream);

      byte[] bytes = outputStream.toByteArray();
      // No BOM in the first bytes
      assertEquals('t', bytes[0]);
    }
  }

  private static CSVOptions enableOnly(String... fields) {
    CSVOptions csvOptions = new CSVOptions();
    Set fieldSet = ImmutableSet.copyOf(fields);
    for (BooleanOption option : csvOptions.getTaskOptions().values()) {
      if (!fieldSet.contains(option.getID())) {
        option.setValue(false);
      }
    }
    for (BooleanOption option : csvOptions.getResourceOptions().values()) {
      if (!fieldSet.contains(option.getID())) {
        option.setValue(false);
      }
    }
    return csvOptions;
  }
}
