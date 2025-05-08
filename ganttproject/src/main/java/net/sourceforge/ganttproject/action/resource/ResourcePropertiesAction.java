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
package net.sourceforge.ganttproject.action.resource;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.GanttDialogPerson;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.resource.AssignmentContext;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceContext;

import java.awt.event.ActionEvent;

public class ResourcePropertiesAction extends ResourceAction {
  private final IGanttProject myProject;
  private final UIFacade myUIFacade;
  private final AssignmentContext assignmentContext;
  private boolean isShowing;
  private GPAction taskPropertiesAction;

  public ResourcePropertiesAction(IGanttProject project, ResourceContext context, AssignmentContext assignmentContext, UIFacade uiFacade) {
    this(project, context, assignmentContext, uiFacade, IconSize.MENU);
  }

  private ResourcePropertiesAction(IGanttProject project, ResourceContext context, AssignmentContext assignmentContext, UIFacade uiFacade, IconSize size) {
    super("resource.properties", null, context, size);
    myProject = project;
    myUIFacade = uiFacade;
    this.assignmentContext = assignmentContext;
    setEnabled(hasResources());

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (isShowing || calledFromAppleScreenMenu(e)) {
      return;
    }
    HumanResource[] selectedResources = getSelection();
    if (selectedResources.length > 0) {
      isShowing = true;
      myUIFacade.getResourceTree().stopEditing();
      // TODO Allow to edit multiple resources (instead of [0])
      GanttDialogPerson dp = new GanttDialogPerson(myProject.getHumanResourceManager(), myProject.getResourceCustomPropertyManager(), myProject.getTaskManager(),
        myProject.getProjectDatabase(), myUIFacade, selectedResources[0], this::onHide);
      dp.setVisible(true);
    } else {
      var assignments = assignmentContext.getResourceAssignments();
      if (!assignments.isEmpty() && taskPropertiesAction != null) {
        taskPropertiesAction.actionPerformed(null);
      }
    }
  }

  private void onHide() {
    isShowing = false;
  }

  @Override
  public ResourcePropertiesAction asToolbarAction() {
    final ResourcePropertiesAction result = new ResourcePropertiesAction(myProject, getContext(), assignmentContext, myUIFacade);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    addPropertyChangeListener(evt -> {
      if ("enabled".equals(evt.getPropertyName())) {
        result.setEnabled(ResourcePropertiesAction.this.isEnabled());
      }
    });
    return result;
  }

  public void setTaskPropertiesAction(GPAction taskPropertiesAction) {
    this.taskPropertiesAction = taskPropertiesAction;
  }
}
