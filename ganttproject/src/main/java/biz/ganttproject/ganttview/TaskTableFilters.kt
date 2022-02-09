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
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent
import java.awt.event.ActionEvent
import javax.swing.Action

typealias TaskFilter = (parent: Task, child: Task?) -> Boolean

class TaskFilterManager(taskManager: TaskManager) {
  val options: List<GPOption<*>> get() = listOf(filterCompletedTasksOption)

  val filterCompletedTasksOption = DefaultBooleanOption("filter.completedTasks", false)
  val filterCompletedTasksAction = FilterCompletedTasks(this, taskManager)
  var activeFilter: TaskFilter = VOID_FILTER
    set(value) {
      field = value
      sync()
    }

  internal var sync: ()->Unit = {}
}
/**
 * @author dbarashev@bardsoftware.com
 */
class FilterCompletedTasks(
  private val filterManager: TaskFilterManager,
  taskManager: TaskManager) : GPAction("taskTable.filter.completedTasks") {

  private val taskListener = TaskListenerAdapter().also {
    it.taskProgressChangedHandler = { e: TaskPropertyEvent ->
      val isChecked = getValue(Action.SELECTED_KEY)
      if (isChecked is java.lang.Boolean && isChecked.booleanValue()) {
        filterManager.sync()
      }
    }
  }

  init {
    putValue(Action.SELECTED_KEY, java.lang.Boolean.FALSE)
    taskManager.addTaskListener(this.taskListener)
    filterManager.filterCompletedTasksOption.addChangeValueListener { evt ->
      (evt.newValue as? Boolean)?.let {
        if (evt.newValue != evt.oldValue) {
          setChecked(it)
        }
      }
    }
  }

  override fun actionPerformed(e: ActionEvent?) {
    val isChecked = getValue(Action.SELECTED_KEY)
    if (isChecked is java.lang.Boolean) {
      setChecked(isChecked.booleanValue())
    }
  }

  override fun putValue(key: String?, newValue: Any?) {
    super.putValue(key, newValue)
    if (Action.SELECTED_KEY == key && newValue is Boolean) {
      filterManager.filterCompletedTasksOption.value = newValue
    }
  }

  fun setChecked(value: Boolean) {
    putValue(Action.SELECTED_KEY, value)
    if (value) {
      filterManager.activeFilter = { _, child ->
        child?.completionPercentage?.let { it < 100 } ?: true
      }
    } else {
      filterManager.activeFilter = VOID_FILTER
    }
  }
}

val VOID_FILTER: TaskFilter = { _, _ -> true }
