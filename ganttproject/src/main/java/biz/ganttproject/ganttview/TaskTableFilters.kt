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
import biz.ganttproject.core.option.GPOption
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.SimpleSelect
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.ColumnConsumer
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.undo.UndoableEditTxn
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Encapsulates the task filter properties.
 */
data class TaskFilter(
  override var title: String,
  var description: String,
  override val isEnabledProperty: BooleanProperty = SimpleBooleanProperty(false),
  val filterFxn: TaskFilterFxn,
  var expression: String? = null,
  val isBuiltIn: Boolean = false,
  ): Item<TaskFilter>

typealias TaskFilterFxn = (parent: Task, child: Task?) -> Boolean
typealias FilterChangedListener = (filter: TaskFilter?) -> Unit

/**
 * A few built-in task filters that do not use a database.
 */
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
    "filter.completedTasks", "", filterCompletedTasksOption.asJavafxProperty(), completedTasksFilterFxn, isBuiltIn = true
  )
  // ----------------------------
  private val dueTodayFilterFxn: TaskFilterFxn  = { _, child ->
    child?.let {
      it.completionPercentage < 100 && it.endsToday()
    } != false
  }
  val dueTodayFilter = TaskFilter(
    "filter.dueTodayTasks", "", filterDueTodayOption.asJavafxProperty(), dueTodayFilterFxn, isBuiltIn = true
  )
  // ----------------------------
  private val overdueFilterFxn: TaskFilterFxn  = { _, child ->
    child?.let { it.completionPercentage < 100 && it.endsBeforeToday()
    } != false
  }
  val overdueFilter = TaskFilter(
    "filter.overdueTasks", "", filterOverdueOption.asJavafxProperty(), overdueFilterFxn, isBuiltIn = true
  )
  // ----------------------------
  private val inProgressTodayFilterFxn: TaskFilterFxn  = { _, child ->
    child?.let {
      it.completionPercentage < 100 && it.runsToday()
    } != false
  }
  val inProgressTodayFilter = TaskFilter(
    "filter.inProgressTodayTasks", "", filterInProgressTodayOption.asJavafxProperty(), inProgressTodayFilterFxn, isBuiltIn = true
  )

  val allFilters: List<TaskFilter> get() = listOf(
    completedTasksFilter,
    dueTodayFilter,
    overdueFilter,
    inProgressTodayFilter,
  )
}

/**
 * Manages the filters, both built-in and custom.
 */
class TaskFilterManager(private val taskManager: TaskManager, private val projectDatabase: ProjectDatabase) {
  // This is a set of task IDs that are retained by the current active filter.
  private val filterResults: MutableSet<Int> = mutableSetOf()
  private val hasUpdatesInTxn = AtomicBoolean(false)

