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
package net.sourceforge.ganttproject.action.edit;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.gui.view.GPViewManager;
import net.sourceforge.ganttproject.undo.GPUndoManager;

//TODO Enable/Disable action depending on clipboard contents
public class PasteAction extends GPAction {
  private final GPViewManager myViewmanager;
  private final GPUndoManager myUndoManager;

  public PasteAction(GPViewManager viewManager, GPUndoManager undoManager) {
    super("paste");
    myViewmanager = viewManager;
    myUndoManager = undoManager;
  }

  private PasteAction(GPViewManager viewmanager, IconSize size, GPUndoManager undoManager) {
    super("paste", size);
    myViewmanager = viewmanager;
    myUndoManager = undoManager;
  }

  @Override
  public GPAction withIcon(IconSize size) {
    return new PasteAction(myViewmanager, size, myUndoManager);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    myUndoManager.undoableEdit(getLocalizedName(), new Runnable() {
      @Override
      public void run() {
        ChartSelection selection = myViewmanager.getSelectedArtefacts();
        myViewmanager.getActiveChart().paste(selection);
        selection.commitClipboardTransaction();
      }
    });
  }
}
