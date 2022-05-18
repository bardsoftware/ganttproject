/*
Copyright 2022 BarD Software s.r.o, GanttProject Cloud OU, Dmitry Barashev

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.parser

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.io.XmlProject
import biz.ganttproject.core.io.XmlTasks.XmlTask
import biz.ganttproject.core.model.task.ConstraintType
import biz.ganttproject.core.option.GPOption
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.customproperty.CustomColumnsException
import biz.ganttproject.customproperty.SimpleSelect
import biz.ganttproject.lib.fx.TreeCollapseView
import com.google.common.base.Charsets
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.gui.zoom.ZoomManager
import net.sourceforge.ganttproject.task.CostStub
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskView
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl
import org.apache.commons.lang3.StringEscapeUtils
import org.w3c.util.DateParser
import org.w3c.util.InvalidDateException
import java.awt.Color
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.net.URLDecoder
import java.util.*

class TaskLoader(private val taskManager: TaskManager, private val treeCollapseView: TreeCollapseView<Task>) {
  val legacyFixedStartTasks = mutableListOf<Task>()
  val dependencies = mutableListOf<GanttDependStructure>()
  private val mapXmlGantt = mutableMapOf<XmlTask, Task>()

  fun loadTaskCustomPropertyDefinitions(xmlProject: XmlProject) {
    xmlProject.tasks.taskproperties?.filter { it.type == "custom" }?.forEach { xmlTaskProperty ->
      val def = taskManager.customPropertyManager.createDefinition(
        xmlTaskProperty.id, xmlTaskProperty.valuetype, xmlTaskProperty.name, xmlTaskProperty.defaultvalue)
      xmlTaskProperty.simpleSelect?.let {
        def.calculationMethod = SimpleSelect(xmlTaskProperty.id, StringEscapeUtils.unescapeXml(it.select), def.propertyClass.javaClass)
      }
    }
  }

  fun loadTask(parent: XmlTask?, child: XmlTask): Task {
    var builder = taskManager.newTaskBuilder().withId(child.id).withName(child.name)
    if (child.uid.isNotBlank()) {
      builder = builder.withUid(child.uid)
    }
    val start = child.startDate
    if (start.isNotBlank()) {
      builder = builder.withStartDate(GanttCalendar.parseXMLDate(start).time)
    }
    builder = builder.withDuration(taskManager.createLength(child.duration.toLong()))
    builder =
      if (parent != null) {
        mapXmlGantt[parent]?.let { parentTask -> builder.withParent(parentTask) } ?: run {
          LOG.error("Can't find parent task for xmlTask={}, xmlParent={}", child, parent)
          builder
        }
      } else {
        builder
      }
    builder = builder.withExpansionState(child.isExpanded)
    if (child.isMilestone) {
      builder = builder.withLegacyMilestone()
    }
    return builder.build().also { task ->
      mapXmlGantt[child] = task
      treeCollapseView.setExpanded(task, child.isExpanded)
      task.isProjectTask = child.isProjectTask
      task.completionPercentage = child.completion
      task.priority = Task.Priority.fromPersistentValue(child.priority)
      if (child.color != null) {
        task.color = ColorValueParser.parseString(child.color)
      }

      // We used to have "fixed-start" attribute in the earlier versions of GanttProject.
      // It's meaning was "do not pull this task backwards if possible". It was replaced with rubber constraint type.
      child.legacyFixedStart?.let {
        if (it == "true") {
          legacyFixedStartTasks.add(task)
        }
      }

      val earliestStart = child.earliestStartDate
      if (earliestStart != null) {
        task.setThirdDate(GanttCalendar.parseXMLDate(earliestStart))
      }

      child.webLink?.let { it ->
        try {
          task.webLink = URLDecoder.decode(it, Charsets.UTF_8.name())
        } catch (e: UnsupportedEncodingException) {
          LOG.error("Can't decode URL value={} from XML task={}", it, child)
        }
      }
      child.shape?.let { it ->
        val st1 = StringTokenizer(it, ",")
        val array = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        var token: String
        var count = 0
        while (st1.hasMoreTokens()) {
          token = st1.nextToken()
          array[count] = token.toInt()
          count++
        }
        task.shape = ShapePaint(4, 4, array, Color.white, task.color)
      }
      val costValue = child.costManualValue
      val isCostCalculated = child.isCostCalculated
      task.cost = CostStub(costValue ?: BigDecimal.ZERO, isCostCalculated ?: false)
      child.notes?.let {
        task.notes = it
      }

      loadDependencies(child)
      loadCustomProperties(task, child)
    }
  }

  private fun loadDependencies(xmlTask: XmlTask) {
    xmlTask.dependencies?.forEach { xmlDependency ->
      dependencies.add(GanttDependStructure(
        taskID = xmlTask.id,
        successorTaskID = xmlDependency.id,
        difference = xmlDependency.lag,
        dependType =
          if (xmlDependency.type.isBlank()) { ConstraintType.finishstart }
          else { ConstraintType.fromPersistentValue(xmlDependency.type) },
        hardness =
          if (xmlDependency.type.isBlank()) { Hardness.STRONG }
          else { Hardness.parse(xmlDependency.hardness) }
      ))
    }
  }

  private fun loadCustomProperties(task: Task, xmlTask: XmlTask) {
    xmlTask.customPropertyValues.forEach { xmlCustomProperty ->
      val cc = taskManager.customPropertyManager.getCustomPropertyDefinition(xmlCustomProperty.propId) ?: run {
        LOG.error("Can't find custom property definition for XML custom property {}", xmlCustomProperty)
        return@forEach
      }
      xmlCustomProperty.value?.let { valueStr ->
        when {
          cc.type == String::class.java -> valueStr
          cc.type == Boolean::class.java -> java.lang.Boolean.valueOf(valueStr)
          cc.type == Int::class.java -> Integer.valueOf(valueStr)
          cc.type == Double::class.java -> java.lang.Double.valueOf(valueStr)
          GregorianCalendar::class.java.isAssignableFrom(cc.type) ->
            try {
              CalendarFactory.createGanttCalendar(DateParser.parse(valueStr))
            } catch (e: InvalidDateException) {
              LOG.error("Can't parse date {} from XML custom property {}", valueStr, xmlCustomProperty)
              null
            }
          else -> null
        }
      }?.also { value ->
        try {
          task.customValues.setValue(cc, value)
        } catch (e: CustomColumnsException) {
          LOG.error("Can't set the value={} of a custom property={} for task={}", value, cc, task)
        }
      }
    }
  }
}

data class GanttDependStructure(
  var taskID: Int = 0,
  var successorTaskID: Int = 0,
  var difference: Int = 0,
  var dependType: ConstraintType = ConstraintType.finishstart,
  var hardness: Hardness = Hardness.STRONG
)

fun loadDependencyGraph(deps: List<GanttDependStructure>, taskManager: TaskManager, legacyFixedStartTasks: List<Task>) {
  deps.forEach { ds ->
    val dependee: Task = taskManager.getTask(ds.taskID) ?: return@forEach
    val dependant: Task = taskManager.getTask(ds.successorTaskID) ?: return@forEach
    try {
      val dep = taskManager.dependencyCollection.createDependency(dependant, dependee, FinishStartConstraintImpl())
      dep.constraint = taskManager.createConstraint(ds.dependType)
      dep.difference = ds.difference
      if (legacyFixedStartTasks.contains(dependant)) {
        dep.hardness = Hardness.RUBBER
      } else {
        dep.hardness = ds.hardness
      }
    } catch (e: TaskDependencyException) {
      GPLogger.log(e)
    }
  }
}

fun loadGanttView(xmlProject: XmlProject, taskManager: TaskManager, taskView: TaskView, zoomManager: ZoomManager, taskColumns: ColumnList, options: List<GPOption<*>>) {
  val xmlView = xmlProject.views.firstOrNull { "gantt-chart" == it.id } ?: return
  // Load timeline tasks
  xmlView.timeline.let { timelineString ->
      taskView.timelineTasks.clear()
      timelineString.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach { id ->
        taskManager.getTask(id)?.let { taskView.timelineTasks.add(it) }
      }
    }
  xmlView.options?.forEach { xmlOption ->
    options.firstOrNull { it.id == xmlOption.id }?.loadPersistentValue(
      xmlOption.text ?: xmlOption.value
    )
  }
  loadView(xmlView, zoomManager, taskColumns)
}

private val LOG = GPLogger.create("Project.IO.Load.Task")

