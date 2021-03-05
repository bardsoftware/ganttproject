/*
Copyright 2014-2021 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.impex.csv

import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.option.ColorOption
import biz.ganttproject.core.time.TimeUnitStack
import com.google.common.base.Function
import com.google.common.base.Joiner
import com.google.common.base.Strings
import com.google.common.collect.Maps
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskProperties
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException
import net.sourceforge.ganttproject.util.ColorConvertion
import net.sourceforge.ganttproject.util.collect.Pair
import java.util.*
import java.util.logging.Level

/**
 * Class responsible for processing task records in CSV import
 *
 * @author dbarashev (Dmitry Barashev)
 */
class TaskRecords(
  private val taskManager: TaskManager,
  private val resourceManager: HumanResourceManager?,
  private val myTimeUnitStack: TimeUnitStack
) : RecordGroup(
  "Task group",
  TaskFields.values().map { it.toString() }.toSet(),
  setOf(TaskFields.NAME.toString(), TaskFields.BEGIN_DATE.toString()),
  null
) {
  /** List of known (and supported) Task attributes  */
  enum class TaskFields(protected val text: String) {
    ID(TaskDefaultColumn.ID.nameKey),
    NAME("tableColName"),
    BEGIN_DATE("tableColBegDate"),
    END_DATE("tableColEndDate"),
    WEB_LINK("webLink"),
    NOTES("notes"),
    COMPLETION("tableColCompletion"),
    COORDINATOR("tableColCoordinator"),
    RESOURCES("resources"),
    ASSIGNMENTS("Assignments") {
      override fun toString(): String {
        return text
      }
    },
    DURATION("tableColDuration"),
    PREDECESSORS(TaskDefaultColumn.PREDECESSORS.nameKey),
    OUTLINE_NUMBER(TaskDefaultColumn.OUTLINE_NUMBER.nameKey),
    COST(TaskDefaultColumn.COST.nameKey),
    COLOR(TaskDefaultColumn.COLOR.nameKey);

    override fun toString(): String {
      // Return translated field name
      return GanttLanguage.getInstance().getText(text)
    }
  }

  private val myAssignmentMap: MutableMap<Task, AssignmentSpec?> = Maps.newHashMap()
  private val myPredecessorMap: MutableMap<Task, String?> = Maps.newHashMap()
  private val myWbsMap: SortedMap<String, Task> = Maps.newTreeMap(OUTLINE_NUMBER_COMPARATOR)
  private val myTaskIdMap: MutableMap<Int, Task> = Maps.newHashMap()

  override fun doProcess(record: SpreadsheetRecord): Boolean {
    if (!super.doProcess(record)) {
      return false
    }
    if (!hasMandatoryFields(record)) {
      return false
    }

    val startDate = record.digDate(TaskFields.BEGIN_DATE.toString(), this::addError)

      // Create the task
    var builder = taskManager.newTaskBuilder()
      .withName(record[TaskFields.NAME.toString()])
      .withStartDate(startDate)
      .withWebLink(record[TaskFields.WEB_LINK.toString()])
      .withNotes(record[TaskFields.NOTES.toString()])

    val duration =
      if (record.isSet(TaskDefaultColumn.DURATION.getName())) {
        record.getInt(TaskDefaultColumn.DURATION.getName())?.toString()
          ?: record[TaskDefaultColumn.DURATION.getName()]?.trim { it <= ' ' }
      } else ""

    if (!duration.isNullOrBlank()) {
      builder = builder.withDuration(taskManager.createLength(duration))
    }

    if (record.isSet(TaskFields.END_DATE.toString())) {
      if (!duration.isNullOrBlank()) {
        if (record.digDate(TaskFields.BEGIN_DATE.toString(), this::addError) == record.digDate(TaskFields.END_DATE.toString(), this::addError)
          && "0" == duration
        ) {
          builder = builder.withLegacyMilestone()
        }
      } else {
        val endDate = record.digDate(TaskFields.END_DATE.toString(), this::addError)
        if (endDate != null) {
          builder = builder.withEndDate(myTimeUnitStack.defaultTimeUnit.adjustRight(endDate))
        }
      }
    }

    if (record.isSet(TaskFields.COMPLETION.toString())) {
      record.getInt(TaskFields.COMPLETION.toString())?.let {
        builder = builder.withCompletion(it)
      }
    }

    if (record.isSet(TaskFields.COLOR.toString())) {
      getOrNull(record, TaskFields.COLOR.toString())?.let {
        if (ColorOption.Util.isValidColor(it)) {
          builder = builder.withColor(ColorConvertion.determineColor(it))
        }
      }
    }

    if (record.isSet(TaskDefaultColumn.COST.getName())) {
      try {
        record.getBigDecimal(TaskDefaultColumn.COST.getName())?.let {
          builder = builder.withCost(it)
        }
      } catch (e: NumberFormatException) {
        GPLogger.logToLogger(e)
        GPLogger.log(String.format("Failed to parse %s as cost value", record[TaskDefaultColumn.COST.getName()]))
      }
    }

    val task = builder.build()
    if (record.isSet(TaskDefaultColumn.ID.getName())) {
      record.getInt(TaskDefaultColumn.ID.getName())?.let {
        myTaskIdMap[it] = task
      }
    }
    myAssignmentMap[task] = parseAssignmentSpec(record)
    myPredecessorMap[task] = getOrNull(record, TaskDefaultColumn.PREDECESSORS.getName())
    val outlineNumber =
      getOrNull(record, TaskDefaultColumn.OUTLINE_NUMBER.getName())?.let {
        if (it.endsWith(".0")) {
          it.removeSuffix(".0")
        } else it
      }
    if (outlineNumber != null) {
      myWbsMap[outlineNumber] = task
    }
    readCustomProperties(
        headerRecord = header!!,
        customFields = customFields ?: emptyList(),
        customPropertyMgr = taskManager.customPropertyManager,
        record = record,
        receiver = task.customValues::addCustomProperty
    )
    return true
  }

  private fun parseAssignmentSpec(record: SpreadsheetRecord): AssignmentSpec {
    val assignmentsColumn = getOrNull(record, TaskFields.ASSIGNMENTS.toString())
    val coordinatorColumn = getOrNull(record, TaskFields.COORDINATOR.toString())
    if (!Strings.isNullOrEmpty(assignmentsColumn)) {
      return AssignmentColumnSpecImpl(assignmentsColumn!!, coordinatorColumn, errorOutput ?: mutableListOf())
    }
    val resourcesColumn = getOrNull(record, TaskFields.RESOURCES.toString())
    return if (!Strings.isNullOrEmpty(resourcesColumn)) {
      ResourceColumnSpecImpl(resourcesColumn!!, coordinatorColumn, errorOutput ?: mutableListOf())
    } else AssignmentSpec.VOID
  }

  override fun postProcess() {
    for ((outlineNumber, value) in myWbsMap) {
      val components = outlineNumber.split(".")
      if (components.size <= 1) {
        continue
      }
      val parentOutlineNumber = Joiner.on('.').join(components.subList(0, components.size - 1))
      val parentTask = myWbsMap[parentOutlineNumber] ?: continue
      taskManager.taskHierarchy.move(value, parentTask, 0)
    }
    if (resourceManager != null) {
      for ((key, value) in myAssignmentMap) {
        if (value == null) {
          continue
        }
        value.apply(key, resourceManager)
      }
    }
    val taskIndex = Function<Int, Task?> {
      myTaskIdMap[it]
    }
    for ((successor, value) in myPredecessorMap) {
      if (value == null) {
        continue
      }
      val depSpecs = value.split(";")
      try {
        val constructors = TaskProperties.parseDependencies(
          depSpecs, successor, taskIndex
        )
        for (constructor in constructors.values) {
          constructor.get()
        }
      } catch (e: IllegalArgumentException) {
        GPLogger.logToLogger(
          String.format(
            "%s\nwhen parsing predecessor specification %s of task %s",
            e.message, value, successor
          )
        )
      } catch (e: TaskDependencyException) {
        GPLogger.logToLogger(e)
      }
    }
  }
}

