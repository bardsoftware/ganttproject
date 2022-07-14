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

import biz.ganttproject.app.MenuBuilder
import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.core.option.GPOption
import biz.ganttproject.core.time.CalendarFactory
import javafx.beans.property.SimpleIntegerProperty
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter

typealias TaskFilter = (parent: Task, child: Task?) -> Boolean
typealias FilterChangedListener = (filter: TaskFilter?) -> Unit

class TaskFilterManager(val taskManager: TaskManager) {

  val options: List<GPOption<*>> get() = listOf(
    filterCompletedTasksOption,
    filterDueTodayOption,
    filterOverdueOption,
    filterInProgressTodayOption)

  val filterListeners = mutableListOf<FilterChangedListener>()

  private val filterCompletedTasksOption = DefaultBooleanOption("filter.completedTasks", false)
  val completedTasksFilter: TaskFilter = { _, child ->
    child?.completionPercentage?.let { it < 100 } ?: true
  }

  private val filterDueTodayOption = DefaultBooleanOption("filter.dueTodayTasks", false)
  val dueTodayFilter: TaskFilter  = { _, child ->
    child?.let {
      it.completionPercentage < 100 && it.endsToday()
    } ?: true
  }

  private val filterOverdueOption = DefaultBooleanOption("filter.overdueTasks", false)
  val overdueFilter: TaskFilter  = { _, child ->
    child?.let { it.completionPercentage < 100 && it.endsBeforeToday()
    } ?: true
  }

  private val filterInProgressTodayOption = DefaultBooleanOption("filter.inProgressTodayTasks", false)
  val inProgressTodayFilter: TaskFilter  = { _, child ->
    child?.let {
      it.completionPercentage < 100 && it.runsToday()
    } ?: true
  }

  // Task filters -> actions
  private val filterCompletedTasksAction = TaskFilterAction("taskTable.filter.completedTasks",
    this, filterCompletedTasksOption, completedTasksFilter)
  private val filterDueTodayTasksAction = TaskFilterAction("taskTable.filter.dueTodayTasks",
    this, filterDueTodayOption, dueTodayFilter)
  private val filterOverdueTasksAction = TaskFilterAction("taskTable.filter.overdueTasks",
    this, filterOverdueOption, overdueFilter)
  private val filterInProgressTodayTasksAction = TaskFilterAction("taskTable.filter.inProgressTodayTasks",
    this, filterInProgressTodayOption, inProgressTodayFilter)

  val hiddenTaskCount = SimpleIntegerProperty(0)

  init {
    taskManager.addTaskListener(TaskListenerAdapter().also {
      it.taskProgressChangedHandler = { _ -> if (activeFilter != VOID_FILTER) sync() }
      it.taskScheduleChangedHandler = { _ -> if (activeFilter != VOID_FILTER) sync() }
    })
  }

  var activeFilter: TaskFilter = VOID_FILTER
    set(value) {
      field = value
      fireFilterChanged(value)
      sync()
    }

  private fun fireFilterChanged(value: TaskFilter) {
    filterListeners.forEach { it(value) }
  }

  fun tableFilterActions(builder: MenuBuilder) {
    builder.apply {
      items(
        filterCompletedTasksAction,
        filterDueTodayTasksAction,
        filterOverdueTasksAction,
        filterInProgressTodayTasksAction,
      )
    }
  }

  internal var sync: ()->Unit = {}
}

private fun today() = CalendarFactory.createGanttCalendar(CalendarFactory.newCalendar().time)
private fun Task.endsToday() = this.end.displayValue == today()
private fun Task.endsBeforeToday() = this.end.displayValue < today()
private fun Task.runsToday() = today().let { this.end.displayValue >= it && this.start <= it }
val VOID_FILTER: TaskFilter = { _, _ -> true }