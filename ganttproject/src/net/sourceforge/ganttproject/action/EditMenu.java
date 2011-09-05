/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JMenu;

import net.sourceforge.ganttproject.GPViewManager;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.undo.GPUndoManager;

public class EditMenu {
    private final IGanttProject myProject;
    private final UIFacade myUiFacade;
    private final GPViewManager myViewManager;
    private final UndoAction myUndoAction;
    private final RedoAction myRedoAction;

    public EditMenu(IGanttProject project, UIFacade uiFacade, GPViewManager viewManager) {
        myProject = project;
        myUiFacade = uiFacade;
        myViewManager = viewManager;
        myUndoAction = new UndoAction(getUndoManager(), getUIFacade());
        myRedoAction = new RedoAction(getUndoManager(), getUIFacade());
    }

    public JMenu create() {
        JMenu result = new JMenu(new GPAction("edit") {
            @Override
            public void actionPerformed(ActionEvent arg0) {
            }
        });
        result.add(getUndoAction());
        result.add(getRedoAction());
        result.addSeparator();
        result.add(new RefreshViewAction(getUIFacade()));
        result.add(new SearchDialogAction(getProject(), getUIFacade()));
        result.addSeparator();
        result.add(getViewManager().getCutAction());
        result.add(getViewManager().getCopyAction());
        result.add(getViewManager().getPasteAction());
        result.addSeparator();
        result.add(new SettingsDialogAction(getProject(), getUIFacade()));

        return result;
    }

    private UIFacade getUIFacade() {
        return myUiFacade;
    }

    private IGanttProject getProject() {
        return myProject;
    }

    private GPUndoManager getUndoManager() {
        return myUiFacade.getUndoManager();
    }

    private GPViewManager getViewManager() {
        return myViewManager;
    }

    public Action getUndoAction() {
        return myUndoAction;
    }

    public Action getRedoAction() {
        return myRedoAction;
    }
}
