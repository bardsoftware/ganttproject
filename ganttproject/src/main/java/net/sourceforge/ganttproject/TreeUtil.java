/*
Copyright 2013 BarD Software s.r.o
Copyright 2012 GanttProject Team

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

import java.util.List;
import java.util.Queue;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.util.collect.Pair;

import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

/**
 * Utility methods for working with Swing trees.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TreeUtil {
  static int getPrevSibling(TreeNode node, TreeNode child) {
    if (node == null) {
      return -1;
    }
    int childIndex = node.getIndex(child);
    return childIndex - 1;
  }

  static TreeTableNode getPrevSibling(TreeTableNode node) {
    TreeTableNode parent = node.getParent();
    int idxPrev = getPrevSibling(parent, node);
    return idxPrev == -1 ? null : parent.getChildAt(idxPrev);
  }

  static int getNextSibling(TreeNode node, TreeNode child) {
    if (node == null) {
      return -1;
    }
    int childIndex = node.getIndex(child);
    return childIndex == node.getChildCount() - 1 ? -1 : childIndex + 1;
  }

  static TreeTableNode getNextSibling(TreeTableNode node) {
    TreeTableNode parent = node.getParent();
    int idxNext = getNextSibling(parent, node);
    return idxNext == -1 ? null : parent.getChildAt(idxNext);
  }

  public static TreePath createPath(TreeNode node) {
    List<TreeNode> ascendingPath = Lists.newArrayList();
    while (node != null) {
      ascendingPath.add(node);
      node = node.getParent();
    }
    TreeNode[] descendingPath = Lists.reverse(ascendingPath).toArray(new TreeNode[ascendingPath.size()]);
    return new TreePath(descendingPath);
  }

  public static List<MutableTreeTableNode> breadthFirstSearch(MutableTreeTableNode rootNode) {
    final List<MutableTreeTableNode> result = Lists.newArrayList();
    breadthFirstSearch(rootNode, new Predicate<Pair<MutableTreeTableNode,MutableTreeTableNode>>() {
      public boolean apply(Pair<MutableTreeTableNode, MutableTreeTableNode> parent_child) {
        result.add(parent_child.second());
        return true;
      }
    });
    return result;
  }

  public static void breadthFirstSearch(MutableTreeTableNode root, Predicate<Pair<MutableTreeTableNode, MutableTreeTableNode>> predicate) {
    final Queue<MutableTreeTableNode> queue = Queues.newArrayDeque();
    if (predicate.apply(Pair.create((MutableTreeTableNode) null, root))) {
      queue.add(root);
    }
    while (!queue.isEmpty()) {
      MutableTreeTableNode head = queue.poll();
      for (int i = 0; i < head.getChildCount(); i++) {
        MutableTreeTableNode child = (MutableTreeTableNode) head.getChildAt(i);
        if (predicate.apply(Pair.create(head, child))) {
          queue.add(child);
        }
      }
    }
  }

  public static List<MutableTreeTableNode> collectSubtree(MutableTreeTableNode root) {
    final List<MutableTreeTableNode> result = Lists.newArrayList();
    collectSubtree(root, result);
    return result;
  }

  static void collectSubtree(MutableTreeTableNode root, List<MutableTreeTableNode> result) {
    result.add(root);
    for (int i = 0; i < root.getChildCount(); i++) {
      collectSubtree((MutableTreeTableNode) root.getChildAt(i), result);
    }
  }

  public static void removeAllChildren(MutableTreeTableNode node) {
    List<MutableTreeTableNode> children = Lists.newArrayList();
    for (int i = 0; i < node.getChildCount(); i++) {
      children.add((MutableTreeTableNode) node.getChildAt(i));
    }
    for (MutableTreeTableNode child : children) {
      node.remove(child);
    }
  }

  public static int getLevel(TreeTableNode treeNode) {
    int level = 0;
    while (treeNode != null) {
      treeNode = treeNode.getParent();
      level++;
    }
    return level - 1;
  }
}
