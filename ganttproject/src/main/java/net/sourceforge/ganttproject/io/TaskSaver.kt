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
import biz.ganttproject.customproperty.CustomPropertyDefinition
import biz.ganttproject.customproperty.CustomPropertyManager
import biz.ganttproject.customproperty.SimpleSelect
import com.google.common.xml.XmlEscapers
import net.sourceforge.ganttproject.GanttTask
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.task.*
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
    tasks.forEach { writeTask(handler, it as GanttTask, project.taskCustomColumnManager) }
    endElement("tasks", handler)
  }

  @Throws(SAXException::class, IOException::class)
  private fun writeTask(handler: TransformerHandler, task: GanttTask, customPropertyManager: CustomPropertyManager) {
    if (task.taskID == -1) {
      throw IllegalArgumentException("Is it a fake root task? Task=$task")
    }
    val attrs = AttributesImpl()
    addAttribute("id", task.taskID, attrs)
    addAttribute("uid", task.uid, attrs)
    addAttribute("name", task.name, attrs)
    addAttribute("color", task.externalizedColor(), attrs)
    if (task.shapeDefined()) {
      addAttribute("shape", task.shape.array, attrs)
    }
    addAttribute("meeting", task.isLegacyMilestone, attrs)
    if (task.isProjectTask) {
      addAttribute("project", true, attrs)
    }
    addAttribute("start", task.start.toXMLString(), attrs)
    addAttribute("duration", task.duration.length, attrs)
    addAttribute("complete", task.completionPercentage, attrs)
    if (task.third != null) {
      addAttribute("thirdDate", task.third.toXMLString(), attrs)
      addAttribute("thirdDate-constraint", task.thirdDateConstraint, attrs)
    }
    if (task.priority != Task.DEFAULT_PRIORITY) {
      addAttribute("priority", task.priority.persistentValue, attrs)
    }
    addAttribute("webLink", task.externalizedWebLink(), attrs)
    addAttribute("expand", taskCollapseView.isExpanded(task), attrs)
    if (!(task.cost.isCalculated && task.cost.manualValue == BigDecimal.ZERO)) {
      addAttribute("cost-manual-value", task.cost.manualValue.toPlainString(), attrs)
      addAttribute("cost-calculated", task.cost.isCalculated, attrs)
    }
    startElement("task", attrs, handler)
    cdataElement("notes", task.externalizedNotes(), attrs, handler)

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
      nestedTasks.forEach { writeTask(handler, it as GanttTask, customPropertyManager) }
    }
    // end of task section
    endElement("task", handler)
  }

  @Throws(SAXException::class)
  private fun writeTaskDefaultProperty(handler: TransformerHandler, id: String, name: String, valueType: String) {
    val attrs = AttributesImpl()
    addAttribute("id", id, attrs)
    addAttribute("name", name, attrs)
    addAttribute("type", "default", attrs)
    addAttribute("valuetype", valueType, attrs)
    emptyElement("taskproperty", attrs, handler)
  }


  @Throws(SAXException::class)
  private fun writeTaskCustomProperty(handler: TransformerHandler, def: CustomPropertyDefinition) {
    val attrs = AttributesImpl()
    var defaultValue = def.defaultValue
    val definitionType = def.type
    val valueType = encodeFieldType(definitionType) ?: return
    if ("date" == valueType && defaultValue != null) {
      defaultValue = when (defaultValue) {
        is GanttCalendar -> DateParser.getIsoDate(defaultValue.time)
        is Date -> DateParser.getIsoDate(defaultValue)
        else -> throw IllegalStateException(
          "Default value is expected to be either GanttCalendar or Date instance, while it is ${defaultValue::class.java}")
      }
    }

    addAttribute("id", def.id, attrs)
    addAttribute("name", def.name, attrs)
    addAttribute("type", "custom", attrs)
    addAttribute("valuetype", valueType, attrs)
    addAttribute("defaultvalue", defaultValue?.toString(), attrs)
    def.attributes.entries.forEach { (key, value) -> addAttribute(key, value, attrs) }
    when (val calculationMethod = def.calculationMethod) {
      null -> emptyElement("taskproperty", attrs, handler)
      is SimpleSelect -> {
        startElement("taskproperty", attrs, handler)
        emptyElement("simple-select", AttributesImpl().also {attrs ->
          addAttribute("select", XmlEscapers.xmlAttributeEscaper().escape(calculationMethod.selectExpression), attrs)
        }, handler)
        endElement("taskproperty", handler)
      }
    }
  }


  @Throws(SAXException::class)
  private fun writeTaskProperties(handler: TransformerHandler, customPropertyManager: CustomPropertyManager) {
    writeTaskDefaultProperty(handler, "tpd0", "type", "icon")
    writeTaskDefaultProperty(handler, "tpd1", "priority", "icon")
    writeTaskDefaultProperty(handler, "tpd2", "info", "icon")
    writeTaskDefaultProperty(handler, "tpd3", "name", "text")
    writeTaskDefaultProperty(handler, "tpd4", "begindate", "date")
    writeTaskDefaultProperty(handler, "tpd5", "enddate", "date")
    writeTaskDefaultProperty(handler, "tpd6", "duration", "int")
    writeTaskDefaultProperty(handler, "tpd7", "completion", "int")
    writeTaskDefaultProperty(handler, "tpd8", "coordinator", "text")
    writeTaskDefaultProperty(handler, "tpd9", "predecessorsr", "text")
    customPropertyManager.definitions.forEach { propertyDef ->
      writeTaskCustomProperty(handler, propertyDef)
    }
  }

  private fun encodeFieldType(fieldType: Class<*>): String? {
    return PropertyTypeEncoder.encodeFieldType(fieldType)
  }
}

fun Task.externalizedWebLink(): String? = externalizeWebLink(this.webLink)

fun externalizeWebLink(webLink: String?) =
  if (!webLink.isNullOrBlank() && webLink != "http://") {
    URLEncoder.encode(webLink, Charsets.UTF_8.name())
  } else null

// XML CDATA section adds extra line separator on Windows.
// See https://bugs.openjdk.java.net/browse/JDK-8133452.
fun Task.externalizedNotes(): String? = externalizeNotes(this.notes)
fun externalizeNotes(notes: String?) = notes?.replace("\\r\\n", "\\n")?.ifBlank { null }

fun TaskImpl.externalizedColor(): String? = if (colorDefined()) ColorConvertion.getColor(color) else null
