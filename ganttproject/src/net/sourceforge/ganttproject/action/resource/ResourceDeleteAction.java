/*
GanttProject is an opensource project management tool.
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

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceContext;

import java.awt.event.ActionEvent;

/**
 * Action for deleting resources
 */
public class ResourceDeleteAction extends ResourceAction {
  private final UIFacade myUIFacade;

  private final GanttProject myProject;

  public ResourceDeleteAction(HumanResourceManager hrManager, ResourceContext context, GanttProject project,
                              UIFacade uiFacade) {
    this(hrManager, context, project, uiFacade, IconSize.TOOLBAR_SMALL);
  }

  private ResourceDeleteAction(HumanResourceManager hrManager, ResourceContext context, GanttProject project,
                               UIFacade uiFacade, IconSize size) {
    super("resource.delete", hrManager, context, size);
    myUIFacade = uiFacade;
    myProject = project;
    setEnabled(hasResources());
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    final HumanResource[] selectedResources = getSelection();
    if (selectedResources.length > 0) {
      myUIFacade.getUndoManager().undoableEdit(getLocalizedDescription(), new Runnable() {
        @Override
        public void run() {
          deleteResources(selectedResources);
          myUIFacade.refresh();
        }
      });
    }
  }

  private void deleteResources(HumanResource[] resources) {
    for (HumanResource resource : resources) {
      resource.delete();
    }
  }

  @Override
  public ResourceDeleteAction asToolbarAction() {
    ResourceDeleteAction result = new ResourceDeleteAction(getManager(), getContext(), myProject, myUIFacade);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    return result;
  }
}
