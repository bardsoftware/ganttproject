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

import java.util.Arrays;
import java.util.List;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttTreeTable;
import net.sourceforge.ganttproject.GanttTreeTableModel;
import net.sourceforge.ganttproject.TreeTableContainer;
import net.sourceforge.ganttproject.AbstractChartImplementation.ChartSelectionImpl;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.algorithm.RetainRootsAlgorithm;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

/**
 * Implementation of ChartSelection on Gantt chart.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class GanttChartSelection extends ChartSelectionImpl {
  private static final Function<DefaultMutableTreeTableNode, DefaultMutableTreeTableNode> getParentNode = new Function<DefaultMutableTreeTableNode, DefaultMutableTreeTableNode>() {
    @Override
    public DefaultMutableTreeTableNode apply(DefaultMutableTreeTableNode node) {
      return (DefaultMutableTreeTableNode) node.getParent();
    }
  };


  private final RetainRootsAlgorithm<DefaultMutableTreeTableNode> myRetainRootsAlgorithm = new RetainRootsAlgorithm<DefaultMutableTreeTableNode>();
  private final TreeTableContainer<Task, GanttTreeTable, GanttTreeTableModel> myTree;
  private final TaskManager myTaskManager;

  private ClipboardContents myClipboardContents;


  private Function<? super DefaultMutableTreeTableNode, ? extends Task> getTaskFromNode = new Function<DefaultMutableTreeTableNode, Task>() {
    @Override
    public Task apply(DefaultMutableTreeTableNode node) {
      return (Task) node.getUserObject();
    }
  };

  GanttChartSelection(TreeTableContainer<Task, GanttTreeTable, GanttTreeTableModel> treeView, TaskManager taskManager) {
    myTree = treeView;
    myTaskManager = taskManager;
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
  }

  @Override
  public void startMoveClipboardTransaction() {
    super.startMoveClipboardTransaction();
    myClipboardContents = buildClipboardContents();
    myClipboardContents.cut();
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
    ClipboardTaskProcessor processor = new ClipboardTaskProcessor(myTaskManager);
    return processor.pasteAsSibling(target, myClipboardContents);
  }
}