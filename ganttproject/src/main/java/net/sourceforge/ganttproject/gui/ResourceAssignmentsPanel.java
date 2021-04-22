/*
Copyright 2017 Oleg Kushnikov, BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui;

import net.sourceforge.ganttproject.gui.taskproperties.CommonPanel;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.util.HashMap;
import java.util.Map;

/**
 * UI component in a resource properties dialog: a table with tasks assigned to
 * a resource.
 *
 * @author Oleg Kushnikov
 */
public class ResourceAssignmentsPanel {
  private static Map<Integer, Integer> ourColumnWidth = new HashMap<>();
  private final TaskManager myTaskManager;
  private ResourceAssignmentsTableModel myModel;
  private final HumanResource myPerson;
  private JTable myTable;

  ResourceAssignmentsPanel(HumanResource person, TaskManager taskManager) {
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
    SwingUtilities.invokeLater(() -> CommonPanel.loadColumnWidth(myTable, ourColumnWidth));
    return AbstractTableAndActionsComponent.createDefaultTableAndActions(getTable(), myModel);
  }

  public void commit() {
    if (myTable.isEditing()) {
      myTable.getCellEditor().stopCellEditing();
    }
    myModel.commit();
    CommonPanel.saveColumnWidths(myTable, ourColumnWidth);
  }

  private void setUpTasksComboColumn(TableColumn column, final JTable table) {
    final JComboBox comboBox = new JComboBox();
    Task[] tasks = myTaskManager.getTasks();
    for (Task next : tasks) {
      comboBox.addItem(next);
    }
    comboBox.setEditable(false);
    column.setCellEditor(new DefaultCellEditor(comboBox));
  }
}
