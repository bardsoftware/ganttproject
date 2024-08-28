/*
 * Copyright 2021-2024 BarD Software s.r.o., Dmitry Barashev.
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

package biz.ganttproject.ganttview

import biz.ganttproject.FXUtil
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.customproperty.CustomPropertyManager
import net.sourceforge.ganttproject.GPLogger
import biz.ganttproject.customproperty.CustomColumnsException
import biz.ganttproject.customproperty.CustomPropertyDefinition
import net.sourceforge.ganttproject.task.CostStub
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskProperties
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException
import java.math.BigDecimal
import java.text.MessageFormat
import java.util.function.Predicate
import java.util.function.Supplier

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTableModel(private val customColumnsManager: CustomPropertyManager) {
  fun getValueAt(t: Task, defaultColumn: TaskDefaultColumn): Any? =
    when (defaultColumn) {
      TaskDefaultColumn.PRIORITY       -> t.priority
      TaskDefaultColumn.INFO           -> t.getProgressStatus()
      TaskDefaultColumn.NAME           -> t.name
      TaskDefaultColumn.BEGIN_DATE     -> t.start
      TaskDefaultColumn.END_DATE       -> t.displayEnd
      TaskDefaultColumn.DURATION       -> t.duration
      TaskDefaultColumn.COMPLETION     -> t.completionPercentage
      TaskDefaultColumn.COORDINATOR    -> t.assignments.joinToString(", ") { it.resource.name }
      TaskDefaultColumn.PREDECESSORS   -> TaskProperties.formatPredecessors(t, ",", true)
      TaskDefaultColumn.ID             -> t.taskID
      TaskDefaultColumn.OUTLINE_NUMBER -> t.manager.taskHierarchy.getOutlinePath(t).joinToString(".")
      TaskDefaultColumn.COST           -> t.cost.value
      TaskDefaultColumn.COLOR          -> t.color
      TaskDefaultColumn.RESOURCES      -> t.assignments.joinToString(",") { it?.resource?.name ?: "" }
      else -> {
        null
      }
    }

  fun getValue(t: Task, customProperty: CustomPropertyDefinition): Any? {
    return t.customValues.getValue(customProperty)
  }

  fun setValue(value: Any, task: Task, property: TaskDefaultColumn) {
    fun runInUiThread(code: ()->Unit) = FXUtil.runLater(code)

    when (property) {
      TaskDefaultColumn.NAME -> task.createMutator().also {
        it.setName(value.toString())
        it.commit()
      }
      TaskDefaultColumn.BEGIN_DATE -> {
        val startDate = value as GanttCalendar
        val earliestStart = if (task.thirdDateConstraint == 1) task.third else null

        runInUiThread {
          task.createMutatorFixingDuration().let {
            it.setStart(minOf(startDate, earliestStart ?: startDate))
            it.commit()
          }
        }
      }
      TaskDefaultColumn.END_DATE -> {
        runInUiThread {
          task.createMutatorFixingDuration().let {
            it.setEnd(CalendarFactory.createGanttCalendar(
              GPTimeUnitStack.DAY.adjustRight((value as GanttCalendar).time)
            ))
            it.commit()
          }
        }
      }
      TaskDefaultColumn.DURATION -> {
        val tl = task.duration
        runInUiThread {
          task.createMutator().let {
            it.setDuration(task.manager.createLength(tl.timeUnit, (value as Number).toInt().toFloat()))
            it.commit()
          }
        }
      }
      TaskDefaultColumn.COMPLETION -> task.createMutator().let {
        it.completionPercentage = (value as Number).toInt()
        it.commit()
      }
      TaskDefaultColumn.PREDECESSORS -> {
        //List<Integer> newIds = Lists.newArrayList();
        val specs: MutableList<String> = mutableListOf()
        for (s in value.toString().split(",".toRegex()).toTypedArray()) {
          if (!s.trim { it <= ' ' }.isEmpty()) {
            specs.add(s.trim { it <= ' ' })
          }
        }
        val promises: Map<Int, Supplier<TaskDependency>>
        try {
          promises = TaskProperties.parseDependencies(
            specs, task
          ) { id -> task.manager.getTask(id!!) }
          runInUiThread {
            try {
              val taskManager = task.manager
              taskManager.algorithmCollection.scheduler.setEnabled(false)
              task.dependenciesAsDependant.clear()
              for (promise in promises.values) {
                promise.get()
              }
              taskManager.algorithmCollection.scheduler.setEnabled(true)
            } catch (e: TaskDependencyException) {
              // TODO: propagate this exception to the UI
              throw ValidationException(e)
            }
          }
        } catch (e: IllegalArgumentException) {
          throw ValidationException(e)
        }
      }
      TaskDefaultColumn.COST -> try {
        val cost = BigDecimal(value.toString())
        task.createMutator().also {
          it.setCost(CostStub(cost, false))
          it.commit()
        }
      } catch (e: NumberFormatException) {
        throw ValidationException(MessageFormat.format("Can't parse {0} as number", value))
      }
      else -> {
      }
    }
  }

  fun setValue(value: Any, task: Task, column: CustomPropertyDefinition) {
    if (column.calculationMethod != null) {
      throw ValidationException("This is a calculated column")
    }
    try {
      val customValues = task.customValues.copyOf()
      customValues.setValue(column, value)
      task.createMutator().let {
        it.setCustomProperties(customValues)
        it.commit()
      }
    } catch (e: CustomColumnsException) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err)
      }
    }
  }

  private fun getCustomProperty(columnIndex: Int): CustomPropertyDefinition? {
    var columnIndex = columnIndex
    assert(columnIndex >= STANDARD_COLUMN_COUNT) {
      ("We have " + STANDARD_COLUMN_COUNT + " default properties, and custom property index starts at " + STANDARD_COLUMN_COUNT + ". I've got index #"
          + columnIndex + ". Something must be wrong here")
    }
    val definitions: List<CustomPropertyDefinition> = customColumnsManager.definitions
    columnIndex -= STANDARD_COLUMN_COUNT
    return if (columnIndex < definitions.size) definitions[columnIndex] else null
  }

}

fun Task.getProgressStatus(): Task.ProgressStatus =
  if (completionPercentage < 100) {
    val c = GanttCalendar.getInstance()
    if (end.before(c)) {
      Task.ProgressStatus.DEADLINE_MISS
    } else {
      if (start.before(c)) {
        Task.ProgressStatus.INPROGRESS
      } else null
    }
  } else { null } ?: Task.ProgressStatus.NOT_YET

private val STANDARD_COLUMN_COUNT = TaskDefaultColumn.entries.size

val NOT_SUPERTASK: Predicate<Task> = Predicate<Task> { task ->
  task?.isSupertask?.not() ?: false
}

val NOT_MILESTONE: Predicate<Task> = Predicate<Task> { task ->
  task?.isMilestone?.not() ?: false
}

//fun getEnumValues(enumClass: KClass<out Enum<*>>): Array<out Enum<*>> = enumClass.java.enumConstants
