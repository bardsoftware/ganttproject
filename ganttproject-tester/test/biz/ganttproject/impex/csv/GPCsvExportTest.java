// Copyright (C) 2017 BarD Software
package biz.ganttproject.impex.csv;

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.BooleanOption;
import com.google.common.base.Charsets;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.io.CSVOptions;
import net.sourceforge.ganttproject.io.CSVOptionsHandler;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManagerImpl;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.test.task.TaskTestCase;
import org.junit.Ignore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author dbarashev@bardsoftware.com
 */
public class GPCsvExportTest extends TaskTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    TaskDefaultColumn.setLocaleApi(null);
  }


  @Ignore
  public void testResourceCustomFieldsCsvXls() throws IOException {
    HumanResourceManager hrManager = new HumanResourceManager(null, new CustomColumnsManager());
    TaskManager taskManager = getTaskManager();
    RoleManager roleManager = new RoleManagerImpl();
    CSVOptions csvOptions = new CSVOptions();
    for (BooleanOption option : csvOptions.getResourceOptions().values()) {
      if (!"id".equals(option.getID())) {
        option.setValue(false);
      }
    }
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

    File xlsTemp = File.createTempFile("exportToXls", ".xls");
    CSVOptionsHandler csvOptionsHandler = new CSVOptionsHandler(csvOptions, csvOptions);
    GanttCSVExport exporter = new GanttCSVExport(taskManager, hrManager, roleManager, csvOptionsHandler);
    FileOutputStream fileOutputStream = new FileOutputStream(xlsTemp);
    exporter.saveXls(fileOutputStream);
    System.out.println(xlsTemp.getAbsolutePath());

    File csvTemp = File.createTempFile("exportToCsv", ".csv");
    fileOutputStream = new FileOutputStream(csvTemp);
    exporter.saveCsv(fileOutputStream);
    System.out.println(csvTemp.getAbsolutePath());

    File csvTemp2 = File.createTempFile("exportToCsv", ".csv");
    fileOutputStream = new FileOutputStream(csvTemp2);
    exporter.save(fileOutputStream, "csv");
    System.out.println(csvTemp2.getAbsolutePath());

    File xlsTemp2 = File.createTempFile("exportToXls", ".xls");
    fileOutputStream = new FileOutputStream(xlsTemp2);
    exporter.save(fileOutputStream, "xls");
    System.out.println(xlsTemp2.getAbsolutePath());


  }

  public void testResourceCustomFields() throws IOException {
    HumanResourceManager hrManager = new HumanResourceManager(null, new CustomColumnsManager());
    TaskManager taskManager = getTaskManager();
    RoleManager roleManager = new RoleManagerImpl();
    CSVOptions csvOptions = new CSVOptions();
    for (BooleanOption option : csvOptions.getResourceOptions().values()) {
      if (!"id".equals(option.getID())) {
        option.setValue(false);
      }
    }
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

    CSVOptionsHandler csvOptionsHandler = new CSVOptionsHandler(csvOptions, csvOptions);
    GanttCSVExport exporter = new GanttCSVExport(taskManager, hrManager, roleManager, csvOptionsHandler);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    exporter.saveCsv(outputStream);
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
    CSVOptions csvOptions = new CSVOptions();
    for (BooleanOption option : csvOptions.getTaskOptions().values()) {
      if (!TaskDefaultColumn.ID.getStub().getID().equals(option.getID())) {
        option.setValue(false);
      }
    }
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

    CSVOptionsHandler csvOptionsHandler = new CSVOptionsHandler(csvOptions, csvOptions);
    GanttCSVExport exporter = new GanttCSVExport(taskManager, hrManager, roleManager, csvOptionsHandler);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    exporter.saveCsv(outputStream);
    String[] lines = new String(outputStream.toByteArray(), Charsets.UTF_8.name()).split("\\n");
    assertEquals(4, lines.length);
    assertEquals("tableColID,prop1,prop2,prop3", lines[0].trim());
    assertEquals("0,,,a", lines[1].trim());
    assertEquals("1,,b,", lines[2].trim());
    assertEquals("2,c,,", lines[3].trim());

  }
}
