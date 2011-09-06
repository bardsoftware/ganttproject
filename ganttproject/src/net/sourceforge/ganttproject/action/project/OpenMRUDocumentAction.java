/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.Document.DocumentException;

/**
 * Creates a new action, that stores the specified document and invokes the
 * specified listener.
 */
public class OpenMRUDocumentAction extends GPAction {
    private final Document myDocument;
    private final GanttProject myProject;

    // FIXME Keyboard shortcuts are not working... (because action is created dynamically?)
    public OpenMRUDocumentAction(int index, Document document, GanttProject project) {
        super("project.mru." + index);
        myDocument = document;
        myProject = project;
    }

    @Override
    protected String getLocalizedName() {
        return myDocument == null ? "" : myDocument.getFileName();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (myProject.getProjectUIFacade().ensureProjectSaved(myProject)) {
            try {
                myProject.open(myDocument);
            } catch (DocumentException exception) {
                myProject.getUIFacade().showErrorDialog(exception);
            } catch (IOException exception) {
                myProject.getUIFacade().showErrorDialog(exception);
            }
        }
    }
}
