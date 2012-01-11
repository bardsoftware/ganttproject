/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2005-2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.action.edit;

import java.awt.event.ActionEvent;

import javax.swing.event.UndoableEditEvent;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.undo.GPUndoListener;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * @author bard
 */
public class RedoAction extends GPAction implements GPUndoListener {
    private final GPUndoManager myUndoManager;

    public RedoAction(GPUndoManager undoManager) {
        this(undoManager, IconSize.MENU);
    }

    private RedoAction(GPUndoManager undoManager, IconSize size) {
        super("redo", size);
        myUndoManager = undoManager;
        myUndoManager.addUndoableEditListener(this);
        setEnabled(myUndoManager.canRedo());
    }

    @Override
    public GPAction withIcon(IconSize size) {
        return new RedoAction(myUndoManager, size);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        myUndoManager.redo();
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        setEnabled(myUndoManager.canRedo());
        updateAction();
    }

    @Override
    public void undoOrRedoHappened() {
        setEnabled(myUndoManager.canRedo());
        updateAction();
    }

    @Override
    protected String getLocalizedName() {
        if(myUndoManager == null || myUndoManager.canRedo() == false) {
            return super.getLocalizedName();
        }
        // use name of reundoable action
        return myUndoManager.getRedoPresentationName();
    }

    @Override
    protected String getIconFilePrefix() {
        return "redo_";
    }
}
