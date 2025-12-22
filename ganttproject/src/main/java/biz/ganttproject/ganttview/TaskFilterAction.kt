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
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.customproperty.CustomPropertyManager
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.storage.ProjectDatabase
import java.awt.event.ActionEvent

/**
 * @author apopov77@gmail.com
 */
class TaskFilterAction(
  private val filterManager: TaskFilterManager,
  private val taskFilter: TaskFilter
) : GPAction(taskFilter.title) {

  init {
    putValue(SELECTED_KEY, filterManager.activeFilter == taskFilter)
    putValue(NAME, taskFilter.getLocalizedTitle())
    putValue(HELP_TEXT, taskFilter.getLocalizedDescription())

    filterManager.filterListeners.add { filter ->
      if (taskFilter != filter) {
        putValue(SELECTED_KEY, false)
      }
    }

    // An option can be saved and changed when a project will be open
    // or undo\redo
    taskFilter.isEnabledProperty.subscribe { oldValue, newValue ->
      newValue?.let {
        if (newValue != oldValue) {
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
        taskFilter.isEnabledProperty.value = newValue
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

class TaskFilterActionSet(
  private val taskFilterManager: TaskFilterManager,
  customPropertyManager: CustomPropertyManager,
  projectDatabase: ProjectDatabase
) {
  // Task filters -> actions
  private val filterDialogAction = GPAction.create("taskTable.filterDialog.action") {
    showFilterDialog(taskFilterManager, customPropertyManager, projectDatabase)
  }


  fun tableFilterActions(builder: MenuBuilder) {
    val recentFilters = taskFilterManager.recentFilters.map { filter ->
      TaskFilterAction(taskFilterManager, filter)
    }
    builder.apply {
      items(recentFilters + listOf(filterDialogAction))
    }
  }
}

internal fun TaskFilter.getLocalizedTitle(): String =
  if (this.isBuiltIn) {
    val suffix = if (this.title == "filter.completedTasks") "filter.uncompletedTasks" else this.title
    RootLocalizer.createWithRootKey("taskTable", RootLocalizer).formatText(suffix)
  } else this.title

internal fun TaskFilter.getLocalizedDescription(): String =
  if (this.isBuiltIn) {
    val suffix = if (this.title == "filter.completedTasks") "filter.uncompletedTasks" else this.title
    RootLocalizer.createWithRootKey("taskTable", RootLocalizer).formatText("$suffix.help")
  } else this.description
