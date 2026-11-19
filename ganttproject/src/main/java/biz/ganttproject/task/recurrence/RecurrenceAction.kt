/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

This file is part of GanttProject, an opensource project management tool.

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
package biz.ganttproject.task.recurrence

import net.sourceforge.ganttproject.action.task.TaskActionBase
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskSelectionManager

/**
 * Opens the recurrence dialog of the selected task. The dialog makes its own undoable edit, so the action does not.
 */
class TaskRecurrenceAction(taskManager: TaskManager, selectionManager: TaskSelectionManager, uiFacade: UIFacade)
  : TaskActionBase("task.recurrence", taskManager, selectionManager, uiFacade) {

  init {
    disableUndoableEdit()
  }

  override fun isEnabled(selection: List<Task>): Boolean = selection.size == 1 && canRecur(selection[0], taskManager)

  override fun run(selection: List<Task>) {
    showRecurrenceDialog(selection[0], taskManager, uiFacade.undoManager)
  }
}

/**
 * Whether a rule can be set on [task]. A repeated task and a group are made from a rule and do not get one of their
 * own; a summary task takes its dates from its subtasks, so its repetitions would not be what it shows.
 */
fun canRecur(task: Task, taskManager: TaskManager): Boolean =
  task.recurrenceOrigin == null && !taskManager.taskHierarchy.hasNestedTasks(task)
