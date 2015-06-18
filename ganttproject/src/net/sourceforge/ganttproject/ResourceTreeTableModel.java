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
package net.sourceforge.ganttproject;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.resource.ResourceTableNode;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ResourceTreeTableModel extends DefaultTreeTableModel {
  private static final int STANDARD_COLUMN_COUNT = ResourceDefaultColumn.values().length;
  /** Column indexer */
  private static int index = -1;

  private DefaultMutableTreeTableNode root = null;

  private final HumanResourceManager myResourceManager;

  private final TaskManager myTaskManager;

  private TreeSelectionModel mySelectionModel;

  private final CustomPropertyManager myCustomPropertyManager;

  public ResourceTreeTableModel(HumanResourceManager resMgr, TaskManager taskManager,
      CustomPropertyManager customPropertyManager) {
    super();
    myCustomPropertyManager = customPropertyManager;
    myResourceManager = resMgr;
    myTaskManager = taskManager;
    myTaskManager.addTaskListener(new TaskListenerAdapter() {
      @Override
      public void taskScheduleChanged(TaskScheduleEvent e) {
        Set<HumanResource> affected = Sets.newHashSet();
        List<Task> subtree = Lists.newArrayList(myTaskManager.getTaskHierarchy().getDeepNestedTasks(e.getTask()));
        subtree.add(e.getTask());
        for (Task t : subtree) {
          for (ResourceAssignment ra : t.getAssignments()) {
            affected.add(ra.getResource());
          }
        }
        for (HumanResource resource : affected) {
          resource.resetLoads();
        }
        resourceAssignmentsChanged(affected);
      }
    });
    root = buildTree();
    this.setRoot(root);
  }

  public int useNextIndex() {
    index++;
    return index;
  }

  public MutableTreeTableNode getNodeForAssigment(ResourceAssignment assignement) {
    for (MutableTreeTableNode an : ImmutableList.copyOf(Iterators.forEnumeration(getNodeForResource(
        assignement.getResource()).children()))) {
      if (assignement.equals(an.getUserObject())) {
        return an;
      }
    }
    return null;
  }

  private DefaultMutableTreeTableNode buildTree() {

    DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode();
    List<HumanResource> listResources = myResourceManager.getResources();
    Iterator<HumanResource> itRes = listResources.iterator();

    while (itRes.hasNext()) {
      HumanResource hr = itRes.next();
      ResourceNode rnRes = new ResourceNode(hr); // the first for the resource
      root.add(rnRes);
    }
    return root;
  }

  public void updateResources() {
    HumanResource[] listResources = myResourceManager.getResourcesArray();

    for (int idxResource = 0; idxResource < listResources.length; idxResource++) {
      HumanResource hr = listResources[idxResource];

      ResourceNode rnRes = getNodeForResource(hr);
      if (rnRes == null) {
        rnRes = new ResourceNode(hr);
      }
      buildAssignmentsSubtree(rnRes);
      // for (int i = 0; i < tra.length; i++) {
      // AssignmentNode an = exists(rnRes, tra[i]);
      // if (an == null) {
      // an = new AssignmentNode(tra[i]);
      // rnRes.add(an);
      // }
      // }
      if (getNodeForResource(hr) == null) {
        root.add(rnRes);
      }
    }
    // this.setRoot(root);

  }

  public ResourceNode getNodeForResource(final HumanResource hr) {
    try {
      return (ResourceNode) Iterators.find(Iterators.forEnumeration(root.children()),
          new Predicate<MutableTreeTableNode>() {
            @Override
            public boolean apply(MutableTreeTableNode input) {
              return input.getUserObject().equals(hr);
            }
          });
    } catch (NoSuchElementException e) {
      return null;
    }
  }

  public void changePeople(List<HumanResource> people) {
    Iterator<HumanResource> it = people.iterator();
    while (it.hasNext()) {
      addResource(it.next());
    }
  }

  public DefaultMutableTreeTableNode addResource(HumanResource people) {
    DefaultMutableTreeTableNode result = new ResourceNode(people);
    insertNodeInto(result, root, root.getChildCount());
    myResourceManager.toString();
    return result;
  }

  public void deleteResources(HumanResource[] peoples) {
    for (int i = 0; i < peoples.length; i++) {
      deleteResource(peoples[i]);
    }
  }

  public void deleteResource(HumanResource people) {
    removeNodeFromParent(getNodeForResource(people));
    // myResourceManager.remove(people);
  }

  /** Move Up the selected resource */
  public boolean moveUp(HumanResource resource) {
    myResourceManager.up(resource);
    ResourceNode rn = getNodeForResource(resource);
    int index = TreeUtil.getPrevSibling(root, rn);
    if (index == -1) {
      return false;
    }
    removeNodeFromParent(rn);
    insertNodeInto(rn, root, index);
    return true;
  }

  public boolean moveDown(HumanResource resource) {
    myResourceManager.down(resource);
    ResourceNode rn = getNodeForResource(resource);
    int index = TreeUtil.getNextSibling(root, rn);
    if (index == -1) {
      return false;
    }
    removeNodeFromParent(rn);
    insertNodeInto(rn, root, index);
    return true;
  }

  public void reset() {
    myResourceManager.clear();
  }

  public List<HumanResource> getAllResouces() {
    return myResourceManager.getResources();
  }

  @Override
  public int getColumnCount() {
    return STANDARD_COLUMN_COUNT + myCustomPropertyManager.getDefinitions().size();
  }

  // public ArrayList<ResourceColumn> getColumns()
  // {
  // return new ArrayList<ResourceColumn>(columns.values());
  // }
  //
  // /** @return the ResourceColumn associated to the given index */
  // public ResourceColumn getColumn(int index) {
  // return columns.get(new Integer(index));
  // }

  private CustomPropertyDefinition getCustomProperty(int columnIndex) {
    return myCustomPropertyManager.getDefinitions().get(columnIndex - STANDARD_COLUMN_COUNT);
  }

  @Override
  public Class<?> getColumnClass(int column) {
    if (column < 0) {
      return null;
    }
    if (column >= 0 && column < STANDARD_COLUMN_COUNT) {
      return ResourceDefaultColumn.values()[column].getValueClass();
    }
    CustomPropertyDefinition customColumn = getCustomProperty(column);
    Class<?> result = customColumn == null ? String.class : customColumn.getType();
    return result;
  }

  @Override
  public String getColumnName(int column) {
    if (column < STANDARD_COLUMN_COUNT) {
      return ResourceDefaultColumn.values()[column].getName();
    }
    CustomPropertyDefinition customColumn = getCustomProperty(column);
    return customColumn.getName();
  }

  @Override
  public boolean isCellEditable(Object node, int column) {
    if (false == node instanceof ResourceTableNode) {
      return false;
    }
    if (column >= STANDARD_COLUMN_COUNT) {
      return true;
    }
    ResourceDefaultColumn standardColumn = ResourceDefaultColumn.values()[column];
    ResourceTableNode resourceNode = (ResourceTableNode) node;
    return resourceNode.isEditable(standardColumn);
  }

  @Override
  public Object getValueAt(Object obj, int column) {
    if (false == obj instanceof ResourceTableNode) {
      return "";
    }
    ResourceTableNode node = (ResourceTableNode)obj;
    if (column >= STANDARD_COLUMN_COUNT) {
      return node.getCustomField(getCustomProperty(column));
    }
    return node.getStandardField(ResourceDefaultColumn.values()[column]);
  }

  @Override
  public void setValueAt(Object value, Object obj, int column) {
    if (false == obj instanceof ResourceTableNode) {
      return;
    }
    ResourceTableNode node = (ResourceTableNode)obj;
    if (column >= STANDARD_COLUMN_COUNT) {
      node.setCustomField(getCustomProperty(column), value);
      return;
    }
    if (isCellEditable(node, column)) {
      node.setStandardField(ResourceDefaultColumn.values()[column], value);
    }
  }

  public void resourceChanged(HumanResource resource) {
    ResourceNode node = getNodeForResource(resource);
    if (node == null) {
      return;
    }
    modelSupport.firePathChanged(TreeUtil.createPath(node));
  }

  public void resourceAssignmentsChanged(Iterable<HumanResource> resources) {
    for (HumanResource resource : resources) {
      ResourceNode nextNode = getNodeForResource(resource);
      SelectionKeeper selectionKeeper = new SelectionKeeper(mySelectionModel, nextNode);
      buildAssignmentsSubtree(nextNode);
      selectionKeeper.restoreSelection();
    }
  }

  private void buildAssignmentsSubtree(ResourceNode resourceNode) {
    HumanResource resource = resourceNode.getResource();
    resourceNode.removeAllChildren();
    ResourceAssignment[] assignments = resource.getAssignments();
    int[] indices = new int[assignments.length];
    TreeNode[] children = new TreeNode[assignments.length];
    if (assignments.length > 0) {
      for (int i = 0; i < assignments.length; i++) {
        indices[i] = i;
        AssignmentNode an = new AssignmentNode(assignments[i]);
        children[i] = an;
        resourceNode.add(an);
      }
    }
    modelSupport.fireTreeStructureChanged(TreeUtil.createPath(resourceNode));
  }

  void decreaseCustomPropertyIndex(int i) {
    index -= i;
  }

  void setSelectionModel(TreeSelectionModel selectionModel) {
    mySelectionModel = selectionModel;
  }

  private class SelectionKeeper {
    private final DefaultMutableTreeTableNode myChangingSubtreeRoot;
    private final TreeSelectionModel mySelectionModel;
    private boolean hasWork = false;
    private Object myModelObject;

    SelectionKeeper(TreeSelectionModel selectionModel, DefaultMutableTreeTableNode changingSubtreeRoot) {
      mySelectionModel = selectionModel;
      myChangingSubtreeRoot = changingSubtreeRoot;
      TreePath selectionPath = mySelectionModel.getSelectionPath();
      if (selectionPath != null && TreeUtil.createPath(myChangingSubtreeRoot).isDescendant(selectionPath)) {
        hasWork = true;
        DefaultMutableTreeTableNode lastNode = (DefaultMutableTreeTableNode) selectionPath.getLastPathComponent();
        myModelObject = lastNode.getUserObject();
      }
    }

    void restoreSelection() {
      if (!hasWork) {
        return;
      }
      for (MutableTreeTableNode node : TreeUtil.collectSubtree(myChangingSubtreeRoot)) {
        if (node.getUserObject().equals(myModelObject)) {
          mySelectionModel.setSelectionPath(TreeUtil.createPath(node));
          break;
        }
      }
    }
  }
}
