/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.io;

import biz.ganttproject.core.time.GanttCalendar;
import com.google.common.base.Charsets;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.task.CustomColumnsValues;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.util.ColorConvertion;
import org.w3c.util.DateParser;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.sax.TransformerHandler;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

class TaskSaver extends SaverBase {
  void save(IGanttProject project, TransformerHandler handler) throws SAXException, IOException {
    AttributesImpl attrs = new AttributesImpl();
    if (project.getTaskManager().isZeroMilestones() != null) {
      addAttribute("empty-milestones", project.getTaskManager().isZeroMilestones(), attrs);
    }
    startElement("tasks", attrs, handler);

    startElement("taskproperties", handler);
    writeTaskProperties(handler, project.getTaskCustomColumnManager());
    endElement("taskproperties", handler);
    Task rootTask = project.getTaskManager().getTaskHierarchy().getRootTask();
    Task[] tasks = project.getTaskManager().getTaskHierarchy().getNestedTasks(rootTask);
    for (Task task : tasks) {
      writeTask(handler, (GanttTask) task, project.getTaskCustomColumnManager());
    }
    endElement("tasks", handler);
  }

  private void writeTask(TransformerHandler handler, GanttTask task, CustomPropertyManager customPropertyManager)
      throws SAXException, IOException {
    if (task.getTaskID() == -1) {
      throw new IllegalArgumentException("Is it a fake root task? Task=" + task);
    }
    AttributesImpl attrs = new AttributesImpl();
    addAttribute("id", String.valueOf(task.getTaskID()), attrs);
    addAttribute("name", task.getName(), attrs);
    if (task.colorDefined()) {
      addAttribute("color", ColorConvertion.getColor(task.getColor()), attrs);
    }
    if (task.shapeDefined()) {
      addAttribute("shape", task.getShape().getArray(), attrs);
    }
    addAttribute("meeting", Boolean.valueOf(task.isLegacyMilestone()).toString(), attrs);
    if (task.isProjectTask()) {
      addAttribute("project", Boolean.TRUE.toString(), attrs);
    }
    addAttribute("start", task.getStart().toXMLString(), attrs);
    addAttribute("duration", String.valueOf(task.getLength()), attrs);
    addAttribute("complete", String.valueOf(task.getCompletionPercentage()), attrs);
    if (task.getThird() != null) {
      addAttribute("thirdDate", task.getThird().toXMLString(), attrs);
      addAttribute("thirdDate-constraint", String.valueOf(task.getThirdDateConstraint()), attrs);
    }
    if (task.getPriority() != Task.DEFAULT_PRIORITY) {
      addAttribute("priority", task.getPriority().getPersistentValue(), attrs);
    }
    final String sWebLink = task.getWebLink();
    if (sWebLink != null && !sWebLink.equals("") && !sWebLink.equals("http://")) {
      addAttribute("webLink", URLEncoder.encode(sWebLink, Charsets.UTF_8.name()), attrs);
    }
    addAttribute("expand", String.valueOf(task.getExpand()), attrs);

    if (!(task.getCost().isCalculated() && task.getCost().getManualValue().equals(BigDecimal.ZERO))) {
      addAttribute("cost-manual-value", task.getCost().getManualValue().toPlainString(), attrs);
      addAttribute("cost-calculated", task.getCost().isCalculated(), attrs);
    }
    startElement("task", attrs, handler);

    if (task.getNotes() != null && task.getNotes().length() > 0) {
      // See https://bugs.openjdk.java.net/browse/JDK-8133452
      String taskNotes = task.getNotes().replace("\\r\\n", "\\n");
      cdataElement("notes", taskNotes, attrs, handler);
    }
    // use successors to write depends information
    final TaskDependency[] depsAsDependee = task.getDependenciesAsDependee().toArray();
    for (TaskDependency next : depsAsDependee) {
      addAttribute("id", String.valueOf(next.getDependant().getTaskID()), attrs);
      addAttribute("type", next.getConstraint().getType().getPersistentValue(), attrs);
      addAttribute("difference", String.valueOf(next.getDifference()), attrs);
      addAttribute("hardness", next.getHardness().getIdentifier(), attrs);
      emptyElement("depend", attrs, handler);
    }

    CustomColumnsValues ccv = task.getCustomValues();
    for (CustomPropertyDefinition def : customPropertyManager.getDefinitions()) {
      final String idc = def.getID();
      if (ccv.hasOwnValue(def)) {
        Object value = ccv.getValue(def);
        if (GregorianCalendar.class.isAssignableFrom(def.getType()) && value != null) {
          value = DateParser.getIsoDate(((GanttCalendar) value).getTime());
        }
        addAttribute("taskproperty-id", idc, attrs);
        addAttribute("value", value == null ? null : String.valueOf(value), attrs);
        emptyElement("customproperty", attrs, handler);
      }
    }

    // Write the child of the task
    if (task.getManager().getTaskHierarchy().hasNestedTasks(task)) {
      Task[] nestedTasks = task.getManager().getTaskHierarchy().getNestedTasks(task);
      for (Task nestedTask : nestedTasks) {
        writeTask(handler, (GanttTask) nestedTask, customPropertyManager);
      }
    }

    // end of task section
    endElement("task", handler);
  }

