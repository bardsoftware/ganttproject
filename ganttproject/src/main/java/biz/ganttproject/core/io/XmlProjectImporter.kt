/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev, Dmitry Kazakov
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.core.io

import biz.ganttproject.core.calendar.CalendarEvent
import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.model.task.ConstraintType.fromPersistentValue
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import net.sourceforge.ganttproject.GanttPreviousState
import net.sourceforge.ganttproject.GanttPreviousStateTask
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.roles.Role
import net.sourceforge.ganttproject.roles.RolePersistentID
import biz.ganttproject.customproperty.CustomColumnsException
import net.sourceforge.ganttproject.task.CostStub
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.Task.DEFAULT_PRIORITY
import net.sourceforge.ganttproject.task.algorithm.AlgorithmBase
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl
import net.sourceforge.ganttproject.util.ColorConvertion
import org.slf4j.LoggerFactory
import org.w3c.util.InvalidDateException
import java.awt.Color
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.net.URLDecoder
import java.util.*


/**
 * @author: Dmitry Kazakov (qudeed@gmail.com)
 */
class XmlProjectImporter(private val ganttProject: GanttProjectImpl = GanttProjectImpl(),
                         private val debugId: String = "") {
  private lateinit var xmlProject: XmlProject
  private val fixedStartTasks = mutableSetOf<Task>()
  private val taskManager
    get() = ganttProject.taskManager
  private val resourceManager
    get() = ganttProject.humanResourceManager
  private val resCustomPropManager
    get() = ganttProject.resourceCustomPropertyManager
  private val taskCustomColManager
    get() = ganttProject.taskCustomColumnManager
  private val roleManager
    get() = ganttProject.roleManager
  private val calendar
    get() = ganttProject.activeCalendar

  fun import(xml: ByteArray): IGanttProject = import(xml.toString(Charsets.UTF_8))

  fun import(xml: String): IGanttProject {
    xmlProject = parseXmlProject(xml)

    ganttProject.projectName = xmlProject.name
    ganttProject.description = xmlProject.description?.trim() ?: ""
    ganttProject.organization = xmlProject.company
    ganttProject.webLink = xmlProject.webLink
    taskManager.isZeroMilestones = xmlProject.tasks.emptyMilestones
    taskManager.algorithmCollection.scheduler.setDiagnostic(RethrowingDiagnosticImpl())

    importCustomProps()
    importRoles()
    importResources()
    importTasks()
    importTasksDeps()
    importAllocations()
    importVacations()
    importPreviousStateTasks()
    // TODO: table columns
    importCalendar()

    return ganttProject
  }

  private fun importCalendar() {
    calendar.onlyShowWeekends = xmlProject.calendars.dayTypes.onlyShowWeekends.value
    calendar.baseCalendarID = xmlProject.calendars.baseId
    importHolidays()
    importDefaultWeek()
  }

  private fun importHolidays() {
    val holidays = xmlProject.calendars.events?.map {
      val type = if (it.type.isBlank()) CalendarEvent.Type.HOLIDAY else CalendarEvent.Type.valueOf(it.type)
      val color = if (it.color == null) null else ColorConvertion.determineColor(it.color)
      val description = it.value?.trim()
      val date = CalendarFactory.createGanttCalendar(
          if (it.year.isBlank()) 1 else it.year.toInt(), it.month - 1, it.date
      ).time
      CalendarEvent.newEvent(date, it.year.isBlank(), type, description, color)
    }?.toList()
    calendar.publicHolidays = holidays ?: emptyList()
  }

  private fun importDefaultWeek() {
    val defaultWeek = xmlProject.calendars.dayTypes.defaultWeek
    calendar.setWeekDayType(2, if (defaultWeek.mon == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(3, if (defaultWeek.tue == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(4, if (defaultWeek.wed == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(5, if (defaultWeek.thu == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(6, if (defaultWeek.fri == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(7, if (defaultWeek.sat == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(1, if (defaultWeek.sun == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
  }

  private fun importVacations() = xmlProject.vacations.forEach {
    val resource = resourceManager.getById(it.resourceid)
    val daysOff = GanttDaysOff(GanttCalendar.parseXMLDate(it.startDate), GanttCalendar.parseXMLDate(it.endDate))
    resource.addDaysOff(daysOff)
  }

  private fun importAllocations() = xmlProject.allocations.forEach {
    val task = taskManager.getTask(it.taskId)
    if (task == null) {
      LOGGER.warn("Task ${it.taskId} not found @project=${debugId}, skipping")
      return@forEach
    }
    val resource = resourceManager.getById(it.resourceId)
    if (resource == null) {
      LOGGER.warn("Task ${it.resourceId} not found @project=${debugId}, skipping")
      return@forEach
    }
    val assignment = task.assignmentCollection.addAssignment(resource)
    assignment.load = it.load
    assignment.isCoordinator = it.isCoordinator
    it.role?.let { function ->
      val role = findRole(function)
      if (role != null) {
        assignment.roleForAssignment = role
      }
    }
  }

  private fun importTasksDeps() = xmlProject.tasks.tasks?.forEach { importTasksDeps(it) }

  private fun importTasksDeps(task: XmlTasks.XmlTask) {
    task.dependencies?.forEach {
      val dependant = taskManager.getTask(it.id)
      val dependee = taskManager.getTask(task.id)
      if (dependant != null && dependee != null) {
        try {
          val dependency = taskManager.dependencyCollection.createDependency(dependant, dependee, FinishStartConstraintImpl())
          dependency.constraint = taskManager.createConstraint(fromPersistentValue(it.type))
          dependency.difference = it.lag
          dependency.hardness = if (fixedStartTasks.contains(dependant)) Hardness.RUBBER else Hardness.parse(it.hardness)
        } catch (exception: TaskDependencyException) {
          LOGGER.error("Unable to set dependency: $it")
        }
      }
    }
    task.tasks?.forEach { importTasksDeps(it) }
  }

  private fun importCustomProps() {
    xmlProject.resources.customProperties.forEach {
      resCustomPropManager.createDefinition(it.id, it.type, it.name, it.defaultValue)
    }
    xmlProject.tasks.taskproperties?.forEach {
      if (it.type == "custom") {
        taskCustomColManager.createDefinition(it.id, it.valuetype, it.name, it.defaultvalue)
      }
    }
  }

  private fun importRoles() {
    xmlProject.roles.forEach { roleset ->
      val rolesetName = roleset.rolesetName
      val roleSet = if (rolesetName == null) {
        roleManager.projectRoleSet
      } else {
        (roleManager.getRoleSet(rolesetName) ?: roleManager.createRoleSet(rolesetName)).also { it.isEnabled = true }
      }
      roleset.roles?.forEach {
        val persistentId = RolePersistentID(it.id)
        if (roleSet.findRole(persistentId.roleID) == null) {
          roleSet.createRole(it.name, persistentId.roleID)
        }
      }
    }
  }

  private fun findRole(roleIdAsString: String): Role? {
    val persistentId = RolePersistentID(roleIdAsString)
    val roleId = persistentId.roleID

    val roleSet = if (persistentId.roleSetID == null) {
      val roleSet = roleManager.projectRoleSet
      if (roleSet.findRole(roleId) == null) {
        val rolesetName = if (roleId in 3..10) "SoftwareDevelopment" else "Default"
        roleManager.getRoleSet(rolesetName).also { it.isEnabled = true }
      } else {
        roleSet
      }
    } else {
      roleManager.getRoleSet(persistentId.roleSetID)
    }

    return roleSet.findRole(roleId)
  }

  private fun importResources() {
    xmlProject.resources.resources.forEach { xmlRes ->
      val resource = resourceManager.create(xmlRes.name, xmlRes.id)
      resource.mail = xmlRes.email
      resource.phone = xmlRes.phone
      xmlRes.props.forEach { xmlProp ->
        val prop = resCustomPropManager.definitions.find { it.id == xmlProp.definitionId }
        if (prop != null) {
          resource.addCustomProperty(prop, xmlProp.value)
        }
      }
      xmlRes.rate?.let {
        if (it.name == "standard") {
          resource.standardPayRate = it.value
        }
      }
      val role = findRole(xmlRes.role)
      if (role != null) {
        resource.role = role
      } else {
        LOGGER.error("Role ${xmlRes.role} not found")
      }
    }
  }

  private fun importTasks() = xmlProject.tasks.tasks?.forEach { importTasks(it) }

  private fun importTasks(xmlTask: XmlTasks.XmlTask, parent: Task? = null) {
    val builder = taskManager.newTaskBuilder()
        .withUid(xmlTask.uid)
        .withId(xmlTask.id)
        .withName(xmlTask.name)
        .withStartDate(GanttCalendar.parseXMLDate(xmlTask.startDate).time)
        .withDuration(taskManager.createLength(xmlTask.duration.toLong()))
        .withParent(parent)
        .withExpansionState(xmlTask.isExpanded)
        .withCompletion(xmlTask.completion)
        .withPriority(xmlTask.priority?.let { Task.Priority.fromPersistentValue(it) } ?: DEFAULT_PRIORITY)
        .withColor(xmlTask.color?.let { ColorConvertion.determineColor(it) })
        .withWebLink(xmlTask.webLink?.let {
          try {
            URLDecoder.decode(it, Charsets.UTF_8.name())
          } catch (e: UnsupportedEncodingException) {
            it
          }
        } ?: "")
        .withCost(xmlTask.costManualValue ?: BigDecimal.ZERO)
        .withNotes(xmlTask.notes?.trim())
        .apply { if (xmlTask.isMilestone) withLegacyMilestone() }
    val task = builder.build()
    xmlTask.earliestStartDate?.let { task.setThirdDate(GanttCalendar.parseXMLDate(it)) }
    task.thirdDateConstraint = xmlTask.thirdDateConstraint ?: 0
    task.cost = CostStub(xmlTask.costManualValue ?: BigDecimal.ZERO, xmlTask.isCostCalculated ?: true)
    xmlTask.shape?.let {
      task.shape = buildShapePaint(it, task.color)
    }
    xmlTask.customPropertyValues.forEach { setTaskCustomValue(task, it) }
    task.isProjectTask = xmlTask.isProjectTask ?: false
    if (xmlTask.legacyFixedStart == "true") {
      fixedStartTasks.add(task)
    }

    xmlTask.tasks?.forEach { importTasks(it, task) }
  }

  private fun importPreviousStateTasks() = xmlProject.baselines?.baselines?.forEach { baseline ->
    val tasks = baseline.tasks?.map {
      GanttPreviousStateTask(it.id, GanttCalendar.parseXMLDate(it.startDate), it.duration, it.isMilestone, it.isSummaryTask)
    }
    ganttProject.baselines.add(GanttPreviousState(baseline.name, tasks))
  }

  private fun setTaskCustomValue(task: Task, property: XmlTasks.XmlTask.XmlCustomProperty) {
    val propDef = taskCustomColManager.getCustomPropertyDefinition(property.propId)
    val value = property.value?.let { when (propDef.type) {
      java.lang.String::class.java -> it
      java.lang.Boolean::class.java -> it.toBoolean()
      java.lang.Integer::class.java -> it.toInt()
      java.lang.Double::class.java -> it.toDouble()
      else -> if (GregorianCalendar::class.java.isAssignableFrom(propDef.type)) {
        try {
          GanttCalendar.parseXMLDate(it)
        } catch (exception: InvalidDateException) {
          LOGGER.error("Invalid date in custom type value: $it")
          null
        }
      } else {
        LOGGER.error("Unknown custom value type: ${propDef.typeAsString}")
        null
      }
    } }

    try {
      task.customValues.setValue(propDef, value)
    } catch (exception: CustomColumnsException) {
      LOGGER.error("Custom column mismatch for $property")
    }
  }

  private fun buildShapePaint(shape: String, color: Color): ShapePaint {
    val st1 = StringTokenizer(shape, ",")
    val array = IntArray(16) {0}
    var count = 0
    while (st1.hasMoreTokens()) {
      val token = st1.nextToken()
      array[count] = token.toInt()
      count++
    }
    return ShapePaint(4, 4, array, Color.white, color)
  }
}

class RethrowingDiagnosticImpl : AlgorithmBase.Diagnostic {
  override fun addModifiedTask(p0: Task?, p1: Date?, p2: Date?) {
    // no-op
  }

  override fun logError(ex: Exception) {
    throw RuntimeException(ex)
  }
}

private val LOGGER = LoggerFactory.getLogger("Document")
