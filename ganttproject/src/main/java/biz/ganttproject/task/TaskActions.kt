package biz.ganttproject.task

import biz.ganttproject.ganttview.TaskTableActionConnector
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.task.*
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.task.TaskSelectionManager

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskActions(private val project: IGanttProject,
                  private val uiFacade: UIFacade,
                  private val selectionManager: TaskSelectionManager,
                  tableConnector: () -> TaskTableActionConnector) {
  val createAction = TaskNewAction(project, uiFacade)
  val propertiesAction = TaskPropertiesAction(project, selectionManager, uiFacade)
  val deleteAction = TaskDeleteAction(project.taskManager, selectionManager, uiFacade)
  val indentAction = TaskIndentAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val unindentAction = TaskUnindentAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val moveUpAction = TaskMoveUpAction(project.taskManager, selectionManager, uiFacade, tableConnector)
  val moveDownAction = TaskMoveDownAction(project.taskManager, selectionManager, uiFacade, tableConnector)

  fun all() = listOf(createAction, propertiesAction, deleteAction, indentAction, unindentAction, moveDownAction, moveUpAction)
}