private interface AssignmentSpec {
  fun apply(task: Task, resourceManager: HumanResourceManager)

  companion object {
    val VOID: AssignmentSpec = object : AssignmentSpec {
      override fun apply(task: Task, resourceManager: HumanResourceManager) {
        // Do nothing.
      }
    }
  }
}

private class AssignmentColumnSpecImpl(
  private val myValue: String,
  private val myCoordinator: String?,
  private val myErrors: MutableList<Pair<Level, String>>
) : AssignmentSpec {
  override fun apply(task: Task, resourceManager: HumanResourceManager) {
    val assignments = myValue.split(";").toTypedArray()
    for (item in assignments) {
      val idAndLoad = item.split(":").toTypedArray()
      if (idAndLoad.size != 2) {
        addError(
          myErrors, Level.SEVERE, String.format(
            "Malformed entry=%s in assignment cell=%s of task=%d", item, myValue, task.taskID
          )
        )
        continue
      }
      try {
        val resourceId = idAndLoad[0].toInt()
        val resource = resourceManager.getById(resourceId)
        if (resource == null) {
          addError(
            myErrors, Level.WARNING, String.format(
              "Resource not found by id=%d from assignment cell=%s of task=%d", item, myValue, task.taskID
            )
          )
          continue
        }
        val load = idAndLoad[1].toFloat()
        val assignment = task.assignmentCollection.addAssignment(resource)
        assignment.load = load
        if (myCoordinator != null && myCoordinator == resource.name) {
          assignment.isCoordinator = true
        }
      } catch (e: NumberFormatException) {
        addError(
          myErrors, Level.SEVERE, String.format(
            "Failed to parse number from assignment cell=%s of task=%d %n%s",
            item, task.taskID, e.message
          )
        )
      }
    }
  }
}

