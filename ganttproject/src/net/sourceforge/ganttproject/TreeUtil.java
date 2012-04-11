package net.sourceforge.ganttproject;

import java.util.List;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import com.google.common.collect.Lists;

class TreeUtil {
  static int getPrevSibling(TreeNode node, TreeNode child) {
    int childIndex = node.getIndex(child);
    return childIndex - 1;
  }

  static TreeNode getPrevSibling(TreeNode node) {
    TreeNode parent = node.getParent();
    int idxPrev = getPrevSibling(parent, node);
    return idxPrev == -1 ? null : parent.getChildAt(idxPrev);
  }

  static int getNextSibling(TreeNode node, TreeNode child) {
    int childIndex = node.getIndex(child);
    return childIndex == node.getChildCount() - 1 ? -1 : childIndex + 1;
  }

  static TreeNode getNextSibling(TreeNode node) {
    TreeNode parent = node.getParent();
    int idxNext = getNextSibling(parent, node);
    return idxNext == -1 ? null : parent.getChildAt(idxNext);
  }

  static TreePath createPath(TreeTableNode node) {
    List<TreeNode> ascendingPath = Lists.newArrayList();
    while (node != null) {
      ascendingPath.add(node);
      node = node.getParent();
    }
    TreeNode[] descendingPath = Lists.reverse(ascendingPath).toArray(new TreeNode[ascendingPath.size()]);
    return new TreePath(descendingPath);
  }

  static List<MutableTreeTableNode> collectSubtree(MutableTreeTableNode root) {
    final List<MutableTreeTableNode> result = Lists.newArrayList();
    collectSubtree(root, result);
    return result;
  }

  static void collectSubtree(MutableTreeTableNode root, List<MutableTreeTableNode> result) {
    result.add(root);
    for (int i = 0; i < root.getChildCount(); i++) {
      collectSubtree((MutableTreeTableNode)root.getChildAt(i), result);
    }
  }


}
