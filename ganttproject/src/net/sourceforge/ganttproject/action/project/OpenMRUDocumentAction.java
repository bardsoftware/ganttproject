/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject Team

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

import java.awt.event.ActionEvent;
import java.io.IOException;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;

/**
 * Creates a new action, that stores the specified document and invokes the
 * specified listener.
 */
public class OpenMRUDocumentAction extends GPAction {
  private final String myDocument;
  private final IGanttProject myProject;
  private final UIFacade myUIFacade;
  private final ProjectUIFacade myProjectUIFacade;

  // FIXME Keyboard shortcuts are not working... (because action is created
  // dynamically?)
  public OpenMRUDocumentAction(int index, String document, IGanttProject project, UIFacade uiFacade,
      ProjectUIFacade projectUIFacade) {
    super("project.mru." + index);
    myDocument = document;
    myProject = project;
    myUIFacade = uiFacade;
    myProjectUIFacade = projectUIFacade;

    // Now the muDocument field is set, the correct name can be found, so force
    // updating the action
    updateAction();
  }

  @Override
  protected String getLocalizedName() {
    return myDocument == null ? "" : myProject.getDocumentManager().getDocument(myDocument).getFileName();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (myProjectUIFacade.ensureProjectSaved(myProject)) {
      try {
        Document doc = myProject.getDocumentManager().getDocument(myDocument);
        myProjectUIFacade.openProject(doc, myProject);
      } catch (DocumentException exception) {
        myUIFacade.showErrorDialog(exception);
      } catch (IOException exception) {
        myUIFacade.showErrorDialog(exception);
      }
    }
  }
}
