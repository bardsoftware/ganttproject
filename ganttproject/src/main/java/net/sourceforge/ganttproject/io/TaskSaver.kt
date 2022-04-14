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
package net.sourceforge.ganttproject.io

import biz.ganttproject.customproperty.PropertyTypeEncoder
import biz.ganttproject.lib.fx.TreeCollapseView
import biz.ganttproject.core.time.GanttCalendar
import com.google.common.base.Charsets
import net.sourceforge.ganttproject.CustomPropertyManager
import net.sourceforge.ganttproject.GanttTask
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.util.ColorConvertion
import org.w3c.util.DateParser
import org.xml.sax.SAXException
import org.xml.sax.helpers.AttributesImpl

import javax.xml.transform.sax.TransformerHandler
import java.io.IOException
import java.math.BigDecimal
import java.net.URLEncoder
import java.util.*
import kotlin.jvm.Throws

class TaskSaver(private val taskCollapseView: TreeCollapseView<Task>): SaverBase() {
  @Throws(SAXException::class, IOException::class)
  fun save(project: IGanttProject, handler: TransformerHandler) {
    val attrs = AttributesImpl()
    if (project.taskManager.isZeroMilestones != null) {
      addAttribute("empty-milestones", project.taskManager.isZeroMilestones, attrs)
    }
    startElement("tasks", attrs, handler)

    startElement("taskproperties", handler)
    writeTaskProperties(handler, project.taskCustomColumnManager)
    endElement("taskproperties", handler)
    val rootTask = project.taskManager.taskHierarchy.rootTask
    val tasks = project.taskManager.taskHierarchy.getNestedTasks(rootTask)
    tasks.forEach { task ->
      writeTask(handler,  task as GanttTask, project.taskCustomColumnManager)
    }
    endElement("tasks", handler)
  }

  @Throws(SAXException::class, IOException::class)
  private fun writeTask(handler: TransformerHandler, task: GanttTask, customPropertyManager: CustomPropertyManager) {
    if (task.taskID == -1) {
      throw IllegalArgumentException("Is it a fake root task? Task=$task")
    }
    val attrs = AttributesImpl()
    addAttribute("id", task.taskID, attrs)
    addAttribute("name", task.name, attrs)
    if (task.colorDefined()) {
      addAttribute("color", ColorConvertion.getColor(task.color), attrs)
    }
    if (task.shapeDefined()) {
      addAttribute("shape", task.shape.array, attrs)
    }
    addAttribute("meeting", task.isLegacyMilestone, attrs)
    if (task.isProjectTask) {
      addAttribute("project", true, attrs)
    }
    addAttribute("start", task.start.toXMLString(), attrs)
    addAttribute("duration", task.length, attrs)
    addAttribute("complete", task.completionPercentage, attrs)
    if (task.third != null) {
      addAttribute("thirdDate", task.third.toXMLString(), attrs)
      addAttribute("thirdDate-constraint", task.thirdDateConstraint, attrs)
    }
    if (task.priority != Task.DEFAULT_PRIORITY) {
      addAttribute("priority", task.priority.persistentValue, attrs)
    }
    val sWebLink = task.webLink
    if (sWebLink != null && !sWebLink.equals("") && !sWebLink.equals("http://")) {
      addAttribute("webLink", URLEncoder.encode(sWebLink, Charsets.UTF_8.name()), attrs)
    }
    addAttribute("expand", taskCollapseView.isExpanded(task), attrs)

    if (!(task.cost.isCalculated && task.cost.manualValue.equals(BigDecimal.ZERO))) {
      addAttribute("cost-manual-value", task.cost.manualValue.toPlainString(), attrs)
      addAttribute("cost-calculated", task.cost.isCalculated, attrs)
    }
    startElement("task", attrs, handler)

    if (task.notes != null && task.notes.isNotEmpty()) {
      // See https://bugs.openjdk.java.net/browse/JDK-8133452
      val taskNotes = task.notes.replace("\\r\\n", "\\n")
      cdataElement("notes", taskNotes, attrs, handler)
    }
    // use successors to write depends information
    val depsAsDependee = task.dependenciesAsDependee.toArray()
    depsAsDependee.forEach { next ->
      addAttribute("id", next.dependant.taskID, attrs)
      addAttribute("type", next.constraint.type.persistentValue, attrs)
      addAttribute("difference", next.difference, attrs)
      addAttribute("hardness", next.hardness.identifier, attrs)
      emptyElement("depend", attrs, handler)
    }

    val customValues = task.customValues
    customPropertyManager.definitions.forEach { propertyDef ->
      val propertyId = propertyDef.id
      if (customValues.hasOwnValue(propertyDef)) {
        var value = customValues.getValue(propertyDef)
        if (GregorianCalendar::class.java.isAssignableFrom(propertyDef.type) && value != null) {
          value = DateParser.getIsoDate((value as GanttCalendar).time)
        }
        addAttribute("taskproperty-id", propertyId, attrs)
        addAttribute("value", value?.toString(), attrs)
        emptyElement("customproperty", attrs, handler)
      }
    }

    // Write the child of the task
    if (task.manager.taskHierarchy.hasNestedTasks(task)) {
      val nestedTasks = task.manager.taskHierarchy.getNestedTasks(task)
      nestedTasks.forEach { nestedTask ->
        writeTask(handler, nestedTask as GanttTask, customPropertyManager)
      }
    }

    // end of task section
    endElement("task", handler)
  }

