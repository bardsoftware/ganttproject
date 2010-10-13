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

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.ResourcesTableModel;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;

import org.jdesktop.jdnc.JNTable;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskAllocationsPanel {
    private ResourcesTableModel myModel;
    private final HumanResourceManager myHRManager;
    private final RoleManager myRoleManager;
    private final Task myTask;

	private JNTable myTable;

    public TaskAllocationsPanel(Task task, HumanResourceManager hrManager,  RoleManager roleMgr) {
        myHRManager = hrManager;
        myRoleManager = roleMgr;
        myTask = task;
    }

    public JPanel getComponent() {
        myModel = new ResourcesTableModel(myTask.getAssignmentCollection());
        myTable = new JNTable(myModel);
        CommonPanel.setupTableUI(myTable);
        setUpCoordinatorBooleanColumn(myTable.getTable());
        CommonPanel.setupComboBoxEditor(
        		myTable.getTable().getColumnModel().getColumn(1), 
        		myHRManager.getResources().toArray());
        CommonPanel.setupComboBoxEditor(
        		myTable.getTable().getColumnModel().getColumn(4), 
        		myRoleManager.getEnabledRoles());

        AbstractTableAndActionsComponent<TaskDependency> tableAndActions =
            new AbstractTableAndActionsComponent<TaskDependency>(myTable.getTable()) {
                @Override
                protected void onAddEvent() {
                    myTable.getTable().editCellAt(myModel.getRowCount(), 1);
                }

                @Override
                protected void onDeleteEvent() {
                    myModel.delete(myTable.getTable().getSelectedRows());
                }

                @Override
                protected void onSelectionChanged() {
                }
        };
        JPanel result = new JPanel(new BorderLayout());
        result.add(tableAndActions.getActionsComponent(), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(myTable);
        result.add(scrollPane, BorderLayout.CENTER);
        return result;
    }


    private void setUpCoordinatorBooleanColumn(final JTable resourceTable) {
        TableColumn resourcesColumn = resourceTable.getColumnModel().getColumn(3);
        resourcesColumn.setCellRenderer(new BooleanRenderer());
    }

    static class BooleanRenderer extends JCheckBox implements TableCellRenderer {
        public BooleanRenderer() {
            super();
            setHorizontalAlignment(JLabel.CENTER);
        }

        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            if (isSelected) {
                setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }
            if (!value.getClass().equals(Boolean.class))
                setSelected(false);
            else
                setSelected((value != null && ((Boolean) value).booleanValue()));

            return this;
        }
    }

	public void commit() {
		myModel.commit();
	}
}
