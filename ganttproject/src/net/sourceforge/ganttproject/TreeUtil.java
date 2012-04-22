package net.sourceforge.ganttproject;

import java.util.List;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import com.google.common.collect.Lists;

public class TreeUtil {
  static int getPrevSibling(TreeNode node, TreeNode child) {
    int childIndex = node.getIndex(child);
    return childIndex - 1;
  }

  static TreeTableNode getPrevSibling(TreeTableNode node) {
    TreeTableNode parent = node.getParent();
    int idxPrev = getPrevSibling(parent, node);
    return idxPrev == -1 ? null : parent.getChildAt(idxPrev);
  }

  static int getNextSibling(TreeNode node, TreeNode child) {
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
