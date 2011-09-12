/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.action.edit;

import javax.swing.Action;
import javax.swing.JMenu;

import net.sourceforge.ganttproject.GPViewManager;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.undo.GPUndoManager;

public class EditMenu extends JMenu {
    private final UndoAction myUndoAction;
    private final RedoAction myRedoAction;

    public EditMenu(IGanttProject project, UIFacade uiFacade, GPViewManager viewManager) {
        super();
        final GPUndoManager undoManager = uiFacade.getUndoManager();
        myUndoAction = new UndoAction(undoManager);
        myRedoAction = new RedoAction(undoManager);

        add(getUndoAction());
        add(getRedoAction());
        addSeparator();
        add(new RefreshViewAction(uiFacade));
        add(new SearchDialogAction(project,uiFacade));
        addSeparator();
        add(viewManager.getCutAction());
        add(viewManager.getCopyAction());
        add(viewManager.getPasteAction());
        addSeparator();
        add(new SettingsDialogAction(project, uiFacade));
    }

    public Action getUndoAction() {
        return myUndoAction;
    }

    public Action getRedoAction() {
        return myRedoAction;
    }
}
