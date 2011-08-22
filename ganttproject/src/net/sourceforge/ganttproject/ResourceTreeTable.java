/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swing.table.TableColumnExt;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.AssignmentNode;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceNode;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleManager.RoleEvent;
import net.sourceforge.ganttproject.task.ResourceAssignment;

public class ResourceTreeTable extends GPTreeTableBase {
    private final RoleManager myRoleManager;

    private final ResourceTreeTableModel myResourceTreeModel;

    private final IGanttProject myProject;

    private final UIFacade myUiFacade;

    private static enum DefaultColumn {
        NAME(new TableHeaderUIFacade.ColumnStub("0", null, true, 0, 200)),
        ROLE(new TableHeaderUIFacade.ColumnStub("1", null, true, 1, 75)),
        EMAIL(new TableHeaderUIFacade.ColumnStub("2", null, false, -1, 75)),
        PHONE(new TableHeaderUIFacade.ColumnStub("3", null, false, -1, 50)),
        ROLE_IN_TASK(new TableHeaderUIFacade.ColumnStub("4", null, false, -1, 75)),
        ;

        private final Column myDelegate;
        private DefaultColumn(TableHeaderUIFacade.Column delegate) {
            myDelegate = delegate;
        }

        Column getStub() {
            return myDelegate;
        }

        static List<Column> getColumnStubs() {
            List<Column> result = new ArrayList<Column>();
            for (DefaultColumn dc : values()) {
                result.add(dc.myDelegate);
            }
            return result;
        }
    }

