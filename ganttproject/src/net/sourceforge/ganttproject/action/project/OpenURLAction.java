/*
GanttProject is an opensource project management tool. License: GPL3
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
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;

import java.awt.event.ActionEvent;
import java.io.IOException;

public class OpenURLAction extends CloudProjectActionBase {
  private final ProjectUIFacade myProjectUiFacade;
  private final IGanttProject myProject;

  OpenURLAction(IGanttProject project, UIFacade uiFacade, ProjectUIFacade projectUiFacade) {
    super("project.open.url", uiFacade, project.getDocumentManager());
    myProject = project;
    myProjectUiFacade = projectUiFacade;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (calledFromAppleScreenMenu(e)) {
      return;
    }
    if (myProjectUiFacade.ensureProjectSaved(myProject)) {
      try {
        openRemoteProject(myProject);
      } catch (IOException e1) {
        GPLogger.log(e1);
      } catch (DocumentException e1) {
        GPLogger.log(e1);
      }
    }
  }

  public void openRemoteProject(final IGanttProject project) throws IOException, DocumentException {
    final Document document = showURLDialog(project, true);
    if (document != null) {
      myProjectUiFacade.openProject(document, project);
    }
  }
}
