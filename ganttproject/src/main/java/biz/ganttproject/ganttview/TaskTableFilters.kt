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

import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent
import java.awt.event.ActionEvent
import javax.swing.Action

/**
 * @author dbarashev@bardsoftware.com
 */
class FilterCompletedTasks(
  private val taskTable: TaskTable,
  private val taskManager: TaskManager) : GPAction("taskTable.filter.completedTasks") {

  private val taskListener = object : TaskListenerAdapter() {
    override fun taskProgressChanged(e: TaskPropertyEvent?) {
      val isChecked = getValue(Action.SELECTED_KEY)
      if (isChecked is java.lang.Boolean && isChecked.booleanValue()) {
        taskTable.sync()
      }
    }
  }

  init {
    putValue(Action.SELECTED_KEY, java.lang.Boolean.FALSE)
    taskManager.addTaskListener(this.taskListener)
  }

  override fun actionPerformed(e: ActionEvent?) {
    val isChecked = getValue(Action.SELECTED_KEY)
    if (isChecked is java.lang.Boolean) {
      if (isChecked.booleanValue()) {
        taskTable.activeFilter = { _, child ->
          child?.completionPercentage?.let { it < 100 } ?: true
        }
      } else {
        taskTable.activeFilter = { _, _ -> true }
      }
    }
  }

  fun setChecked(value: Boolean) {
    putValue(Action.SELECTED_KEY, value)
  }
}

typealias TaskFilter = (parent: Task, child: Task?) -> Boolean
val VOID_FILTER: TaskFilter = { _, _ -> true }