    public ResourceTreeTable(GanttProject project, ResourceTreeTableModel model, UIFacade uiFacade) {
        super(project, uiFacade, project.getResourceCustomPropertyManager(), model);
        myUiFacade = uiFacade;
        myProject = project;
        myRoleManager = project.getRoleManager();
        myRoleManager.addRoleListener(new RoleManager.Listener() {
            public void rolesChanged(RoleEvent e) {
                setEditor(getTableHeaderUiFacade().findColumnByID(DefaultColumn.ROLE.getStub().getID()));
                setEditor(getTableHeaderUiFacade().findColumnByID(DefaultColumn.ROLE_IN_TASK.getStub().getID()));
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
        getTableHeaderUiFacade().createDefaultColumns(DefaultColumn.getColumnStubs());
        setTreeTableModel(model);
        initTreeTable();
        myResourceTreeModel.setSelectionModel(getTree().getSelectionModel());
    }

    public boolean isVisible(DefaultMutableTreeNode node) {
        return getTreeTable().getTree().isVisible(new TreePath(node.getPath()));
    }


    @Override
    protected List<Column> getDefaultColumns() {
        return DefaultColumn.getColumnStubs();
    }

    @Override
    protected Chart getChart() {
        return myUiFacade.getResourceChart();
    }

    /**
     * Initialize the treetable. Addition of various listeners, tree's icons,
     */
    @Override
    protected void doInit() {
        super.doInit();
        myResourceTreeModel.updateResources();
        getVerticalScrollBar().addAdjustmentListener(new VscrollAdjustmentListener(false) {
            @Override
            protected TimelineChart getChart() {
                return (TimelineChart)myUiFacade.getResourceChart();
            }

        });
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
        if (modelIndex == DefaultColumn.ROLE.ordinal() || modelIndex == DefaultColumn.ROLE_IN_TASK.ordinal()) {
            JComboBox comboBox = new JComboBox(getRoleManager().getEnabledRoles());
            comboBox.setEditable(false);
            tableColumn.setCellEditor(new DefaultCellEditor(comboBox));
        }
        return tableColumn;
    }

    /** @return the list of the selected nodes. */
    public DefaultMutableTreeNode[] getSelectedNodes() {
        TreePath[] currentSelection = getTreeTable().getTree()
                .getSelectionPaths();

        if (currentSelection == null || currentSelection.length == 0) {
            return new DefaultMutableTreeNode[0];
        }
        DefaultMutableTreeNode[] dmtnselected = new DefaultMutableTreeNode[currentSelection.length];

        for (int i = 0; i < currentSelection.length; i++) {
            dmtnselected[i] = (DefaultMutableTreeNode) currentSelection[i]
                    .getLastPathComponent();
        }
        return dmtnselected;
    }

    public boolean isExpanded(HumanResource hr) {
        ResourceNode node = ((ResourceTreeTableModel) getTreeTableModel())
                .exists(hr);
        if (node != null) {
            return getTreeTable().isExpanded(new TreePath(node.getPath()));
        }
        return false;
    }

    public void setAction(Action action) {
        InputMap inputMap = new InputMap();

        inputMap.put((KeyStroke) action.getValue(Action.ACCELERATOR_KEY),
                action.getValue(Action.NAME));

        inputMap.setParent(getTreeTable().getInputMap(JComponent.WHEN_FOCUSED));
        getTreeTable().setInputMap(JComponent.WHEN_FOCUSED, inputMap);

        // Add the action to the component
        getTreeTable().getActionMap().put(action.getValue(Action.NAME), action);
    }

    boolean canMoveSelectionUp() {
        final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
        if(selectedNodes.length!=1) {
            return false;
        }
        DefaultMutableTreeNode selectedNode = selectedNodes[0];
        DefaultMutableTreeNode previousSibling = selectedNode.getPreviousSibling();
        if (previousSibling == null) {
            return false;
        }
        return true;
    }

    void upResource() {
        final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
        if(selectedNodes.length!=1) {
            return;
        }
        DefaultMutableTreeNode selectedNode = selectedNodes[0];
        DefaultMutableTreeNode previousSibling = selectedNode.getPreviousSibling();
        if (previousSibling == null) {
            return;
        }
        if (selectedNode instanceof ResourceNode) {
            HumanResource people = (HumanResource)selectedNode.getUserObject();
            myResourceTreeModel.moveUp(people);
            getTree().setSelectionPath(new TreePath(selectedNode.getPath()));
        } else if (selectedNode instanceof AssignmentNode) {
            swapAssignents(selectedNode, previousSibling);
        }
    }

    boolean canMoveSelectionDown() {
        final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
        if(selectedNodes.length!=1) {
            return false;
        }
        DefaultMutableTreeNode selectedNode = selectedNodes[0];
        DefaultMutableTreeNode nextSibling = selectedNode.getNextSibling();
        if (nextSibling == null) {
            return false;
        }
        return true;
    }

    /** Move down the selected resource */
    void downResource() {
        final DefaultMutableTreeNode[] selectedNodes = getSelectedNodes();
        if(selectedNodes.length==0) {
            return;
        }
        DefaultMutableTreeNode selectedNode = selectedNodes[0];
        DefaultMutableTreeNode nextSibling = selectedNode.getNextSibling();
        if (nextSibling == null) {
            return;
        }
        if (selectedNode instanceof ResourceNode) {
            HumanResource people = (HumanResource)selectedNode.getUserObject();
            myResourceTreeModel.moveDown(people);
            getTree().setSelectionPath(new TreePath(selectedNode.getPath()));
        } else if (selectedNode instanceof AssignmentNode) {
            swapAssignents(selectedNode, nextSibling);
        }
    }

    void swapAssignents(DefaultMutableTreeNode selected, DefaultMutableTreeNode sibling) {
        ResourceAssignment selectedAssignment = ((AssignmentNode)selected).getAssignment();
        assert sibling instanceof AssignmentNode;
        ResourceAssignment previousAssignment = ((AssignmentNode)sibling).getAssignment();
        selectedAssignment.getResource().swapAssignments(selectedAssignment, previousAssignment);
    }
/*
    public CustomPropertyDefinition createDefinition(String id, String typeAsString, String name, String defaultValueAsString) {
        final ResourceColumn newColumn = newResourceColumn(Integer.valueOf(id).intValue());
        newColumn.setTitle(name);
        final CustomPropertyDefinition stubDefinition = CustomPropertyManager.PropertyTypeEncoder.decodeTypeAndDefaultValue(typeAsString, defaultValueAsString);
        newColumn.setType(stubDefinition.getType());
        newColumn.setDefaultVal(stubDefinition.getDefaultValue());
        assert String.valueOf(newColumn.getIndex()).equals(id);
        addCustomColumn(newColumn);
        CustomPropertyDefinition result = myResourceManager.getCustomPropertyManager().getCustomPropertyDefinition(name);
        assert result != null : "Where is custom property " + name + " I just've created?";
        return result;
    }

    public List<CustomPropertyDefinition> getDefinitions() {
        return myResourceManager.getDefinitions();
    }

    public void importData(CustomPropertyManager source) {
        List<CustomPropertyDefinition> sourceDefs = source.getDefinitions();
        for (int i=0; i<sourceDefs.size(); i++) {
            CustomPropertyDefinition nextDefinition = sourceDefs.get(i);
            createDefinition(nextDefinition.getID(),
                             nextDefinition.getTypeAsString(),
                             nextDefinition.getName(),
                             nextDefinition.getDefaultValueAsString());
        }
    }
*/
/*
    class TableHeaderImpl implements TableHeaderUIFacade {
        public void add(String name, int order, int width) {
            ArrayList<ResourceColumn> cols = myResourceTreeModel.getColumns();
            for (int i =0; i < cols.size(); i++) {
                ResourceColumn col = cols.get(i);
                if (name.equals(col.getID()) && !col.isVisible()) {
                    col.setWidth(width);
                    col.setOrder(order);
                    showColumn(col);
                }
            }
        }

        public void clear() {
            deleteAllColumns();
        }

        public Column getField(int index) {
            return myResourceTreeModel.getColumns().get(index);
        }

        public int getSize() {
            return myResourceTreeModel.getColumns().size();
        }

        public void importData(TableHeaderUIFacade source) {
            if (source.getSize() == 0) {
              return;
            }
            ArrayList<ResourceColumn> cols = myResourceTreeModel.getColumns();
            for (int i =0; i < cols.size(); i++) {
                ResourceColumn col = cols.get(i);
                hideColumn(col);
            }
            ArrayList<Column> sourceColumns = new ArrayList<Column>();
            for (int i=0; i<source.getSize(); i++) {
                Column nextField = source.getField(i);
                sourceColumns.add(nextField);
            }
            Collections.sort(sourceColumns, new Comparator<Column>() {
                public int compare(Column lhs, Column rhs) {
                    return lhs.getOrder()-rhs.getOrder();
                }
            });
            for (int i=0; i<sourceColumns.size(); i++) {
                Column nextField = sourceColumns.get(i);
                add(nextField.getID(), i, nextField.getWidth());
            }
        }
    }
*/
}
