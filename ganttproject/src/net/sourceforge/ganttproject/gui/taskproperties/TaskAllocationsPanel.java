/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.taskproperties;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.ResourcesTableModel;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskAllocationsPanel {
    private ResourcesTableModel myModel;
    private final HumanResourceManager myHRManager;
    private final RoleManager myRoleManager;
    private final Task myTask;

    private JTable myTable;

    public TaskAllocationsPanel(Task task, HumanResourceManager hrManager,  RoleManager roleMgr) {
        myHRManager = hrManager;
        myRoleManager = roleMgr;
        myTask = task;
    }

    private JTable getTable() {
        return myTable;
    }
    public JPanel getComponent() {
        myModel = new ResourcesTableModel(myTask.getAssignmentCollection());
        myTable = new JTable(myModel);
        CommonPanel.setupTableUI(getTable());
        setUpCoordinatorBooleanColumn(getTable());
        CommonPanel.setupComboBoxEditor(
                getTable().getColumnModel().getColumn(1),
                myHRManager.getResources().toArray());
        CommonPanel.setupComboBoxEditor(
                getTable().getColumnModel().getColumn(4),
                myRoleManager.getEnabledRoles());

        AbstractTableAndActionsComponent<TaskDependency> tableAndActions =
            new AbstractTableAndActionsComponent<TaskDependency>(getTable()) {
                @Override
                protected void onAddEvent() {
                    getTable().editCellAt(myModel.getRowCount() - 1, 1);
                }

                @Override
                protected void onDeleteEvent() {
                    myModel.delete(getTable().getSelectedRows());
                }

                @Override
                protected void onSelectionChanged() {
                }
        };
        return CommonPanel.createTableAndActions(myTable, tableAndActions);
    }


    private void setUpCoordinatorBooleanColumn(final JTable resourceTable) {
        TableColumn resourcesColumn = resourceTable.getColumnModel().getColumn(3);
        resourcesColumn.setCellRenderer(new BooleanRenderer());
    }

    static class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        private static JPanel EMPTY_LABEL = new JPanel();

        public BooleanRenderer() {
            super();
            setHorizontalAlignment(JLabel.CENTER);
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            final JComponent result;
            if (value == null || "".equals(value)) {
                result = EMPTY_LABEL;
            } else {
                setSelected(((Boolean) value).booleanValue());
                result = this;
            }
            setupRendererColors(isSelected, table, result);
            return result;
        }

        private static void setupRendererColors(boolean isSelected, JTable table, JComponent component) {
            if (isSelected) {
                component.setForeground(table.getSelectionForeground());
                component.setBackground(table.getSelectionBackground());
            } else {
                component.setForeground(table.getForeground());
                component.setBackground(table.getBackground());
            }
        }
    }

    public void commit() {
        myModel.commit();
    }
}
