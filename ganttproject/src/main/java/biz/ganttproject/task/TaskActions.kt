package biz.ganttproject.task

import biz.ganttproject.ganttview.TaskTableActionConnector
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.action.resource.AssignmentToggleAction
import net.sourceforge.ganttproject.action.task.*
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.view.GPViewManager
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskSelectionManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import javax.swing.Action

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskActions(private val project: IGanttProject,
                  private val uiFacade: UIFacade,
                  private val selectionManager: TaskSelectionManager,
                  private val viewManager: () -> GPViewManager,
                  tableConnector: () -> TaskTableActionConnector) {
  val createAction = TaskNewAction(project, uiFacade)
  val propertiesAction = TaskPropertiesAction(project, selectionManager, uiFacade)
  val deleteAction = TaskDeleteAction(project.taskManager, selectionManager, uiFacade)
  val indentAction = TaskIndentAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val unindentAction = TaskUnindentAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val moveUpAction = TaskMoveUpAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val moveDownAction = TaskMoveDownAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val copyAction get() = viewManager().copyAction
  val cutAction get() = viewManager().cutAction
  val pasteAction get() = viewManager().pasteAction

  fun all() = listOf(propertiesAction, indentAction, unindentAction, moveDownAction, moveUpAction)
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
