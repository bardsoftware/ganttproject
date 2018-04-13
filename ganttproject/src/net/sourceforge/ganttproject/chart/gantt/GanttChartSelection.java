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
package net.sourceforge.ganttproject.chart.gantt;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.AbstractChartImplementation.ChartSelectionImpl;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GPTransferable;
import net.sourceforge.ganttproject.GanttTreeTable;
import net.sourceforge.ganttproject.GanttTreeTableModel;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.TreeTableContainer;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.algorithm.RetainRootsAlgorithm;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of ChartSelection on Gantt chart.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GanttChartSelection extends ChartSelectionImpl implements ClipboardOwner {
  private static final Function<DefaultMutableTreeTableNode, DefaultMutableTreeTableNode> getParentNode = new Function<DefaultMutableTreeTableNode, DefaultMutableTreeTableNode>() {
    @Override
    public DefaultMutableTreeTableNode apply(DefaultMutableTreeTableNode node) {
      return (DefaultMutableTreeTableNode) node.getParent();
    }
  };


  private final RetainRootsAlgorithm<DefaultMutableTreeTableNode> myRetainRootsAlgorithm = new RetainRootsAlgorithm<DefaultMutableTreeTableNode>();
  private final TreeTableContainer<Task, GanttTreeTable, GanttTreeTableModel> myTree;
  private final TaskManager myTaskManager;
  private final IGanttProject myProject;

  private ClipboardContents myClipboardContents;


  private Function<? super DefaultMutableTreeTableNode, ? extends Task> getTaskFromNode = new Function<DefaultMutableTreeTableNode, Task>() {
    @Override
    public Task apply(DefaultMutableTreeTableNode node) {
      return (Task) node.getUserObject();
    }
  };

  GanttChartSelection(IGanttProject project, TreeTableContainer<Task, GanttTreeTable, GanttTreeTableModel> treeView, TaskManager taskManager) {
    myTree = treeView;
    myTaskManager = taskManager;
    myProject = project;
  }
  @Override
  public boolean isEmpty() {
    return myTree.getSelectedNodes().length == 0;
  }

  @Override
  public void startCopyClipboardTransaction() {
    super.startCopyClipboardTransaction();
    myClipboardContents = buildClipboardContents();
    myClipboardContents.copy();
    exportTasksIntoSystemClipboard();
  }

  private void exportTasksIntoSystemClipboard() {
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(new GPTransferable(myClipboardContents), this);
  }

  @Override
  public void startMoveClipboardTransaction() {
    super.startMoveClipboardTransaction();
    myClipboardContents = buildClipboardContents();
    myClipboardContents.cut();
    exportTasksIntoSystemClipboard();
  }

  public ClipboardContents buildClipboardContents() {
    DefaultMutableTreeTableNode[] selectedNodes = myTree.getSelectedNodes();
    GPLogger.getLogger("Clipboard").fine(String.format("Selected nodes: %s", Arrays.asList(selectedNodes)));
    List<DefaultMutableTreeTableNode> selectedRoots = Lists.newArrayList();
    myRetainRootsAlgorithm.run(selectedNodes, getParentNode, selectedRoots);
    GPLogger.getLogger("Clipboard").fine(String.format("Roots: %s", selectedRoots));
    ClipboardContents result = new ClipboardContents(myTaskManager);
    result.addTasks(Lists.transform(selectedRoots, getTaskFromNode));
    return result;
  }

  List<Task> paste(Task target) {
    if (myClipboardContents == null) {
      return Collections.emptyList();
    }
    ClipboardTaskProcessor processor = new ClipboardTaskProcessor(myTaskManager);
    processor.setTaskCopyNameOption(myTaskManager.getTaskCopyNamePrefixOption());
    return processor.pasteAsSibling(target, myClipboardContents);
  }

  @Override
  public void lostOwnership(Clipboard clipboard, Transferable contents) {
    // Do nothing
  }
}
