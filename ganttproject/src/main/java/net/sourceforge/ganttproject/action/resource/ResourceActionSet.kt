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
import net.sourceforge.ganttproject.resource.AssignmentContext
import net.sourceforge.ganttproject.GanttProject
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.ResourceTreeTable
import net.sourceforge.ganttproject.action.resource.ResourceNewAction
import net.sourceforge.ganttproject.action.resource.ResourceDeleteAction
import net.sourceforge.ganttproject.action.resource.ResourcePropertiesAction
import net.sourceforge.ganttproject.action.resource.ResourceMoveUpAction
import net.sourceforge.ganttproject.action.resource.ResourceMoveDownAction
import net.sourceforge.ganttproject.action.resource.ResourceSendMailAction
import net.sourceforge.ganttproject.action.resource.AssignmentDeleteAction
import javax.swing.AbstractAction
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.resource.ResourceContext
import javax.swing.Action

class ResourceActionSet(
  resourceContext: ResourceContext, assignmentContext: AssignmentContext,
  projectFrame: GanttProject, uiFacade: UIFacade, table: ResourceTreeTable
) {
  val resourceNewAction = ResourceNewAction(projectFrame.humanResourceManager, projectFrame.roleManager, projectFrame.taskManager, uiFacade)
  val cloudResourceList = GPCloudResourceListAction(projectFrame.humanResourceManager)
  val resourceDeleteAction: ResourceDeleteAction
  val resourcePropertiesAction = ResourcePropertiesAction(projectFrame, resourceContext, uiFacade)
  val resourceMoveUpAction: ResourceMoveUpAction
  val resourceMoveDownAction: ResourceMoveDownAction
  val resourceSendMailAction = ResourceSendMailAction(table)
  val assignmentDelete: AssignmentDeleteAction
  val actions: Array<AbstractAction> by lazy {
    resourceNewAction.putValue(Action.SHORT_DESCRIPTION, null)
    resourcePropertiesAction.putValue(Action.SHORT_DESCRIPTION, null)
    resourceSendMailAction.putValue(Action.SHORT_DESCRIPTION, null)
    arrayOf(resourceNewAction, resourcePropertiesAction)
  }

  init {
    val manager = projectFrame.humanResourceManager
    resourceDeleteAction = ResourceDeleteAction(manager, resourceContext, projectFrame, uiFacade)
    resourceMoveUpAction = ResourceMoveUpAction(table)
    resourceMoveDownAction = ResourceMoveDownAction(table)
    assignmentDelete = AssignmentDeleteAction(assignmentContext, uiFacade)
  }
}
