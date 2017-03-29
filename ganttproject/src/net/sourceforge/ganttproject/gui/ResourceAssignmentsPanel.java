/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui;

import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import javax.swing.*;
import javax.swing.table.TableColumn;

/**
 * UI component in a resource properties dialog: a table with tasks assigned to
 * a resource.
 *
 * @author Oleg Kushnikov
 */
public class ResourceAssignmentsPanel {
  private final TaskManager myTaskManager;
  private ResourceAssignmentsTableModel myModel;
  private final HumanResource myPerson;
  private JTable myTable;

  public ResourceAssignmentsPanel(HumanResource person, TaskManager taskManager) {
    myPerson = person;
    myTaskManager = taskManager;
  }

  private JTable getTable() {
    return myTable;
  }

  public JPanel getComponent() {
    myModel = new ResourceAssignmentsTableModel(myPerson);
    myTable = new JTable(myModel);
    UIUtil.setupTableUI(getTable());
    setUpTasksComboColumn(getTable().getColumnModel().getColumn(ResourceAssignmentsTableModel.Column.NAME.ordinal()), getTable());
    AbstractTableAndActionsComponent<ResourceAssignment> tableAndActions =
        new AbstractTableAndActionsComponent<ResourceAssignment>(getTable()) {
      @Override
      protected void onAddEvent() {
        getTable().editCellAt(myModel.getRowCount() - 1, 1);
      }

      @Override
      protected void onDeleteEvent() {
        if (getTable().isEditing() && getTable().getCellEditor() != null) {
          getTable().getCellEditor().stopCellEditing();
        }
        myModel.delete(getTable().getSelectedRows());
      }

      @Override
      protected ResourceAssignment getValue(int row) {
        java.util.List<ResourceAssignment> values = myModel.getResourcesAssignments();
        return (row >= 0 && row < values.size()) ? values.get(row) : null;
      }
    };
    JPanel tablePanel = AbstractTableAndActionsComponent.createDefaultTableAndActions(getTable(), tableAndActions.getActionsComponent());
    return tablePanel;
  }

  public void commit() {
    if (myTable.isEditing()) {
      myTable.getCellEditor().stopCellEditing();
    }
    myModel.commit();
  }

  protected void setUpTasksComboColumn(TableColumn column, final JTable table) {
    final JComboBox comboBox = new JComboBox();
    Task[] tasks = myTaskManager.getTasks();
    for (int i = 0; i < tasks.length; i++) {
      Task next = tasks[i];
      comboBox.addItem(next);
    }
    comboBox.setEditable(false);
    column.setCellEditor(new DefaultCellEditor(comboBox));
  }
}
