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

import biz.ganttproject.customproperty.CustomPropertyDefinition;
import biz.ganttproject.customproperty.CustomPropertyManager;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.resource.ResourceTableNode;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.TaskManager;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.util.List;
import java.util.NoSuchElementException;

public class ResourceTreeTableModel extends DefaultTreeTableModel {
  private static final int STANDARD_COLUMN_COUNT = ResourceDefaultColumn.values().length;

  private DefaultMutableTreeTableNode root = null;

  private final HumanResourceManager myResourceManager;

  private final CustomPropertyManager myCustomPropertyManager;

  public ResourceTreeTableModel(HumanResourceManager resMgr, TaskManager taskManager,
      CustomPropertyManager customPropertyManager) {
    super();
    myCustomPropertyManager = customPropertyManager;
    myResourceManager = resMgr;
    this.setRoot(root);
  }

  @Override
  public int getHierarchicalColumn() {
    return ResourceDefaultColumn.NAME.ordinal();
  }

  public void updateResources() {
    for (HumanResource hr : myResourceManager.getResources()) {
      ResourceNode rnRes = getNodeForResource(hr);
      if (rnRes == null) {
        rnRes = new ResourceNode(hr);
      }
      buildAssignmentsSubtree(rnRes);
      if (getNodeForResource(hr) == null) {
        root.add(rnRes);
      }
    }
  }

  public void updateResources(List<HumanResource> sorted){
    myResourceManager.clear();
    sorted.forEach(hr -> myResourceManager.add(hr));
    updateResources();
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
//    myResourceManager.clear();
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

  private void buildAssignmentsSubtree(ResourceNode resourceNode) {
    HumanResource resource = resourceNode.getResource();
    resourceNode.removeAllChildren();
    modelSupport.fireTreeStructureChanged(TreeUtil.createPath(resourceNode));
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

  void setSelectionModel(TreeSelectionModel selectionModel) {
  }

}
