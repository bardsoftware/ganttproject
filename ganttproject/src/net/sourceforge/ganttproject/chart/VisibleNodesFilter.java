/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.chart;

import java.util.ArrayList;
import java.util.List;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;

import net.sourceforge.ganttproject.TreeUtil;
import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class VisibleNodesFilter {
  public List<Task> getVisibleNodes(JXTreeTable jtree, int minHeight, int maxHeight, int nodeHeight) {
    List<MutableTreeTableNode> preorderedNodes = TreeUtil.collectSubtree((MutableTreeTableNode) jtree.getTreeTableModel().getRoot());
    List<Task> result = new ArrayList<Task>();
    int currentHeight = 0;
    for (int i = 1; i < preorderedNodes.size(); i++) {
      MutableTreeTableNode nextNode = preorderedNodes.get(i);
      if (false == nextNode.getUserObject() instanceof Task) {
        continue;
      }
      if ((currentHeight + nodeHeight) > minHeight && jtree.isVisible(TreeUtil.createPath(nextNode))) {
        result.add((Task) nextNode.getUserObject());
      }
      if (jtree.isVisible(TreeUtil.createPath(nextNode))) {
        currentHeight += nodeHeight;
      }
      if (currentHeight > minHeight + maxHeight) {
        break;
      }
    }
    return result;
  }

}
