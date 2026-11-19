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

// ---------------------------------------------------------------------------------------------
// Named views. Built after the filter action set above, with two deliberate differences, both
// noted where they occur.
// ---------------------------------------------------------------------------------------------

/**
 * One menu entry per named view, with a check mark. Switching a view on switches the previous one
 * off -- a view is a place to stand in, not something to stack.
 */
class TaskViewAction(
  private val viewManager: TaskViewManager,
  private val taskView: NamedTaskView
) : GPAction(taskView.title) {

  init {
    // The title is user text, so it is no i18n key. GPAction has already asked the localizer in its
    // constructor and got null; the name is put in here afterwards, as TaskFilterAction does for a
    // custom filter.
    refresh()
  }

  /**
   * The entry is cached, and the management dialog renames and changes the very view it was made
   * for. So name, hidden count and check mark are read again whenever the menu is built.
   */
  internal fun refresh() {
    putValue(NAME, taskView.title)
    putValue(HELP_TEXT, RootLocalizer.formatText("taskView.hiddenCount", taskView.hiddenCount))
    putValue(SELECTED_KEY, viewManager.isActive(taskView))
  }

  override fun actionPerformed(e: ActionEvent?) {
    val isChecked = getValue(SELECTED_KEY)
    if (isChecked is Boolean) {
      setChecked(isChecked)
    }
  }

  override fun putValue(key: String?, newValue: Any?) {
    if (SELECTED_KEY == key && newValue is Boolean) {
      super.putValue(key, if (newValue) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE)
    } else {
      super.putValue(key, newValue)
    }
  }

  internal fun setChecked(value: Boolean) {
    putValue(SELECTED_KEY, value)
    if (value) {
      viewManager.activeView = taskView
    } else if (viewManager.isActive(taskView)) {
      viewManager.showAll()
    }
  }
}

/**
 * The drop-down of the views button in the toolbar above the task table.
 *
 * DIFFERENCE FROM TaskFilterActionSet, on purpose: the entries are cached per view instead of being
 * created afresh every time the menu opens. Every GPAction registers itself with GanttLanguage in
 * its constructor and is never unregistered, so building them again on each click would let that
 * list grow without end. The filter set does exactly that today; there is no reason to copy it.
 */
class TaskViewActionSet(private val viewManager: TaskViewManager) {

  private val actionCache = mutableMapOf<NamedTaskView, TaskViewAction>()

  private val manageAction = GPAction.create("taskView.manage") {
    showViewDialog(viewManager)
  }

  /** THE RETURN PATH, always present and always enabled, whatever a view hides. */
  private val showAllAction = GPAction.create("taskView.showAll") {
    viewManager.showAll()
  }

  fun tableViewActions(builder: MenuBuilder) {
    val viewActions = viewManager.views.map { view ->
      actionCache.getOrPut(view) { TaskViewAction(viewManager, view) }.also {
        it.refresh()
      }
    }
    actionCache.keys.retainAll(viewManager.views.toSet())
    builder.apply {
      items(viewActions)
      if (viewActions.isNotEmpty()) {
        separator()
      }
      items(showAllAction, manageAction)
    }
  }
}
