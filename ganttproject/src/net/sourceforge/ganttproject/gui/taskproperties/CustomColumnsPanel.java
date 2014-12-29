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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import biz.ganttproject.core.table.ColumnList;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyHolder;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.ShowHideColumnsDialog;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * This class implements a UI component for editing custom properties.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CustomColumnsPanel {
  private static GanttLanguage language = GanttLanguage.getInstance();
  private static final String[] COLUMN_NAMES = new String[] { CustomColumnsPanel.language.getText("name"),
    CustomColumnsPanel.language.getText("typeClass"), CustomColumnsPanel.language.getText("value") };

  private final CustomPropertyManager myCustomPropertyManager;

  private final UIFacade myUiFacade;

  private CustomColumnTableModel myModel;

  private JTable myTable;

  private CustomPropertyHolder myHolder;

  private ColumnList myTableHeaderFacade;

  public CustomColumnsPanel(CustomPropertyManager manager, UIFacade uifacade,
      CustomPropertyHolder customPropertyHolder, ColumnList tableHeaderFacade) {
    assert manager != null;
    myCustomPropertyManager = manager;
    myUiFacade = uifacade;
    myHolder = customPropertyHolder;
    myTableHeaderFacade = tableHeaderFacade;
  }

  public JComponent getComponent() {
    myModel = new CustomColumnTableModel();
    myTable = new JTable(myModel);

    UIUtil.setupTableUI(myTable);
    JPanel buttonPanel = new JPanel(new BorderLayout());
    buttonPanel.add(new JButton(new GPAction("columns.manage.label") {
      @Override
      public void actionPerformed(ActionEvent e) {
        ShowHideColumnsDialog dialog = new ShowHideColumnsDialog(myUiFacade, myTableHeaderFacade,
            myCustomPropertyManager);
        dialog.show();
        myModel.fireTableStructureChanged();
      }
    }), BorderLayout.WEST);
    return CommonPanel.createTableAndActions(myTable, buttonPanel);
  }

  public void commit() {
    if (myTable.isEditing()) {
      myTable.getCellEditor().stopCellEditing();
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
        return def.getDefaultValue() + " (default)";
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
      myHolder.addCustomProperty(myCustomPropertyManager.getDefinitions().get(row), String.valueOf(o));
      // myHolder.addCustomProperty(def,
      // DateParser.getIsoDate(GanttLanguage.getInstance().parseDate(String.valueOf(o))));
    }
  }
}
