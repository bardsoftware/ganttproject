/*
Copyright 2017 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.impex.msproject2;

import biz.ganttproject.core.time.GanttCalendar;
import net.sf.mpxj.FieldType;
import net.sf.mpxj.MPXJException;
import net.sf.mpxj.TaskField;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

import java.util.Map;

/**
 * Tests mapping of custom properties in GanttProject to TaskField enum values in MPXJ (which become
 * custom properties in MS Project file)
 *
 * @author dbarashev@bardsoftware.com
 */
public class CustomPropertyMappingTest extends TaskTestCase {
  /**
   * Basic test which maps custom properties from simple project created directly in GP
   */
  public void testSimpleTypeMapping() throws MPXJException {
    TaskManager taskManager = getTaskManager();
    CustomPropertyDefinition col1 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "col1", null);
    CustomPropertyDefinition col2 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Boolean.class), "col2", null);
    CustomPropertyDefinition col3 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Integer.class), "col3", null);
    CustomPropertyDefinition col4 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Double.class), "col4", null);
    CustomPropertyDefinition col5 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(GanttCalendar.class), "col5", null);

    Map<CustomPropertyDefinition, FieldType> mapping = CustomPropertyMapping.buildMapping(taskManager);
    assertEquals(TaskField.TEXT1, mapping.get(col1));
    assertEquals(TaskField.FLAG1, mapping.get(col2));
    assertEquals(TaskField.NUMBER1, mapping.get(col3));
    assertEquals(TaskField.NUMBER2, mapping.get(col4));
    assertEquals(TaskField.DATE1, mapping.get(col5));
  }

  /**
   * Tests that custom properties are sequentially mapped to increasing <PROPERTYTYPE>N fields in the order
   * of their creation in CustomPropertyManager
   */
  public void testSequentialNumbers() throws MPXJException {
    TaskManager taskManager = getTaskManager();
    CustomPropertyDefinition col5 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "col5", null);
    CustomPropertyDefinition col4 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "col4", null);
    CustomPropertyDefinition col3 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "col3", null);
    CustomPropertyDefinition col2 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "col2", null);
    CustomPropertyDefinition col1 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "col1", null);

    Map<CustomPropertyDefinition, FieldType> mapping = CustomPropertyMapping.buildMapping(taskManager);
    assertEquals(TaskField.TEXT1, mapping.get(col5));
    assertEquals(TaskField.TEXT2, mapping.get(col4));
    assertEquals(TaskField.TEXT3, mapping.get(col3));
    assertEquals(TaskField.TEXT4, mapping.get(col2));
    assertEquals(TaskField.TEXT5, mapping.get(col1));
  }

  /**
   * Tests custom properties with 'special' names. Such names could be created by import from MS Project
   * in GP < 2.8.5
   */
  public void testSpecialNames() throws MPXJException {
    TaskManager taskManager = getTaskManager();
    CustomPropertyDefinition col1 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(String.class), "Text1", null);
    CustomPropertyDefinition col2 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Boolean.class), "Flag2", null);
    CustomPropertyDefinition col3 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Integer.class), "Number3", null);
    CustomPropertyDefinition col4 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Double.class), "Number4", null);
    CustomPropertyDefinition col5 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(GanttCalendar.class), "Date5", null);
    CustomPropertyDefinition col6 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Integer.class), "Cost6", null);

    Map<CustomPropertyDefinition, FieldType> mapping = CustomPropertyMapping.buildMapping(taskManager);
    assertEquals(TaskField.TEXT1, mapping.get(col1));
    assertEquals(TaskField.FLAG2, mapping.get(col2));
    assertEquals(TaskField.NUMBER3, mapping.get(col3));
    assertEquals(TaskField.NUMBER4, mapping.get(col4));
    assertEquals(TaskField.DATE5, mapping.get(col5));
    assertEquals(TaskField.COST6, mapping.get(col6));
  }

  /**
   * Tests mapping of custom properties with special attribute which is created by import from MS Project
   * in GP >= 2.8.5
   */
  public void testMsProjectType() throws MPXJException {
    TaskManager taskManager = getTaskManager();
    CustomPropertyDefinition col5 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Integer.class), "col5", null);
    col5.getAttributes().put(CustomPropertyMapping.MSPROJECT_TYPE, "NUMBER1");

    CustomPropertyDefinition col4 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Integer.class), "col4", null);

    CustomPropertyDefinition col3 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Integer.class), "col3", null);
    col3.getAttributes().put(CustomPropertyMapping.MSPROJECT_TYPE, "COST10");

    CustomPropertyDefinition col2 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Integer.class), "col2", null);
    CustomPropertyDefinition col1 = taskManager.getCustomPropertyManager().createDefinition(
        CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(Integer.class), "col1", null);
    col1.getAttributes().put(CustomPropertyMapping.MSPROJECT_TYPE, "NUMBER3");

    Map<CustomPropertyDefinition, FieldType> mapping = CustomPropertyMapping.buildMapping(taskManager);
    assertEquals(TaskField.NUMBER3, mapping.get(col1));
    assertEquals(TaskField.NUMBER4, mapping.get(col2));
    assertEquals(TaskField.COST10, mapping.get(col3));
    assertEquals(TaskField.NUMBER2, mapping.get(col4));
    assertEquals(TaskField.NUMBER1, mapping.get(col5));
  }
}