  private void writeTaskProperty(TransformerHandler handler, String id, String name, String type, String valueType)
      throws SAXException {
    writeTaskProperty(handler, id, name, type, valueType, null, Collections.<String,String>emptyMap());
  }

  private void writeTaskProperty(TransformerHandler handler, String id, String name, String type, String valueType,
                                 String defaultValue, Map<String, String> attributes) throws SAXException {
    AttributesImpl attrs = new AttributesImpl();
    addAttribute("id", id, attrs);
    addAttribute("name", name, attrs);
    addAttribute("type", type, attrs);
    addAttribute("valuetype", valueType, attrs);
    if (defaultValue != null) {
      addAttribute("defaultvalue", defaultValue, attrs);
    }
    for (Map.Entry<String,String> kv : attributes.entrySet()) {
      addAttribute(kv.getKey(), kv.getValue(), attrs);
    }
    emptyElement("taskproperty", attrs, handler);
  }

  private void writeTaskProperties(TransformerHandler handler, CustomPropertyManager customPropertyManager)
      throws SAXException {
    writeTaskProperty(handler, "tpd0", "type", "default", "icon");
    writeTaskProperty(handler, "tpd1", "priority", "default", "icon");
    writeTaskProperty(handler, "tpd2", "info", "default", "icon");
    writeTaskProperty(handler, "tpd3", "name", "default", "text");
    writeTaskProperty(handler, "tpd4", "begindate", "default", "date");
    writeTaskProperty(handler, "tpd5", "enddate", "default", "date");
    writeTaskProperty(handler, "tpd6", "duration", "default", "int");
    writeTaskProperty(handler, "tpd7", "completion", "default", "int");
    writeTaskProperty(handler, "tpd8", "coordinator", "default", "text");
    writeTaskProperty(handler, "tpd9", "predecessorsr", "default", "text");
    for (CustomPropertyDefinition cc : customPropertyManager.getDefinitions()) {
      Object defVal = cc.getDefaultValue();
      final Class<?> cla = cc.getType();
      final String valueType = encodeFieldType(cla);
      if (valueType == null) {
        continue;
      }
      if ("date".equals(valueType) && defVal != null) {
        if (defVal instanceof GanttCalendar) {
          defVal = DateParser.getIsoDate(((GanttCalendar) defVal).getTime());
        } else if (defVal instanceof Date) {
          defVal = DateParser.getIsoDate((Date) defVal);
        } else {
          assert false : "Default value is expected to be either GanttCalendar or Date instance, while it is "
              + defVal.getClass();
        }
      }
      String idcStr = cc.getID();
      writeTaskProperty(handler, idcStr, cc.getName(), "custom", valueType,
          defVal == null ? null : String.valueOf(defVal), cc.getAttributes());
    }
  }

  private static String encodeFieldType(Class<?> fieldType) {
    return CustomPropertyManager.PropertyTypeEncoder.encodeFieldType(fieldType);
  }

}
