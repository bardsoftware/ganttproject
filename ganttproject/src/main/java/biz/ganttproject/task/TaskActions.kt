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
package biz.ganttproject.task

import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.ganttview.TaskTableActionConnector
import net.sourceforge.ganttproject.CustomPropertyManager
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ShowHideColumnsDialog
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.action.resource.AssignmentToggleAction
import net.sourceforge.ganttproject.action.task.*
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.view.GPViewManager
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskSelectionManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.awt.event.ActionEvent
import javax.swing.Action

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskActions(private val project: IGanttProject,
                  private val uiFacade: UIFacade,
                  private val selectionManager: TaskSelectionManager,
                  private val viewManager: () -> GPViewManager,
                  private val tableConnector: () -> TaskTableActionConnector) {
  val createAction = TaskNewAction(project, uiFacade, tableConnector)
  val propertiesAction = TaskPropertiesAction(project, selectionManager, uiFacade)
  val deleteAction = TaskDeleteAction(project.taskManager, selectionManager, uiFacade)
  val indentAction = TaskIndentAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val unindentAction = TaskUnindentAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val moveUpAction = TaskMoveUpAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val moveDownAction = TaskMoveDownAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val copyAction get() = viewManager().copyAction
  val cutAction get() = viewManager().cutAction
  val pasteAction get() = viewManager().pasteAction
  val linkTasksAction = TaskLinkAction(project.taskManager, selectionManager, uiFacade)
  val unlinkTasksAction = TaskUnlinkAction(project.taskManager, selectionManager, uiFacade)

  val manageColumnsAction get() = ManageColumnsAction(uiFacade, tableConnector().columnList, project.taskCustomColumnManager)

  fun all() = listOf(propertiesAction, indentAction, unindentAction, moveDownAction, moveUpAction, linkTasksAction, unlinkTasksAction)
  fun assignments(task: Task, hrManager: HumanResourceManager, undoManager: GPUndoManager): List<GPAction> {
    val human2action = hrManager.resources.associateWith {
      AssignmentToggleAction(it, task, undoManager).apply {
        putValue(Action.SELECTED_KEY, false)
      }
    }

    for (ra in task.assignmentCollection.assignments) {
      human2action[ra.resource]?.putValue(Action.SELECTED_KEY, true)
    }
    return human2action.values.toList()
  }
}

class ManageColumnsAction(
  private val uiFacade: UIFacade,
  private val columnList: () -> ColumnList,
  private val customPropertyManager: CustomPropertyManager) : GPAction("columns.manage.label") {

  override fun actionPerformed(e: ActionEvent?) {
    val dialog = ShowHideColumnsDialog(
      uiFacade, columnList(), customPropertyManager
    )
    dialog.show()
  }
}
