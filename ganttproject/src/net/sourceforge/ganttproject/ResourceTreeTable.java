/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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

import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManager.RoleEvent;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;

import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import biz.ganttproject.core.table.ColumnList.Column;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class ResourceTreeTable extends GPTreeTableBase {
  private final RoleManager myRoleManager;

  private final ResourceTreeTableModel myResourceTreeModel;

  private final UIFacade myUiFacade;

  public ResourceTreeTable(IGanttProject project, ResourceTreeTableModel model, UIFacade uiFacade) {
    super(project, uiFacade, project.getResourceCustomPropertyManager(), model);
    myUiFacade = uiFacade;
    myRoleManager = project.getRoleManager();
    myRoleManager.addRoleListener(new RoleManager.Listener() {
      @Override
      public void rolesChanged(RoleEvent e) {
        setEditor(getTableHeaderUiFacade().findColumnByID(ResourceDefaultColumn.ROLE.getStub().getID()));
        setEditor(getTableHeaderUiFacade().findColumnByID(ResourceDefaultColumn.ROLE_IN_TASK.getStub().getID()));
      }

      private void setEditor(ColumnImpl column) {
        if (column == null || column.getTableColumnExt() == null) {
          return;
        }
        JComboBox comboBox = new JComboBox(getRoleManager().getEnabledRoles());
        comboBox.setEditable(false);
        column.getTableColumnExt().setCellEditor(new DefaultCellEditor(comboBox));
      }
    });
    myResourceTreeModel = model;
    getTableHeaderUiFacade().createDefaultColumns(ResourceDefaultColumn.getColumnStubs());
    setTreeTableModel(model);
    myResourceTreeModel.setSelectionModel(getTreeSelectionModel());
  }

  @Override
  public String getToolTipText(MouseEvent event) {
      int column = columnAtPoint(event.getPoint());
      if (column >= 0 && isHierarchical(column)) {
          TreePath pathAtPoint = getTreeTable().getPathForLocation(event.getX(), event.getY());
          TreeTableNode nodeAtPoint = pathAtPoint == null ? null : (TreeTableNode) pathAtPoint.getLastPathComponent();
          if (nodeAtPoint instanceof AssignmentNode) {
            Task task = ((AssignmentNode)nodeAtPoint).getTask();
            return "<html><body>" + buildPath(task) + "</body></html>";
          }
      }
      return super.getToolTipText(event);
  }

  private String buildPath(Task task) {
    List<String> names = Lists.newArrayList();
    TaskContainmentHierarchyFacade hierarchy = task.getManager().getTaskHierarchy();
    for (Task t = task; t != task.getManager().getRootTask(); t = hierarchy.getContainer(t)) {
      names.add(t.getName());
    }
    Collections.reverse(names);
    return Joiner.on(GanttLanguage.getInstance().getText("resourceTable.tooltip.joiner")).join(names);
  }

  public boolean isVisible(DefaultMutableTreeTableNode node) {
    return getTreeTable().isVisible(TreeUtil.createPath(node));
  }

  @Override
  protected List<Column> getDefaultColumns() {
    return ResourceDefaultColumn.getColumnStubs();
  }

  @Override
  protected Chart getChart() {
    return myUiFacade.getResourceChart();
  }

  /** Initialize the treetable. Addition of various listeners, tree's icons. */
  @Override
  protected void doInit() {
    super.doInit();
    myResourceTreeModel.updateResources();
    getVerticalScrollBar().addAdjustmentListener(new VscrollAdjustmentListener(myUiFacade.getResourceChart(), false));
  }

  @Override
  protected void onProjectOpened() {
    super.onProjectOpened();
    myResourceTreeModel.updateResources();
  }

  private RoleManager getRoleManager() {
    return myRoleManager;
  }

  @Override
  protected TableColumnExt newTableColumnExt(int modelIndex) {
    TableColumnExt tableColumn = super.newTableColumnExt(modelIndex);
    if (modelIndex == ResourceDefaultColumn.ROLE.ordinal() || modelIndex == ResourceDefaultColumn.ROLE_IN_TASK.ordinal()) {
      JComboBox comboBox = new JComboBox(getRoleManager().getEnabledRoles());
      comboBox.setEditable(false);
      tableColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }
    return tableColumn;
  }

  /** @return the list of the selected nodes. */
  @Deprecated
  public DefaultMutableTreeTableNode[] getSelectedNodes() {
    TreePath[] currentSelection = getTreeSelectionModel().getSelectionPaths();

    if (currentSelection == null || currentSelection.length == 0) {
      return new DefaultMutableTreeTableNode[0];
    }
    DefaultMutableTreeTableNode[] dmtnselected = new DefaultMutableTreeTableNode[currentSelection.length];

    for (int i = 0; i < currentSelection.length; i++) {
      dmtnselected[i] = (DefaultMutableTreeTableNode) currentSelection[i].getLastPathComponent();
    }
    return dmtnselected;
  }

  public boolean isExpanded(HumanResource hr) {
    ResourceNode node = ((ResourceTreeTableModel) getTreeTableModel()).getNodeForResource(hr);
    if (node != null) {
      return getTreeTable().isExpanded(TreeUtil.createPath(node));
    }
    return false;
  }

  public boolean canMoveSelectionUp() {
    final DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
    if (selectedNodes.length != 1) {
      return false;
    }
    DefaultMutableTreeTableNode selectedNode = selectedNodes[0];
    TreeNode previousSibling = TreeUtil.getPrevSibling(selectedNode);
    if (previousSibling == null) {
      return false;
    }
    return true;
  }

  /** Move selected resource up */
  public void upResource() {
    final DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
    if (selectedNodes.length != 1) {
      return;
    }
    DefaultMutableTreeTableNode selectedNode = selectedNodes[0];
    TreeNode previousSibling = TreeUtil.getPrevSibling(selectedNode);
    if (previousSibling == null) {
      return;
    }
    if (selectedNode instanceof ResourceNode) {
      HumanResource people = (HumanResource) selectedNode.getUserObject();
      myResourceTreeModel.moveUp(people);
      getTreeSelectionModel().setSelectionPath(TreeUtil.createPath(selectedNode));
    } else if (selectedNode instanceof AssignmentNode) {
      swapAssignents((AssignmentNode) selectedNode, (AssignmentNode) previousSibling);
    }
  }

  public boolean canMoveSelectionDown() {
    final DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
    if (selectedNodes.length != 1) {
      return false;
    }
    DefaultMutableTreeTableNode selectedNode = selectedNodes[0];
    TreeNode nextSibling = TreeUtil.getNextSibling(selectedNode);
    if (nextSibling == null) {
      return false;
    }
    return true;
  }

  /** Move the selected resource down */
  public void downResource() {
    final DefaultMutableTreeTableNode[] selectedNodes = getSelectedNodes();
    if (selectedNodes.length == 0) {
      return;
    }
    DefaultMutableTreeTableNode selectedNode = selectedNodes[0];
    TreeNode nextSibling = TreeUtil.getNextSibling(selectedNode);
    if (nextSibling == null) {
      return;
    }
    if (selectedNode instanceof ResourceNode) {
      HumanResource people = (HumanResource) selectedNode.getUserObject();
      myResourceTreeModel.moveDown(people);
      getTreeSelectionModel().setSelectionPath(TreeUtil.createPath(selectedNode));
    } else if (selectedNode instanceof AssignmentNode) {
      swapAssignents((AssignmentNode) selectedNode, (AssignmentNode) nextSibling);
    }
  }

  void swapAssignents(AssignmentNode selected, AssignmentNode sibling) {
    ResourceAssignment selectedAssignment = selected.getAssignment();
    ResourceAssignment previousAssignment = sibling.getAssignment();
    selectedAssignment.getResource().swapAssignments(selectedAssignment, previousAssignment);
  }
}
