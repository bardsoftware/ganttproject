/*
 * Created on 29.09.2005
 */
package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

class TaskContainmentHierarchyFacadeImpl implements
        TaskContainmentHierarchyFacade {
    private Map myTask2treeNode = new HashMap();
    private Map myTask2index = new LinkedHashMap();
    private Task myRootTask;

    private List myPathBuffer = new ArrayList();

    private GanttTree2 myTree;

    public TaskContainmentHierarchyFacadeImpl(GanttTree2 tree) {
        ArrayList/*<DefaultMutableTreeNode>*/ allTasks = tree.getAllTasks();
        // comboBox.addItem("no set");
        // for (int i = 0; i < allTasks.size(); i++) {
        // DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)
        // allTasks.get(i);
        for (int i=0; i<allTasks.size(); i++) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)allTasks.get(i);
            Task task = (Task) treeNode.getUserObject();
            if (treeNode.isRoot()) {
                myRootTask = task;
            }
            myTask2treeNode.put(task, treeNode);
            myTask2index.put(task, new Integer(i));
        }
        myTree = tree;
    }

    public Task[] getNestedTasks(Task container) {
        Task[] result = null;
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) myTask2treeNode
                .get(container);
        if (treeNode != null) {
            ArrayList list = new ArrayList();
            for (Enumeration children = treeNode.children(); children
                    .hasMoreElements();) {
                DefaultMutableTreeNode next = (DefaultMutableTreeNode) children
                        .nextElement();
                if (next instanceof TaskNode)
                    list.add(next.getUserObject());
            }
            result = (Task[]) list.toArray(new Task[0]);
        }
        return result == null ? new Task[0] : result;
    }

    
    public Task[] getDeepNestedTasks(Task container) {
        ArrayList result = new ArrayList();
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) myTask2treeNode.get(container);
        if (treeNode != null) {
            for (Enumeration subtree = treeNode.preorderEnumeration(); subtree.hasMoreElements();) {
                DefaultMutableTreeNode curNode = (DefaultMutableTreeNode) subtree.nextElement();
                assert curNode.getUserObject() instanceof Task;
                result.add(curNode.getUserObject());
            }
            
            // We remove the first task which is == container 
            assert result.size() > 0;
            result.remove(0);
        }
        return (Task[]) result.toArray(new Task[result.size()]);
    }

    /**
     * Purpose: Returns true if the container Task has any nested tasks.
     * This should be a quicker check than using getNestedTasks().
     * 
     * @param container
     *            The Task on which to check for children.
     */
    public boolean hasNestedTasks(Task container) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) myTask2treeNode
                .get(container);
        if (treeNode != null) {
            if (treeNode.children().hasMoreElements()) {
                return true;
            }
        }
        return false;
    }

    public Task getRootTask() {
        return myRootTask;
    }

    public Task getContainer(Task nestedTask) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) myTask2treeNode
                .get(nestedTask);
        if (treeNode == null) {
            return null;
        }
        DefaultMutableTreeNode containerNode = (DefaultMutableTreeNode) treeNode
                .getParent();
        return containerNode == null ? null : (Task) containerNode
                .getUserObject();
    }

    public boolean areUnrelated(Task first, Task second) {
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

    public void move(Task whatMove, Task whereMove) {
        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) myTask2treeNode
                .get(whereMove);
        DefaultMutableTreeNode movedNode = (DefaultMutableTreeNode) myTask2treeNode
                .get(whatMove);
        if (movedNode != null) {
            TreePath movedPath = new TreePath(movedNode.getPath());
            boolean wasSelected = (myTree.getJTree().getSelectionModel()
                    .isPathSelected(movedPath));
            if (wasSelected) {
                myTree.getJTree().getSelectionModel().removeSelectionPath(movedPath);
            }
            myTree.getModel().removeNodeFromParent(movedNode);
            myTree.getModel().insertNodeInto(movedNode, targetNode,
                    targetNode.getChildCount());
            if (wasSelected) {
                movedPath = new TreePath(movedNode.getPath());
                myTree.getJTree().getSelectionModel().addSelectionPath(movedPath);
            }
        } else {
            myTree.addObjectWithExpand(whatMove, targetNode);
        }
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
    
    public int getDepth(Task task) {
        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) myTask2treeNode
                .get(task);
        return treeNode.getLevel();
    }

    public int compareDocumentOrder(Task task1, Task task2) {
//        DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) myTask2treeNode.get(task1);
//        DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) myTask2treeNode.get(task2);
//        int row1 = myTree.getJTree().getRowForPath(new TreePath(node1.getPath()));
//        int row2 = myTree.getJTree().getRowForPath(new TreePath(node2.getPath()));
    	Integer index1 = (Integer) myTask2index.get(task1);
    	Integer index2 = (Integer) myTask2index.get(task2);
        return index1.intValue() - index2.intValue();
    }

    public boolean contains(Task task) {
        return myTask2treeNode.containsKey(task);
    }
}