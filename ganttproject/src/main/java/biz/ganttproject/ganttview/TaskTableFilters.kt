/*
Copyright 2021 BarD Software s.r.o

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
package biz.ganttproject.ganttview

import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.core.option.GPObservable
import biz.ganttproject.core.option.GPOption
import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.SimpleSelect
import javafx.beans.property.SimpleIntegerProperty
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.ColumnConsumer
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.undo.GPUndoListener
import javax.swing.event.UndoableEditEvent

/**
 *
 */
data class TaskFilter(
  override var title: String,
  var description: String,
  val isEnabledProperty: GPObservable<Boolean>,
  val filterFxn: TaskFilterFxn,
  var expression: String? = null,
  val isBuiltIn: Boolean = false,
  override val cloneOf: TaskFilter? = null
  ): Item<TaskFilter> {

  override fun clone(): TaskFilter = copy(cloneOf = this)
}

typealias TaskFilterFxn = (parent: Task, child: Task?) -> Boolean
typealias FilterChangedListener = (filter: TaskFilter?) -> Unit

object BuiltInFilters {
  internal val filterCompletedTasksOption = DefaultBooleanOption("filter.completedTasks", false)
  internal val filterDueTodayOption = DefaultBooleanOption("filter.dueTodayTasks", false)
  internal val filterOverdueOption = DefaultBooleanOption("filter.overdueTasks", false)
  internal val filterInProgressTodayOption = DefaultBooleanOption("filter.inProgressTodayTasks", false)

  // ----------------------------
  private val completedTasksFilterFxn: TaskFilterFxn = { _, child ->
    child?.completionPercentage?.let { it < 100 } != false
  }
  val completedTasksFilter = TaskFilter(
    "filter.completedTasks", "", filterCompletedTasksOption.asObservableValue(), completedTasksFilterFxn, isBuiltIn = true
  )
  // ----------------------------
  private val dueTodayFilterFxn: TaskFilterFxn  = { _, child ->
    child?.let {
      it.completionPercentage < 100 && it.endsToday()
    } != false
  }
  val dueTodayFilter = TaskFilter(
    "filter.dueTodayTasks", "", filterDueTodayOption.asObservableValue(), dueTodayFilterFxn, isBuiltIn = true
  )
  // ----------------------------
  private val overdueFilterFxn: TaskFilterFxn  = { _, child ->
    child?.let { it.completionPercentage < 100 && it.endsBeforeToday()
    } != false
  }
  val overdueFilter = TaskFilter(
    "filter.overdueTasks", "", filterOverdueOption.asObservableValue(), overdueFilterFxn, isBuiltIn = true
  )
  // ----------------------------
  private val inProgressTodayFilterFxn: TaskFilterFxn  = { _, child ->
    child?.let {
      it.completionPercentage < 100 && it.runsToday()
    } != false
  }
  val inProgressTodayFilter = TaskFilter(
    "filter.inProgressTodayTasks", "", filterInProgressTodayOption.asObservableValue(), inProgressTodayFilterFxn, isBuiltIn = true
  )

  val allFilters: List<TaskFilter> get() = listOf(
    completedTasksFilter,
    dueTodayFilter,
    overdueFilter,
    inProgressTodayFilter,
  )
}

class TaskFilterManager(val taskManager: TaskManager, val projectDatabase: ProjectDatabase) {
  private val customFilterResults: MutableSet<Int> = mutableSetOf()
  private val customFilterFxn: TaskFilterFxn = { _, child ->
    child?.taskID?.let { customFilterResults.contains(it) } != false
  }

  val options: List<GPOption<*>> = listOf(
    BuiltInFilters.filterCompletedTasksOption,
    BuiltInFilters.filterDueTodayOption,
    BuiltInFilters.filterOverdueOption,
    BuiltInFilters.filterInProgressTodayOption)

  val filterListeners = mutableListOf<FilterChangedListener>()
  private val customFilters: MutableList<TaskFilter> = mutableListOf()

  // ----------------------------
  // How many tasks are filtered out.
  val hiddenTaskCount = SimpleIntegerProperty(0)

  // If we have a custom filter, we need to refresh its results on every undoable edit or undo/redo.
  val undoListener = object: GPUndoListener {
    override fun undoOrRedoHappened() {
      refreshCustomFilterResults()
    }
    override fun undoReset() {}

    override fun undoableEditHappened(e: UndoableEditEvent?) {
      refreshCustomFilterResults()
    }
  }

  var activeFilter: TaskFilter = VOID_FILTER
    set(value) {
      field = value
      if (!value.isBuiltIn) {
        refreshCustomFilterResults()
      }
      fireFilterChanged(value)
      sync()
    }

  val filters get() = BuiltInFilters.allFilters + customFilters

  init {
    taskManager.addTaskListener(TaskListenerAdapter().also {
      it.taskProgressChangedHandler = { _ -> if (activeFilter != VOID_FILTER) sync() }
      it.taskScheduleChangedHandler = { _ -> if (activeFilter != VOID_FILTER) sync() }
    })
  }

  fun createCustomFilter() = TaskFilter("", "", DefaultBooleanOption("", false),
    filterFxn = { parent, child -> customFilterFxn(parent, child)},
    isBuiltIn = false
  )

  private fun fireFilterChanged(value: TaskFilter) {
    filterListeners.forEach { it(value) }
  }

  internal fun importFilters(filters: List<TaskFilter>) {
    customFilters.clear()
    customFilters.addAll(filters.filter { !it.isBuiltIn })
    filters.find { it.isEnabledProperty.value }?.let {
      activeFilter = it
    }
  }

  private fun refreshCustomFilterResults() {
    customFilterResults.clear()
    if (!activeFilter.isBuiltIn) {
      LOGGER.debug(">>> refreshCustomFilterResults()")
      projectDatabase.mapTasks(
        ColumnConsumer(SimpleSelect("uid", "num", whereExpression = activeFilter.expression, CustomPropertyClass.INTEGER.javaClass)) { taskNum, value ->
          LOGGER.debug("... adding task={} to the results", taskNum)
          customFilterResults.add(taskNum)
        })
      LOGGER.debug("<<< refreshCustomFilterResults()")
    }

    sync()
//
  }
  internal var sync: ()->Unit = {}
}

private fun today() = CalendarFactory.createGanttCalendar(CalendarFactory.newCalendar().time)
private fun Task.endsToday() = this.end.displayValue == today()
private fun Task.endsBeforeToday() = this.end.displayValue < today()
private fun Task.runsToday() = today().let { this.end.displayValue >= it && this.start <= it }
val VOID_FILTER_FXN: TaskFilterFxn = { _, _ -> true }
val VOID_FILTER: TaskFilter = TaskFilter("filter.void", "", ObservableBoolean("", false), isBuiltIn = true, filterFxn = VOID_FILTER_FXN)
private val LOGGER = GPLogger.create("TaskTable.Filters")