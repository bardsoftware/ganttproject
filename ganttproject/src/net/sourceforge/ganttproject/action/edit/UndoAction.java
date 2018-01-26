/*
GanttProject is an opensource project management tool. License: GPL3
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

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.undo.GPUndoListener;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import javax.swing.event.UndoableEditEvent;
import java.awt.event.ActionEvent;

/**
 * @author bard
 */
public class UndoAction extends GPAction implements GPUndoListener {
  private final GPUndoManager myUndoManager;

  public UndoAction(GPUndoManager undoManager) {
    this(undoManager, IconSize.MENU);
  }

  private UndoAction(GPUndoManager undoManager, IconSize size) {
    super("undo", size.asString());
    myUndoManager = undoManager;
    myUndoManager.addUndoableEditListener(this);
    setEnabled(myUndoManager.canUndo());
  }

  @Override
  public GPAction withIcon(IconSize size) {
    return new UndoAction(myUndoManager, size);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (calledFromAppleScreenMenu(e)) {
      return;
    }

    myUndoManager.undo();
  }

  @Override
  public void undoableEditHappened(UndoableEditEvent e) {
    setEnabled(myUndoManager.canUndo());
    updateTooltip();
  }

  @Override
  public void undoOrRedoHappened() {
    setEnabled(myUndoManager.canUndo());
    updateTooltip();
  }

  @Override
  protected String getLocalizedName() {
    if (myUndoManager == null || myUndoManager.canUndo() == false) {
      return super.getLocalizedName();
    }
    // Use name of undoable action
    return myUndoManager.getUndoPresentationName();
  }

  @Override
  protected String getIconFilePrefix() {
    return "undo_";
  }

  @Override
  public UndoAction asToolbarAction() {
    UndoAction result = new UndoAction(myUndoManager);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    return result;
  }
}
