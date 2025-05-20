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
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.view.GPViewManager
import net.sourceforge.ganttproject.resource.AssignmentContext
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.resource.ResourceSelectionManager
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.util.BrowserControl
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

class ResourceActionSet(
  selectionManager: ResourceSelectionManager, assignmentContext: AssignmentContext,
  project: IGanttProject, private val uiFacade: UIFacade
) {
  val resourceNewAction = ResourceNewAction(project.humanResourceManager, project.projectDatabase, project.roleManager, project.taskManager, uiFacade)
  val cloudResourceList = GPCloudResourceListAction(project.humanResourceManager)
  val resourceDeleteAction = ResourceDeleteAction2(project.humanResourceManager, selectionManager, uiFacade)
  val resourcePropertiesAction = ResourcePropertiesAction(project, selectionManager, assignmentContext, uiFacade)
  val resourceMoveUpAction = ResourceMoveUpAction2(project.humanResourceManager, selectionManager)
  val resourceMoveDownAction = ResourceMoveDownAction2(project.humanResourceManager, selectionManager)
  val assignmentDelete = AssignmentDeleteAction2(selectionManager, uiFacade)
  val copyAction = ResourceCopyAction(project.humanResourceManager, selectionManager, uiFacade.viewManager)
  val pasteAction get() = uiFacade.viewManager.pasteAction
  val cutAction get() = uiFacade.viewManager.cutAction
  val resourceSendMailAction = ResourceSendMailAction2(selectionManager)

  val actions: Array<AbstractAction> by lazy {
    resourceNewAction.putValue(Action.SHORT_DESCRIPTION, null)
    resourcePropertiesAction.putValue(Action.SHORT_DESCRIPTION, null)
    arrayOf(resourceNewAction, resourcePropertiesAction)
  }

  init {
    selectionManager.addResourceListener { _, _ ->
      listOf(resourcePropertiesAction).forEach {
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

/**
 * Moves the selected resources up in the table.
 */
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

/**
 * Moves the selected resources down in the table.
 */
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

/**
 * Places the selected resources into the clipboard.
 */
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

class ResourceDeleteAction2(
  resourceManager: HumanResourceManager,
  private val selectionManager: ResourceSelectionManager,
  private val uiFacade: UIFacade
) : ResourceAction("resource.delete", resourceManager, selectionManager, IconSize.NO_ICON) {

  init {
    selectionManager.addResourceListener(this::onResourceSelectionChange)
    selectionManager.addAssignmentListener(this::onAssignmentSelectionChange)
  }

  override fun actionPerformed(e: ActionEvent?) {
    if (selection.isNotEmpty()) {
      uiFacade.undoManager.undoableEdit(localizedDescription) {
        selection.forEach { it.delete() }
      }
    } else if (selectionManager.resourceAssignments.isNotEmpty()) {
      deleteAssignments(selectionManager, uiFacade, localizedDescription)
    }
  }

  private fun onResourceSelectionChange(selection: List<HumanResource>, trigger: Any) {
    isEnabled = selection.isNotEmpty() || selectionManager.resourceAssignments.isNotEmpty()
  }

  private fun onAssignmentSelectionChange(selection: List<ResourceAssignment>, trigger: Any) {
    isEnabled = selection.isNotEmpty() || selectionManager.resources.isNotEmpty()
  }
}
/**
 * Deletes the selected assignments.
 */
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

/**
 * Opens a desktop-specific email client, capable of handling mailto: urls.
 */
class ResourceSendMailAction2(selectionManager: ResourceSelectionManager)
  : ResourceAction("resource.sendmail", null, selectionManager, IconSize.NO_ICON) {

  init {
    selectionManager.addResourceListener(this::onSelectionChange)
  }

  override fun actionPerformed(e: ActionEvent?) {
    try {
      BrowserControl.displayURL("mailto:${selection[0].mail}")
    } catch (exception: Exception) {
      System.err.println(exception)
    }
  }

  private fun onSelectionChange(selection: List<HumanResource>, trigger: Any) {
    isEnabled = selection.size == 1 && selection[0].mail.isNotBlank()
  }
}
