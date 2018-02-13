/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.project;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;

import java.awt.event.ActionEvent;

class SaveURLAction extends CloudProjectActionBase {

  private ProjectUIFacade myProjectUiFacade;
  private IGanttProject myProject;

  SaveURLAction(IGanttProject project, UIFacade uiFacade, ProjectUIFacade projectUiFacade) {
    super("project.save.url", uiFacade, project.getDocumentManager());
    myProjectUiFacade = projectUiFacade;
    myProject = project;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (calledFromAppleScreenMenu(e)) {
      return;
    }
    try {
      saveProjectRemotely(myProject);
    } catch (Exception ex) {
      GPLogger.log(ex);
    }
  }

  private void saveProjectRemotely(IGanttProject project) {
    Document document = showURLDialog(project, false);
    if (document != null) {
      project.setDocument(document);
      myProjectUiFacade.saveProject(project);
    }
  }

}
