/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action.resource

import biz.ganttproject.resource.GPCloudResourceListAction
import net.sourceforge.ganttproject.GanttProject
import net.sourceforge.ganttproject.ResourceTreeTable
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.view.GPViewManager
import net.sourceforge.ganttproject.resource.*
import net.sourceforge.ganttproject.task.ResourceAssignment
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

class ResourceActionSet(
  selectionManager: ResourceSelectionManager, assignmentContext: AssignmentContext,
  project: GanttProject, private val uiFacade: UIFacade, table: ResourceTreeTable
) {
  val resourceNewAction = ResourceNewAction(project.humanResourceManager, project.projectDatabase, project.roleManager, project.taskManager, uiFacade)
  val cloudResourceList = GPCloudResourceListAction(project.humanResourceManager)
  val resourceDeleteAction: ResourceDeleteAction
  val resourcePropertiesAction = ResourcePropertiesAction(project, selectionManager, assignmentContext, uiFacade)
  val resourceMoveUpAction = ResourceMoveUpAction2(project.humanResourceManager, selectionManager)
  val resourceMoveDownAction = ResourceMoveDownAction2(project.humanResourceManager, selectionManager)
  val resourceSendMailAction = ResourceSendMailAction(table)
  val assignmentDelete = AssignmentDeleteAction2(selectionManager, uiFacade)
  val copyAction = ResourceCopyAction(project.humanResourceManager, selectionManager, uiFacade.viewManager)
  val pasteAction get() = uiFacade.viewManager.pasteAction
  val cutAction get() = uiFacade.viewManager.cutAction

  val actions: Array<AbstractAction> by lazy {
    resourceNewAction.putValue(Action.SHORT_DESCRIPTION, null)
    resourcePropertiesAction.putValue(Action.SHORT_DESCRIPTION, null)
    resourceSendMailAction.putValue(Action.SHORT_DESCRIPTION, null)
    arrayOf(resourceNewAction, resourcePropertiesAction)
  }

  init {
    val manager = project.humanResourceManager
    resourceDeleteAction = ResourceDeleteAction(manager, selectionManager, assignmentContext, uiFacade)

    selectionManager.addResourceListener { _, _ ->
      listOf(resourcePropertiesAction, resourceDeleteAction).forEach {
        it.updateEnabled()
      }
    }
  }
}

fun deleteAssignments(assignmentContext: AssignmentContext, uiFacade: UIFacade, actionDescription: String) {
  assignmentContext.resourceAssignments?.let { assignments ->
    uiFacade.undoManager.undoableEdit(actionDescription) {
      assignments.forEach { assignment ->
        assignment.delete()
        assignment.task.assignmentCollection.deleteAssignment(assignment.resource)
      }
      uiFacade.refresh()
    }
  }
}

class ResourceMoveUpAction2(
  private val resourceManager: HumanResourceManager,
  private val selectionManager: ResourceSelectionManager)
  : ResourceAction("resource.move.up", resourceManager, selectionManager, IconSize.NO_ICON) {

    init {
        selectionManager.addResourceListener(this::onSelectionChange)
    }
  override fun actionPerformed(e: ActionEvent?) {
    resourceManager.resourceHierarchyView.moveUp(selectionManager.resources)
  }

  private fun onSelectionChange(selection: List<HumanResource>, trigger: Any) {
    isEnabled = resourceManager.resourceHierarchyView.canMoveUp(selection)
  }
}

class ResourceMoveDownAction2(
  private val resourceManager: HumanResourceManager,
  private val selectionManager: ResourceSelectionManager)
  : ResourceAction("resource.move.down", resourceManager, selectionManager, IconSize.NO_ICON) {

  init {
    selectionManager.addResourceListener(this::onSelectionChange)
  }

  override fun actionPerformed(e: ActionEvent?) {
    resourceManager.resourceHierarchyView.moveDown(selectionManager.resources)
  }
  private fun onSelectionChange(selection: List<HumanResource>, trigger: Any) {
    isEnabled = resourceManager.resourceHierarchyView.canMoveDown(selection)
  }
}

class ResourceCopyAction(
  resourceManager: HumanResourceManager,
  selectionManager: ResourceSelectionManager,
  private val viewManager: GPViewManager
)
  : ResourceAction("copy", resourceManager, selectionManager, IconSize.NO_ICON) {

  init {
    selectionManager.addResourceListener(this::onSelectionChange)
  }

  override fun actionPerformed(e: ActionEvent?) {
    viewManager.selectedArtefacts.startCopyClipboardTransaction()
  }

  private fun onSelectionChange(selection: List<HumanResource>, trigger: Any) {
    isEnabled = selection.isNotEmpty()
  }
}

class AssignmentDeleteAction2(
  private val selectionManager: ResourceSelectionManager,
  private val uiFacade: UIFacade)
  : ResourceAction("assignment.delete", null, null, IconSize.NO_ICON) {
  init {
    selectionManager.addAssignmentListener(this::onSelectionChange)
  }

  override fun actionPerformed(e: ActionEvent?) {
    deleteAssignments(selectionManager, uiFacade, "assignment.delete")
  }

  private fun onSelectionChange(selection: List<ResourceAssignment>, trigger: Any) {
    isEnabled = selection.isNotEmpty()
  }
}