class ResourceColumnSpecImpl(
  private val myValue: String,
  private val myCoordinator: String?,
  private val myErrors: List<Pair<Level, String>>
) : AssignmentSpec {
  override fun apply(task: Task, resourceManager: HumanResourceManager) {
    val names = myValue.split(";").toTypedArray()
    for (name in names) {
      val resource = getIndexByName(resourceManager)!![name]
      if (resource != null) {
        val assignment = task.assignmentCollection.addAssignment(resource)
        if (myCoordinator != null && myCoordinator == name) {
          assignment.isCoordinator = true
        }
      }
    }
  }

  companion object {
    private var resourceMap: Map<String, HumanResource>? = null
    fun getIndexByName(resourceManager: HumanResourceManager): Map<String, HumanResource>? {
      if (resourceMap == null) {
        resourceMap = Maps.uniqueIndex(resourceManager.resources, object : Function<HumanResource, String> {
          override fun apply(input: HumanResource?): String? {
            return input?.name
          }
        })
      }
      return resourceMap
    }
  }
}

val OUTLINE_NUMBER_COMPARATOR: Comparator<String> = Comparator { s1, s2 ->
  Scanner(s1).useDelimiter("\\.").use { sc1 ->
    Scanner(s2).useDelimiter("\\.").use { sc2 ->
      while (sc1.hasNextInt() && sc2.hasNextInt()) {
        val diff = sc1.nextInt() - sc2.nextInt()
        if (diff != 0) {
          return@Comparator Integer.signum(diff)
        }
      }
      if (sc1.hasNextInt()) {
        return@Comparator 1
      }
      if (sc2.hasNextInt()) {
        return@Comparator -1
      }
      return@Comparator 0
    }
  }
}

fun SpreadsheetRecord.digDate(column: String, addError: (Level, String) -> Unit): Date? =
  this.getDate(column) ?: parseDateOrError(this[column], addError)

