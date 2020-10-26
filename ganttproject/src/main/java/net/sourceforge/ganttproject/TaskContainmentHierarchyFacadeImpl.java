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
package net.sourceforge.ganttproject;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerImpl;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TaskContainmentHierarchyFacadeImpl implements TaskContainmentHierarchyFacade {
  private Map<Task, MutableTreeTableNode> myTask2treeNode = new HashMap<Task, MutableTreeTableNode>();
  private Map<Task, Integer> myTask2index = new LinkedHashMap<Task, Integer>();
  private Task myRootTask;

  private List<Task> myPathBuffer = new ArrayList<Task>();

  private GanttTree2 myTree;

  public TaskContainmentHierarchyFacadeImpl(GanttTree2 tree) {
    List<MutableTreeTableNode> allTasks = tree.getAllTasks();
    for (int i = 0; i < allTasks.size(); i++) {
      MutableTreeTableNode treeNode = allTasks.get(i);
      Task task = (Task) treeNode.getUserObject();
      if (treeNode == tree.getRoot()) {
        myRootTask = task;
      }
      myTask2treeNode.put(task, treeNode);
      myTask2index.put(task, new Integer(i));
    }
    myTree = tree;
  }

  @Override
  public Task[] getNestedTasks(Task container) {
    Task[] result = null;
    MutableTreeTableNode treeNode = myTask2treeNode.get(container);
    if (treeNode != null) {
      ArrayList<Task> list = new ArrayList<Task>();
      for (Enumeration children = treeNode.children(); children.hasMoreElements();) {
        DefaultMutableTreeTableNode next = (DefaultMutableTreeTableNode) children.nextElement();
        if (next instanceof TaskNode) {
          list.add((Task) next.getUserObject());
        }
      }
      result = list.toArray(new Task[0]);
    }
    return result == null ? new Task[0] : result;
  }

  @Override
  public Task[] getDeepNestedTasks(Task container) {
    ArrayList<Task> result = new ArrayList<Task>();
    MutableTreeTableNode treeNodes = myTask2treeNode.get(container);
    if (treeNodes != null) {
      for (MutableTreeTableNode curNode : TreeUtil.collectSubtree(treeNodes)) {
        assert curNode.getUserObject() instanceof Task;
        result.add((Task) curNode.getUserObject());
      }

      // We remove the first task which is == container
      assert result.size() > 0;
      result.remove(0);
    }
    return result.toArray(new Task[result.size()]);
  }

  /**
   * Purpose: Returns true if the container Task has any nested tasks. This
   * should be a quicker check than using getNestedTasks().
   *
   * @param container
   *          The Task on which to check for children.
   */
  @Override
  public boolean hasNestedTasks(Task container) {
    MutableTreeTableNode treeNode = myTask2treeNode.get(container);
    if (treeNode != null) {
      if (treeNode.children().hasMoreElements()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Task getRootTask() {
    return myRootTask;
  }

  @Override
  public Task getContainer(Task nestedTask) {
    MutableTreeTableNode treeNode = myTask2treeNode.get(nestedTask);
    if (treeNode == null) {
      return null;
    }
    MutableTreeTableNode containerNode = (MutableTreeTableNode) treeNode.getParent();
    return containerNode == null ? null : (Task) containerNode.getUserObject();
  }

  @Override
  public void sort(Comparator<Task> comparator) {
    Task[] tasks = getDeepNestedTasks(getRootTask());
    HashMap<Task, Boolean> expanded = new HashMap<>();
    for (Task t : tasks) {
      expanded.put(t, myTree.isExpanded(t));
    }

    sortHelper(getRootTask(), comparator);

    for (Task t : tasks) {
      myTree.setExpanded(t, expanded.get(t));
    }
  }

  private void sortHelper(Task root, Comparator<Task> comparator) {
    Task[] tasks = getNestedTasks(root);
    Arrays.sort(tasks, comparator);

    for (Task t : tasks) {
      myTree.getModel().removeNodeFromParent(myTask2treeNode.get(t));
    }

    for (int i = 0; i < tasks.length; i++) {
      myTree.getModel().insertNodeInto(myTask2treeNode.get(tasks[i]), myTask2treeNode.get(root), i);
      sortHelper(tasks[i], comparator);
    }
  }

  @Override
  public Task getPreviousSibling(Task nestedTask) {
    MutableTreeTableNode treeNode = myTask2treeNode.get(nestedTask);
    assert treeNode != null : "TreeNode of " + nestedTask + " not found. Please inform GanttProject developers";
    TreeTableNode siblingNode = TreeUtil.getPrevSibling(treeNode);
    return siblingNode == null ? null : (Task) siblingNode.getUserObject();
  }

  @Override
  public Task getNextSibling(Task nestedTask) {
    MutableTreeTableNode treeNode = myTask2treeNode.get(nestedTask);
    assert treeNode != null : "TreeNode of " + nestedTask + " not found. Please inform GanttProject developers";
    TreeTableNode siblingNode = TreeUtil.getNextSibling(treeNode);
    return siblingNode == null ? null : (Task) siblingNode.getUserObject();
  }

  @Override
  public int getTaskIndex(Task nestedTask) {
    MutableTreeTableNode treeNode = myTask2treeNode.get(nestedTask);
    assert treeNode != null : "TreeNode of " + nestedTask + " not found. Please inform GanttProject developers";
    TreeNode containerNode = treeNode.getParent();
    return containerNode.getIndex(treeNode);
  }

  @Override
  public List<Integer> getOutlinePath(Task task) {
    int depth = getDepth(task);
    List<Integer> result = Lists.newArrayListWithExpectedSize(depth);
    TreeNode node = myTask2treeNode.get(task);
    for (int i = 0; i < depth; i++) {
      TreeNode containerNode = node.getParent();
      result.add(i, containerNode.getIndex(node) + 1);
      node = containerNode;
    }
    return Lists.reverse(result);
  }

  @Override
  public boolean areUnrelated(Task first, Task second) {
    if (first.equals(second)) {
      return false;
    }
    myPathBuffer.clear();
    for (Task container = getContainer(first); container != null; container = getContainer(container)) {
      myPathBuffer.add(container);
    }
    if (myPathBuffer.contains(second)) {
      return false;
    }
    myPathBuffer.clear();
    for (Task container = getContainer(second); container != null; container = getContainer(container)) {
      myPathBuffer.add(container);
    }
    if (myPathBuffer.contains(first)) {
      return false;
    }
    return true;
  }

  @Override
  public void move(Task whatMove, Task whereMove) {
    MutableTreeTableNode targetNode = myTask2treeNode.get(whereMove);
    if (targetNode == null) {
      GPLogger.log("Failed to find tree node for task=" + whereMove);
      return;
    }
    MutableTreeTableNode currentNode = myTask2treeNode.get(whatMove);
    if (currentNode != null && currentNode.getParent() == targetNode) {
      return;
    }
    move(whatMove, whereMove, targetNode.getChildCount());
  }

  @Override
  public void move(Task whatMove, Task whereMove, int index) {
    MutableTreeTableNode targetNode = myTask2treeNode.get(whereMove);
    MutableTreeTableNode movedNode = myTask2treeNode.get(whatMove);

    if (movedNode == null) {
      movedNode = myTree.addObjectWithExpand(whatMove, targetNode);
    }

    TreePath movedPath = TreeUtil.createPath(movedNode);
    boolean wasSelected = (myTree.getJTree().getTreeSelectionModel().isPathSelected(movedPath));
    if (wasSelected) {
      myTree.getJTree().getTreeSelectionModel().removeSelectionPath(movedPath);
    }
    myTree.getModel().removeNodeFromParent(movedNode);
    myTree.getModel().insertNodeInto(movedNode, targetNode, index);
    if (wasSelected) {
      movedPath = TreeUtil.createPath(movedNode);
      myTree.getJTree().getTreeSelectionModel().addSelectionPath(movedPath);
    }
    ((TaskManagerImpl)getTaskManager()).getDependencyGraph().move(whatMove, whereMove == getTaskManager().getRootTask() ? null : whereMove);
    getTaskManager().getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(whatMove);
    try {
      getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
    } catch (TaskDependencyException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private TaskManager getTaskManager() {
    return myRootTask.getManager();
  }

  @Override
  public int getDepth(Task task) {
    MutableTreeTableNode treeNode = myTask2treeNode.get(task);
    return TreeUtil.getLevel(treeNode);
  }

  @Override
  public int compareDocumentOrder(Task task1, Task task2) {
    Integer index1 = myTask2index.get(task1);
    Integer index2 = myTask2index.get(task2);
    return index1.intValue() - index2.intValue();
  }

  @Override
  public boolean contains(Task task) {
    return myTask2treeNode.containsKey(task);
  }

  private static final Function<MutableTreeTableNode, Task> ourNodeToTaskFxn = new Function<MutableTreeTableNode, Task>() {
    @Override
    public Task apply(MutableTreeTableNode input) {
      return input == null ? null : (Task) input.getUserObject();
    }
  };

  @Override
  public List<Task> getTasksInDocumentOrder() {
    MutableTreeTableNode rootNode = myTask2treeNode.get(getRootTask());
    List<MutableTreeTableNode> subtree = TreeUtil.collectSubtree(rootNode);

    return Lists.transform(subtree.subList(1, subtree.size()), ourNodeToTaskFxn);
  }

  @Override
  public void breadthFirstSearch(Task root, final Predicate<Pair<Task, Task>> predicate) {
    Preconditions.checkNotNull(root);
    MutableTreeTableNode rootNode = myTask2treeNode.get(root);
    TreeUtil.breadthFirstSearch(rootNode, new Predicate<Pair<MutableTreeTableNode,MutableTreeTableNode>>() {
      @Override
      public boolean apply(Pair<MutableTreeTableNode, MutableTreeTableNode> parent_child) {
        Task parentTask = ourNodeToTaskFxn.apply(parent_child.first());
        Task childTask = ourNodeToTaskFxn.apply(parent_child.second());
        return predicate.apply(Pair.create(parentTask, childTask));
      }
    });
  }

  @Override
  public List<Task> breadthFirstSearch(Task root, final boolean includeRoot) {
    final Task _root = (root == null) ? getRootTask() : root;
    final List<Task> result = Lists.newArrayList();
    breadthFirstSearch(_root, new Predicate<Pair<Task,Task>>() {
      @Override
      public boolean apply(Pair<Task, Task> parent_child) {
        if (includeRoot || parent_child.first() != null) {
          result.add(parent_child.second());
        }
        return true;
      }
    });
    return result;
  }
}