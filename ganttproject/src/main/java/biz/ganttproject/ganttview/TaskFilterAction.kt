/*
Copyright 2022 BarD Software s.r.o, Alexander Popov

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
import net.sourceforge.ganttproject.action.GPAction
import java.awt.event.ActionEvent

/**
 * @author apopov77@gmail.com
 */
class TaskFilterAction(
  actionName: String,
  private val filterManager: TaskFilterManager,
  private val taskFilterOption: DefaultBooleanOption,
  private val taskFilter: TaskFilter
) : GPAction(actionName) {

  init {
    putValue(SELECTED_KEY, filterManager.activeFilter == taskFilter)
    filterManager.filterListeners.add { filter ->
      if (taskFilter != filter) {
        putValue(SELECTED_KEY, false)
      }
    }

    // An option can be saved and changed when a project will be open
    // or undo\redo
    taskFilterOption.addChangeValueListener { evt ->
      (evt.newValue as? Boolean)?.let {
        if (evt.newValue != evt.oldValue) {
          setChecked(it)
        }
      }
    }
  }

  override fun actionPerformed(e: ActionEvent?) {
    val isChecked = getValue(SELECTED_KEY)
    if (isChecked is Boolean) {
      setChecked(isChecked)
    }
  }

  override fun putValue(key: String?, newValue: Any?) {
    if (SELECTED_KEY == key) {
      if (newValue is Boolean) {
        taskFilterOption.value = newValue
        super.putValue(key, if (newValue) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE)
      }
    } else {
      super.putValue(key, newValue)
    }
  }

  internal fun setChecked(value: Boolean) {
    putValue(SELECTED_KEY, value)
    if (value) {
      filterManager.activeFilter = taskFilter
    } else {
      if (filterManager.activeFilter == taskFilter) {
        filterManager.activeFilter = VOID_FILTER
      }
    }
  }
}

class TaskFilterActionSet(taskFilterManager: TaskFilterManager) {
  // Task filters -> actions
  private val filterCompletedTasksAction = TaskFilterAction("taskTable.filter.uncompletedTasks",
    taskFilterManager, taskFilterManager.filterCompletedTasksOption, taskFilterManager.completedTasksFilter)
  private val filterDueTodayTasksAction = TaskFilterAction("taskTable.filter.dueTodayTasks",
    taskFilterManager, taskFilterManager.filterDueTodayOption, taskFilterManager.dueTodayFilter)
  private val filterOverdueTasksAction = TaskFilterAction("taskTable.filter.overdueTasks",
    taskFilterManager, taskFilterManager.filterOverdueOption, taskFilterManager.overdueFilter)
  private val filterInProgressTodayTasksAction = TaskFilterAction("taskTable.filter.inProgressTodayTasks",
    taskFilterManager, taskFilterManager.filterInProgressTodayOption, taskFilterManager.inProgressTodayFilter)



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

}