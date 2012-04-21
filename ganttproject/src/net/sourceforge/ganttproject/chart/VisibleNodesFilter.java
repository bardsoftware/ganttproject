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
import java.util.Collections;
import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class VisibleNodesFilter {
  public List<Task> getVisibleNodes(JTree jtree, int minHeight, int maxHeight, int nodeHeight) {
    List<DefaultMutableTreeNode> preorderedNodes = Collections.list(((DefaultMutableTreeNode) jtree.getModel().getRoot()).preorderEnumeration());
    List<Task> result = new ArrayList<Task>();
    int currentHeight = 0;
    for (int i = 1; i < preorderedNodes.size(); i++) {
      DefaultMutableTreeNode nextNode = preorderedNodes.get(i);
      if (false == nextNode.getUserObject() instanceof Task) {
        continue;
      }
      if ((currentHeight + nodeHeight) > minHeight && jtree.isVisible(new TreePath(nextNode.getPath()))) {
        result.add((Task) nextNode.getUserObject());
      }
      if (jtree.isVisible(new TreePath(nextNode.getPath()))) {
        currentHeight += nodeHeight;
      }
      if (currentHeight > minHeight + maxHeight) {
        break;
      }
    }
    return result;
  }

}
