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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.chart.gantt.ClipboardContents;
import net.sourceforge.ganttproject.chart.gantt.ClipboardTaskProcessor;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

/**
 * TransferHandler implementation which creates and consumes ClipboardContents objects
 *
 * @author dbarashev (Dmitry Barashev)
 */
class GPTreeTransferHandler extends TransferHandler {
  private static DataFlavor ourClipboardContentsFlavor;
  private static DataFlavor[] ourFlavors = new DataFlavor[1];

  static {
    try {
      String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + ClipboardContents.class.getName() + "\"";
      ourClipboardContentsFlavor = new DataFlavor(mimeType);
      ourFlavors[0] = ourClipboardContentsFlavor;
    } catch (ClassNotFoundException e) {
      System.out.println("ClassNotFound: " + e.getMessage());
    }
  }

  private final GPTreeTableBase myTreeTable;
  private final TaskManager myTaskManager;

  public GPTreeTransferHandler(GPTreeTableBase treeTable, TaskManager taskManager) {
    myTreeTable = treeTable;
    myTaskManager = taskManager;
  }

  @Override
  public boolean canImport(TransferHandler.TransferSupport support) {
    if (!support.isDrop()) {
      return false;
    }
    support.setShowDropLocation(true);
    if (!support.isDataFlavorSupported(ourClipboardContentsFlavor)) {
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
    System.err.println("Create transferrable: selection=" + Arrays.asList(selectedPaths));
    if (selectedPaths == null || selectedPaths.length == 0) {
      return null;
    }
    ClipboardContents clipboardContents = new ClipboardContents(myTaskManager);
    for (TreePath path : selectedPaths) {
      TreeTableNode node = (TreeTableNode) path.getLastPathComponent();
      Task task = (Task) node.getUserObject();
      if (task != null) {
        clipboardContents.addTasks(Collections.singletonList(task));
      }
    }
    System.err.println("Created transferrable");
    return new NodesTransferable(clipboardContents);
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
      ClipboardContents clipboard = (ClipboardContents) t.getTransferData(ourClipboardContentsFlavor);
      JXTreeTable.DropLocation dl = (JXTreeTable.DropLocation) support.getDropLocation();
      int dropRow = myTreeTable.rowAtPoint(dl.getDropPoint());
      TreePath dropPath = myTreeTable.getPathForRow(dropRow);
      DefaultMutableTreeTableNode dropNode = (DefaultMutableTreeTableNode) dropPath.getLastPathComponent();
      Task dropTask = (Task) dropNode.getUserObject();
      clipboard.cut();
      ClipboardTaskProcessor processor = new ClipboardTaskProcessor(myTaskManager);
      processor.pasteAsChild(dropTask, clipboard);
      return true;
    } catch (UnsupportedFlavorException | IOException e) {
      GPLogger.logToLogger(e);
      return false;
    } catch (RuntimeException e) {
      e.printStackTrace();
      return false;
    }
  }

  private static class NodesTransferable implements Transferable {
    private final ClipboardContents myClipboardContents;

    public NodesTransferable(ClipboardContents contents) {
      myClipboardContents = contents;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
      if (!isDataFlavorSupported(flavor))
        throw new UnsupportedFlavorException(flavor);
      return myClipboardContents;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return ourFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return ourClipboardContentsFlavor.equals(flavor);
    }
  }
}