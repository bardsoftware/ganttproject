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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.StartStartConstraintImpl;

/**
 * UI component in a task properties dialog: a table with task predecessors
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskDependenciesPanel {
    private static TaskDependencyConstraint[] CONSTRAINTS = new TaskDependencyConstraint[] {
        new FinishStartConstraintImpl(), new FinishFinishConstraintImpl(),
        new StartFinishConstraintImpl(), new StartStartConstraintImpl() };

    private static TaskDependency.Hardness[] HARDNESS = new TaskDependency.Hardness[] {
        TaskDependency.Hardness.STRONG, TaskDependency.Hardness.RUBBER
    };

    private Task myTask;
    private DependencyTableModel myModel;
    private JTable myTable;

    private JTable getTable() {
        return myTable;
    }

    public JPanel getComponent() {
        myModel = new DependencyTableModel(myTask);
        myTable = new JTable(myModel);
        CommonPanel.setupTableUI(myTable);
        setUpPredecessorComboColumn(
                DependencyTableModel.MyColumn.TASK_NAME.getTableColumn(getTable()),
                getTable());
        CommonPanel.setupComboBoxEditor(
                DependencyTableModel.MyColumn.CONSTRAINT_TYPE.getTableColumn(getTable()),
                CONSTRAINTS);
        CommonPanel.setupComboBoxEditor(
                DependencyTableModel.MyColumn.HARDNESS.getTableColumn(getTable()),
                HARDNESS);
        AbstractTableAndActionsComponent<TaskDependency> tableAndActions =
            new AbstractTableAndActionsComponent<TaskDependency>(getTable()) {
                @Override
                protected void onAddEvent() {
                    getTable().editCellAt(
                            myModel.getRowCount() - 1, DependencyTableModel.MyColumn.TASK_NAME.ordinal());
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

    public void init(Task task) {
        myTask = task;
    }

    public void commit() {
        if (myTable.isEditing()) {
            myTable.getCellEditor().stopCellEditing();
        }
        myModel.commit();
    }

    private Task getTask() {
        return myTask;
    }

    protected void setUpPredecessorComboColumn(TableColumn predecessorColumn, final JTable predecessorTable) {
        final JComboBox comboBox = new JComboBox();
        Task[] possiblePredecessors = getTaskManager().getAlgorithmCollection()
                .getFindPossibleDependeesAlgorithm().run(getTask());
        for (int i = 0; i < possiblePredecessors.length; i++) {
            Task next = possiblePredecessors[i];
            comboBox.addItem(new DependencyTableModel.TaskComboItem(next));
        }

        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (predecessorTable.getEditingRow() != -1) {
                    DependencyTableModel.TaskComboItem selectedItem =
                        (DependencyTableModel.TaskComboItem) comboBox.getSelectedItem();
                    if (selectedItem != null) {
                        predecessorTable.setValueAt(selectedItem,
                                predecessorTable.getEditingRow(), 0);
                        predecessorTable.setValueAt(TaskDependenciesPanel.CONSTRAINTS[0],
                                predecessorTable.getEditingRow(), 2);
                    }
                }
            }
        });
        comboBox.setEditable(false);
        predecessorColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }

    private TaskManager getTaskManager() {
        return getTask().getManager();
    }
}
