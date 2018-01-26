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
import net.sourceforge.ganttproject.gui.GanttDialogPerson;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceContext;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ResourcePropertiesAction extends ResourceAction {
  private final IGanttProject myProject;
  private final UIFacade myUIFacade;

  public ResourcePropertiesAction(IGanttProject project, ResourceContext context, UIFacade uiFacade) {
    this(project, context, uiFacade, IconSize.MENU);
  }

  private ResourcePropertiesAction(IGanttProject project, ResourceContext context, UIFacade uiFacade, IconSize size) {
    super("resource.properties", null, context, size);
    myProject = project;
    myUIFacade = uiFacade;
    setEnabled(hasResources());
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (calledFromAppleScreenMenu(e)) {
      return;
    }
    HumanResource[] selectedResources = getSelection();
    if (selectedResources.length > 0) {
      myUIFacade.getResourceTree().stopEditing();
      // TODO Allow to edit multiple resources (instead of [0])
      GanttDialogPerson dp = new GanttDialogPerson(myProject.getResourceCustomPropertyManager(), myUIFacade,
          selectedResources[0]);
      dp.setVisible(true);
      if (dp.result()) {
        myProject.setModified(true);
      }
    }
  }

  @Override
  public ResourcePropertiesAction asToolbarAction() {
    final ResourcePropertiesAction result = new ResourcePropertiesAction(myProject, getContext(), myUIFacade);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if ("enabled".equals(evt.getPropertyName())) {
          result.setEnabled(ResourcePropertiesAction.this.isEnabled());
        }
      }
    });
    return result;
  }
}
