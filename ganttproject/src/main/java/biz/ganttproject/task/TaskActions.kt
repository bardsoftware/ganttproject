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

import biz.ganttproject.ganttview.NewTaskActor
import biz.ganttproject.ganttview.TaskTableActionConnector
import biz.ganttproject.ganttview.showTaskColumnManager
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.action.resource.AssignmentToggleAction
import net.sourceforge.ganttproject.action.task.*
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.view.GPViewManager
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskSelectionManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.Action

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskActions(private val project: IGanttProject,
                  private val uiFacade: UIFacade,
                  selectionManager: TaskSelectionManager,
                  private val viewManager: () -> GPViewManager,
                  private val tableConnector: () -> TaskTableActionConnector,
                  newTaskActor: NewTaskActor<Task>,
                  private val projectDatabase: ProjectDatabase) {
  val createAction = TaskNewAction(project, uiFacade, newTaskActor)
  val propertiesAction = TaskPropertiesAction(project, selectionManager, uiFacade)
  val deleteAction = TaskDeleteAction(project.taskManager, selectionManager, uiFacade)

  /**
   * "Indents" the selection, that is, moves tasks downwards in the task tree which in the UI looks
   * as if they move rightwards (get indented)
   *
   * 1. For a single selected task T, this action is enabled if there is a task S
   * which is a sibling of T (that is, they both have the same parent P) and sits
   * immediately before T in the list of P's children (that is, T is not the first child).
   * This action moves T under S.
   *
   * 2. For multiple selected tasks, if they are all children of the same task P and form a
   * consecutive range of P's children, it works as if they all were at the place of the first task
   * in the range.
   *
   * 3. For multiple selected tasks, which are not in a consecutive range of siblings, it works by partitioning
   * the selection into consecutive ranges of siblings, and indenting every range as written above.
   */
  val indentAction: GPAction = TaskMoveAction("task.indent", project.taskManager, selectionManager, uiFacade, tableConnector,
    isEnabledPredicate = { selection ->
      TaskMoveEnabledPredicate(project.taskManager, IndentTargetFunctionFactory(project.taskManager)).test(selection)
    },
    onAction = { selection ->
      indent(selection, project.taskManager.taskHierarchy)
      selection.first()
    }
  )

  /**
   * "Outdents" the selected tasks by moving them upwards in the task tree, which in the UI looks
   * as if they move leftwards (indent is decreased).
   *
   * 1. For a single selected task T, this action is enabled if there is a parent task P.
   * This action moves T upwards, so that it becomes a sibling of P and immediately follows
   * P in the list of P's siblings.
   *
   * 2. For multiple selected tasks, if they are all children of the same task P and form a
   * consecutive range of P's children, it works as if they all were at the place of the first task
   * in the range.
   *
   * 3. For multiple selected tasks, which are not in a consecutive range of siblings, it works by partitioning
   * the selection into consecutive ranges of siblings, and indenting every range as written above.
   */
  val unindentAction: GPAction = TaskMoveAction("task.unindent", project.taskManager, selectionManager, uiFacade, tableConnector,
    isEnabledPredicate = { selection ->
      TaskMoveEnabledPredicate(project.taskManager, OutdentTargetFunctionFactory(project.taskManager)).test(selection)
    },
    onAction = { selection ->
      unindent(selection, project.taskManager.taskHierarchy)
      selection.first()
    }
  )

  /**
   * Swaps the selected tasks with their siblings, moving closer to the beginning of the list
   * of siblings. In the UI it looks as if they move upwards.
   */
  val moveUpAction: GPAction = TaskMoveAction("task.move.up", project.taskManager, selectionManager, uiFacade, tableConnector,
    isEnabledPredicate = { selection ->
      selection.find { project.taskManager.taskHierarchy.getPreviousSibling(it) == null } == null
    },
    onAction = { selection ->
      val taskHierarchy = project.taskManager.taskHierarchy
      selection.forEach { task ->
        val parent = taskHierarchy.getContainer(task)
        val index = taskHierarchy.getTaskIndex(task) - 1
        taskHierarchy.move(task, parent, index)
      }
      selection.first()
    }
  )

  /**
   * Swaps the selected tasks with their siblings, moving closer to the end of the list
   * of siblings. In the UI it looks as if they move downwards.
   */
  val moveDownAction: GPAction = TaskMoveAction("task.move.down", project.taskManager, selectionManager, uiFacade, tableConnector,
    isEnabledPredicate = { selection ->
      selection.find { project.taskManager.taskHierarchy.getNextSibling(it) == null } == null
    },
    onAction = { selection ->
      val taskHierarchy = project.taskManager.taskHierarchy
      selection.asReversed().forEach { task ->
        val parent = taskHierarchy.getContainer(task)
        val index = taskHierarchy.getTaskIndex(task) + 1
        taskHierarchy.move(task, parent, index)
      }
      selection.last()
    }
  )
  val copyAction get() = viewManager().copyAction
  val cutAction get() = viewManager().cutAction
  val pasteAction get() = viewManager().pasteAction
  val linkTasksAction = TaskLinkAction(project.taskManager, selectionManager, uiFacade)
  val unlinkTasksAction = TaskUnlinkAction(project.taskManager, selectionManager, uiFacade)

  val manageColumnsAction: GPAction
    get() = GPAction.create("columns.manage.label") {
      showTaskColumnManager(tableConnector().columnList(), project.taskCustomColumnManager, uiFacade.undoManager, projectDatabase)
    }
  fun all() = listOf(indentAction, unindentAction, moveDownAction, moveUpAction, linkTasksAction, unlinkTasksAction)
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


/**
 * Action for moving tasks in the task tree.
 */
private class TaskMoveAction(
  actionId: String,
  taskManager: TaskManager,
  selectionManager: TaskSelectionManager,
  uiFacade: UIFacade,
  private val myTableConnector: () -> TaskTableActionConnector,
  private val isEnabledPredicate: (List<Task>) -> Boolean,
  private val onAction: (List<Task>) -> Task
) : TaskActionBase(actionId, taskManager, selectionManager, uiFacade) {

  init {
    uiFacade.mainFrame.addWindowListener(object: WindowAdapter() {
      override fun windowOpened(e: WindowEvent?) {
        myTableConnector().isSorted.addListener { _, _, newValue: Boolean ->
          isEnabled = !newValue
        }
      }
    })
    super.disableUndoableEdit()
  }

  override fun isEnabled(selection: List<Task>): Boolean {
    if (selection.isEmpty()) {
      return false
    }
    if (myTableConnector().isSorted.get()) {
      return false
    }
    return isEnabledPredicate(selection)
  }

  override fun run(selection: List<Task>) {
    myTableConnector().commitEdit()

    taskManager.algorithmCollection.scheduler.isEnabled = false
    try {
      myTableConnector().scrollTo(onAction(selection))
    } finally {
      taskManager.algorithmCollection.scheduler.isEnabled = true
    }
  }

  override fun asToolbarAction(): GPAction {
    return this
  }
}

/**
 * The logic of indenting the selection.
 */
fun indent(selection: List<Task>, taskHierarchy: TaskContainmentHierarchyFacade) =
  documentOrdered(retainRoots(selection), taskHierarchy).forEach { task ->
    val newParent = taskHierarchy.getPreviousSibling(task)
    taskHierarchy.move(task, newParent)
  }

/**
 * The logic of unindenting the selection.
 */
fun unindent(selection: List<Task>, taskHierarchy: TaskContainmentHierarchyFacade) =
  documentOrdered(retainRoots(selection), taskHierarchy).asReversed().forEach { task ->
    val parent = taskHierarchy.getContainer(task)
    val ancestor = taskHierarchy.getContainer(parent)
    val index = taskHierarchy.getTaskIndex(parent) + 1
    taskHierarchy.move(task, ancestor, index)
  }