  @Throws(SAXException::class)
  private fun writeTaskProperty(handler: TransformerHandler, id: String, name: String, type: String, valueType: String) {
    writeTaskProperty(handler, id, name, type, valueType, null, emptyMap())
  }


  @Throws(SAXException::class)
  private fun writeTaskProperty(handler: TransformerHandler, id:  String, name: String, type: String, valueType: String,
  defaultValue: String?, attributes: Map<String, String>) {
    val attrs = AttributesImpl()
    addAttribute("id", id, attrs)
    addAttribute("name", name, attrs)
    addAttribute("type", type, attrs)
    addAttribute("valuetype", valueType, attrs)
    if (defaultValue != null) {
      addAttribute("defaultvalue", defaultValue, attrs)
    }
    attributes.entries.forEach { (key, value) -> addAttribute(key, value, attrs) }
    emptyElement("taskproperty", attrs, handler)
  }


  @Throws(SAXException::class)
  private fun writeTaskProperties(handler: TransformerHandler, customPropertyManager:  CustomPropertyManager) {
    writeTaskProperty(handler, "tpd0", "type", "default", "icon")
    writeTaskProperty(handler, "tpd1", "priority", "default", "icon")
    writeTaskProperty(handler, "tpd2", "info", "default", "icon")
    writeTaskProperty(handler, "tpd3", "name", "default", "text")
    writeTaskProperty(handler, "tpd4", "begindate", "default", "date")
    writeTaskProperty(handler, "tpd5", "enddate", "default", "date")
    writeTaskProperty(handler, "tpd6", "duration", "default", "int")
    writeTaskProperty(handler, "tpd7", "completion", "default", "int")
    writeTaskProperty(handler, "tpd8", "coordinator", "default", "text")
    writeTaskProperty(handler, "tpd9", "predecessorsr", "default", "text")
    customPropertyManager.definitions.forEach { propertyDef ->
      var defaultValue = propertyDef.defaultValue
      val definitionType = propertyDef.type
      val valueType = encodeFieldType(definitionType) ?: return@forEach
      if ("date" == valueType && defaultValue != null) {
        defaultValue = when (defaultValue) {
          is GanttCalendar -> DateParser.getIsoDate(defaultValue.time)
          is Date -> DateParser.getIsoDate(defaultValue)
          else -> throw IllegalStateException(
            "Default value is expected to be either GanttCalendar or Date instance, while it is ${defaultValue::class.java}")
        }
      }
      val propertyId = propertyDef.id
      writeTaskProperty(handler, propertyId, propertyDef.name, "custom", valueType,
        defaultValue?.toString(), propertyDef.attributes
      )
    }
  }

  private fun encodeFieldType(fieldType: Class<*>): String? {
    return PropertyTypeEncoder.encodeFieldType(fieldType)
  }
}
