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
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent
import java.awt.event.ActionEvent

/**
 * @author dbarashev@bardsoftware.com
 */
internal class TaskFilterAction(
  actionName: String,
  private val filterManager: TaskFilterManager,
  private val taskFilterOption: DefaultBooleanOption,
  private val taskFilter: TaskFilter
) : GPAction(actionName) {


  private val taskListener = TaskListenerAdapter().also {
    it.taskProgressChangedHandler = { e: TaskPropertyEvent ->
      val isChecked = getValue(SELECTED_KEY)
      if (isChecked is java.lang.Boolean && isChecked.booleanValue()) {
        filterManager.sync()
      }
    }
  }

  init {
    putValue(SELECTED_KEY, java.lang.Boolean.FALSE)
    filterManager.taskManager.addTaskListener(this.taskListener)
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
    if (isChecked is java.lang.Boolean) {
      setChecked(isChecked.booleanValue())
    }
  }

  override fun putValue(key: String?, newValue: Any?) {
    super.putValue(key, newValue)
    if (SELECTED_KEY == key && newValue is Boolean) {
      taskFilterOption.value = newValue
    }
  }

  fun setChecked(value: Boolean) {
    putValue(SELECTED_KEY, value)
    if (value) {
      filterManager.activeFilter = taskFilter
    } else {
      filterManager.activeFilter = VOID_FILTER
    }
  }
}