  val filterFxn: TaskFilterFxn = { _, child ->
    if (hasUpdatesInTxn.get()) {
      refreshCustomFilterResults()
    }
    (child?.taskID?.let { filterResults.contains(it) } != false).also {
      if (it) LOGGER.debug("Custom filter returned true for task $child. currently filtered tasks: $filterResults")
    }
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

  fun createUndoTxnFactory(): UndoableEditTxnFactory = {
    UndoableEditTxnImpl()
  }

  // This is the filter that is currently active.
  var activeFilter: TaskFilter = VOID_FILTER
    set(value) {
      field = value
      if (value != VOID_FILTER) {
        recentFilterList.remove(value)
        recentFilterList.add(0, value)
        while (recentFilterList.size > RECENT_FILTER_LIST_SIZE) { recentFilterList.removeLast() }
      }
      refreshCustomFilterResults()
      fireFilterChanged(value)
      sync()
    }

  // All available filters.
  val filters get() = BuiltInFilters.allFilters + customFilters

  private val recentFilterList = mutableListOf<TaskFilter>().also { it.addAll(BuiltInFilters.allFilters) }
  // The recent filters that are shown in the popup menu.
  val recentFilters: List<TaskFilter> get() = recentFilterList

  init {
    taskManager.addTaskListener(TaskListenerAdapter().also {
      it.taskProgressChangedHandler = { _ ->
        hasUpdatesInTxn.set(true)
        if (activeFilter != VOID_FILTER) sync()
      }
      it.taskScheduleChangedHandler = { _ ->
        hasUpdatesInTxn.set(true)
        if (activeFilter != VOID_FILTER) sync()
      }
      it.taskAddedHandler = {
        hasUpdatesInTxn.set(true)
      }
    })
  }

  fun createCustomFilter() = TaskFilter("", "",
    filterFxn = { parent, child -> filterFxn(parent, child)},
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

  fun refresh() = refreshCustomFilterResults()

  private fun refreshCustomFilterResults() {
    LOGGER.debug(">>> refresh()")
    filterResults.clear()

    val retainedTasks = mutableListOf<Task>()
    if (!activeFilter.isBuiltIn) {
      LOGGER.debug("... refreshing custom filter")
      LOGGER.debug("... active filter={}", activeFilter)
      projectDatabase.mapTasks(
        ColumnConsumer(SimpleSelect("uid", "num", whereExpression = activeFilter.expression, CustomPropertyClass.INTEGER.javaClass)) { taskNum, value ->
          LOGGER.debug("... adding task={} to the results", taskNum)
          taskManager.getTask(taskNum)?.let(retainedTasks::add)
        })
    } else {
      taskManager.tasks.forEach { task ->
        val parent = taskManager.taskHierarchy.getContainer(task)
        if (activeFilter.filterFxn(parent, task)) {
          retainedTasks.add(task)
        }
      }
    }

    val retainedTaskNums = retainedTasks.map { it.taskID }.toMutableSet()
    val ancestors = retainedTasks.mapNotNull { task ->
      val parent = taskManager.taskHierarchy.getContainer(task)
      if (!retainedTaskNums.contains(parent?.taskID)) {
        retainedTaskNums.add(parent.taskID)
        parent
      } else null
    }.toMutableList()

    while (ancestors.isNotEmpty()) {
      val next = ancestors.removeFirst()
      taskManager.taskHierarchy.getContainer(next)?.let { parent ->
        if (!retainedTaskNums.contains(parent.taskID)) {
          ancestors.add(parent)
          retainedTasks.add(parent)
          retainedTaskNums.add(parent.taskID)
        }
      }
    }
    LOGGER.debug("<<< refresh()")

    filterResults.clear()
    filterResults.addAll(retainedTaskNums)
  }
  internal var sync: ()->Unit = {}

  private inner class UndoableEditTxnImpl : UndoableEditTxn {
    override fun start(displayName: String) {}

    override fun commit() {
      refreshCustomFilterResults()
      hasUpdatesInTxn.set(false)
    }

    override fun rollback(ex: Throwable) {}

    override fun undo() {
      refreshCustomFilterResults()
    }

    override fun redo() {
      refreshCustomFilterResults()
    }
  }
}

private fun DefaultBooleanOption.asJavafxProperty() = SimpleBooleanProperty(this.value).also {
  it.subscribe { oldValue, newValue ->
    if (newValue != oldValue) {
      this.setValue(newValue, it)
    }
  }
  this.asObservableValue().addWatcher { evt ->
    if (evt.newValue != evt.oldValue && evt.trigger != it) {
      it.value = evt.newValue
    }
  }
}
private fun today() = CalendarFactory.createGanttCalendar(CalendarFactory.newCalendar().time)
private fun Task.endsToday() = this.end.displayValue == today()
private fun Task.endsBeforeToday() = this.end.displayValue < today()
private fun Task.runsToday() = today().let { this.end.displayValue >= it && this.start <= it }
val VOID_FILTER_FXN: TaskFilterFxn = { _, _ -> true }
val VOID_FILTER: TaskFilter = TaskFilter("filter.void", "", isBuiltIn = true, filterFxn = VOID_FILTER_FXN)
private const val RECENT_FILTER_LIST_SIZE = 5
private val LOGGER = GPLogger.create("TaskTable.Filters")