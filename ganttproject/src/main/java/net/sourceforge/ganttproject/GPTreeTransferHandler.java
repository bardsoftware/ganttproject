/*
Copyright 2014 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.gantt.ClipboardContents;
import net.sourceforge.ganttproject.chart.gantt.ClipboardTaskProcessor;
import net.sourceforge.ganttproject.chart.gantt.GanttChartSelection;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * TransferHandler implementation which creates and consumes ClipboardContents objects
 *
 * @author dbarashev (Dmitry Barashev)
 */
class GPTreeTransferHandler extends TransferHandler {
  private final GPTreeTableBase myTreeTable;
  private final TaskManager myTaskManager;
  private final Supplier<GanttChart> myGanttChart;
  private final GPUndoManager myUndoManager;

  public GPTreeTransferHandler(GPTreeTableBase treeTable, TaskManager taskManager, Supplier<GanttChart> ganttChart, GPUndoManager undoManager) {
    myGanttChart = Preconditions.checkNotNull(ganttChart);
    myTreeTable = Preconditions.checkNotNull(treeTable);
    myTaskManager = Preconditions.checkNotNull(taskManager);
    myUndoManager = undoManager;
  }

  @Override
  public boolean canImport(TransferHandler.TransferSupport support) {
    if (!support.isDrop()) {
      return false;
    }
    support.setShowDropLocation(true);
    if (!support.isDataFlavorSupported(GPTransferable.INTERNAL_DATA_FLAVOR)) {
      return false;
    }
    // Do not allow a drop on the drag source selections.
    JXTreeTable.DropLocation dl = (JXTreeTable.DropLocation) support.getDropLocation();
    int dropRow = myTreeTable.rowAtPoint(dl.getDropPoint());
    int[] selRows = myTreeTable.getSelectedRows();
    for (int i = 0; i < selRows.length; i++) {
      if (selRows[i] == dropRow) {
        return false;
      }
    }
    return true;
  }

  @Override
  protected Transferable createTransferable(JComponent c) {
    TreePath[] selectedPaths = myTreeTable.getTreeSelectionModel().getSelectionPaths();
    if (selectedPaths == null || selectedPaths.length == 0) {
      return null;
    }
    ClipboardContents clipboardContents = ((GanttChartSelection)myGanttChart.get().getSelection()).buildClipboardContents();
    return new GPTransferable(clipboardContents);
  }


  @Override
  public int getSourceActions(JComponent c) {
    return COPY_OR_MOVE;
  }

  @Override
  public boolean importData(TransferHandler.TransferSupport support) {
    if (!canImport(support)) {
      return false;
    }
    try {
      Transferable t = support.getTransferable();
      final ClipboardContents clipboard = (ClipboardContents) t.getTransferData(GPTransferable.INTERNAL_DATA_FLAVOR);
      JXTreeTable.DropLocation dl = (JXTreeTable.DropLocation) support.getDropLocation();
      int dropRow = myTreeTable.rowAtPoint(dl.getDropPoint());
      TreePath dropPath = myTreeTable.getPathForRow(dropRow);
      if (dropPath == null) {
        return false;
      }
      DefaultMutableTreeTableNode dropNode = (DefaultMutableTreeTableNode) dropPath.getLastPathComponent();
      final Task dropTask = (Task) dropNode.getUserObject();
      final ClipboardTaskProcessor processor = new ClipboardTaskProcessor(myTaskManager);
      if (processor.canMove(dropTask, clipboard)) {
        myUndoManager.undoableEdit(GanttLanguage.getInstance().getText("dragndrop.undo.label"), new Runnable() {
          @Override
          public void run() {
            clipboard.cut();
            processor.pasteAsChild(dropTask, clipboard);
          }
        });
        return true;
      }
    } catch (UnsupportedFlavorException | IOException | RuntimeException e) {
      GPLogger.logToLogger(e);
    }
    return false;
  }

}
