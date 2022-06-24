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
package net.sourceforge.ganttproject.gui.taskproperties;

import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.customproperty.*;
import biz.ganttproject.ganttview.ApplyExecutorType;
import biz.ganttproject.ganttview.ColumnManagerKt;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.storage.ProjectDatabase;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.undo.GPUndoManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements a UI component for editing custom properties.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CustomColumnsPanel {
  private final Type myType;
  private final GPUndoManager myUndoManager;
  private final ProjectDatabase myProjectDatabase;

  public enum Type { TASK, RESOURCE }
  private static final Map<Integer, Integer> ourColumnWidth = new HashMap<>();
  private static final GanttLanguage language = GanttLanguage.getInstance();
  private static final String[] COLUMN_NAMES = new String[] { CustomColumnsPanel.language.getText("name"),
    CustomColumnsPanel.language.getText("typeClass"), CustomColumnsPanel.language.getText("value") };

  private final CustomPropertyManager myCustomPropertyManager;

  private CustomColumnTableModel myModel;

  private JTable myTable;

  private final CustomPropertyHolder myHolder;

  private final ColumnList myTableHeaderFacade;

  public CustomColumnsPanel(
    CustomPropertyManager manager, ProjectDatabase projectDatabase, Type type, GPUndoManager undoManager, CustomPropertyHolder customPropertyHolder, ColumnList tableHeaderFacade) {
    assert manager != null;
    myCustomPropertyManager = manager;
    myHolder = customPropertyHolder;
    myTableHeaderFacade = tableHeaderFacade;
    myType = type;
    myUndoManager = undoManager;
    myProjectDatabase = projectDatabase;
  }

  public JComponent getComponent() {
    myModel = new CustomColumnTableModel();
    myTable = new JTable(myModel);

    UIUtil.setupTableUI(myTable);
    JPanel buttonPanel = new JPanel(new BorderLayout());
    buttonPanel.add(new JButton(new GPAction("columns.manage.label") {
      @Override
      public void actionPerformed(ActionEvent e) {
        switch (myType) {
          case TASK:
            ColumnManagerKt.showTaskColumnManager(myTableHeaderFacade, myCustomPropertyManager, myUndoManager, myProjectDatabase, ApplyExecutorType.SWING);
            break;
          case RESOURCE:
            ColumnManagerKt.showResourceColumnManager(myTableHeaderFacade, myCustomPropertyManager, myUndoManager, myProjectDatabase, ApplyExecutorType.SWING);
            break;

        }
        myModel.fireTableStructureChanged();
      }
    }), BorderLayout.WEST);
    SwingUtilities.invokeLater(() -> CommonPanel.loadColumnWidth(myTable, ourColumnWidth));
    return CommonPanel.createTableAndActions(myTable, buttonPanel);
  }

  public void commit(TaskMutator mutator) {
    if (myTable.isEditing()) {
      myTable.getCellEditor().stopCellEditing();
    }
    CommonPanel.saveColumnWidths(myTable, ourColumnWidth);
    try {
      mutator.setCustomProperties(myHolder);
    } catch (CustomColumnsException e) {
      throw new RuntimeException(e);
    }
  }

  class CustomColumnTableModel extends DefaultTableModel {
    public CustomColumnTableModel() {
    }

    public void reload() {
      fireTableDataChanged();
    }

    @Override
    public String getColumnName(int column) {
      return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
      return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
      return col == 2;
    }

    @Override
    public int getColumnCount() {
      return COLUMN_NAMES.length;
    }

    @Override
    public int getRowCount() {
      return myCustomPropertyManager.getDefinitions().size();
    }

    @Override
    public Object getValueAt(int row, int col) {
      if (row < 0 || row >= myCustomPropertyManager.getDefinitions().size()) {
        return null;
      }
      CustomPropertyDefinition def = myCustomPropertyManager.getDefinitions().get(row);
      switch (col) {
      case 0:
        return def.getName();
      case 1:
        return def.getPropertyClass().getDisplayName();
      case 2:
        for (CustomProperty cp : myHolder.getCustomProperties()) {
          if (cp.getDefinition() == def) {
            return cp.getValueAsString();
          }
        }
        var defValue = def.getDefaultValue();
        return defValue == null ? "" : defValue;
      default:
        throw new IllegalStateException();
      }
    }

    @Override
    public void setValueAt(Object o, int row, int col) {
      if (row < 0 || row >= myCustomPropertyManager.getDefinitions().size()) {
        return;
      }
      if (col != 2) {
        throw new IllegalArgumentException();
      }
      try {
        myHolder.addCustomProperty(myCustomPropertyManager.getDefinitions().get(row), String.valueOf(o));
      } catch (CustomColumnsException ex) {
        throw new RuntimeException(ex);
      }
      // myHolder.addCustomProperty(def,
      // DateParser.getIsoDate(GanttLanguage.getInstance().parseDate(String.valueOf(o))));
    }
  }
}
