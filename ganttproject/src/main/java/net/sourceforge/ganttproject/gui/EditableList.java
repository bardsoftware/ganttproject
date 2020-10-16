/*
GanttProject is an opensource project management tool.
Copyright (C) 2010-2011 Dmitry Barashev

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

import biz.ganttproject.core.option.ValidationException;
import net.sourceforge.ganttproject.language.GanttLanguage;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class EditableList<T> {

  private final Object UNDEFINED_VALUE = new Object() {
    @Override
    public String toString() {
      return myUndefinedValueLabel;
    }
  };
  private final List<T> myValues;
  private final TableModelImpl myTableModel;
  private JTable resourcesTable;
  private AbstractTableAndActionsComponent<T> myTableAndActions;
  private JScrollPane resourcesScrollPane;
  private int[] mySelectedRows;
  private JComboBox myComboBox;
  private final List<T> myPossibleValues;
  private String myTitle;
  private String myUndefinedValueLabel = GanttLanguage.getInstance().getText("editableList.undefinedValueLabel");

  public EditableList(List<T> assigned_values, List<T> possibleValues) {
    myValues = assigned_values;
    myPossibleValues = possibleValues;
    myTableModel = new TableModelImpl();
  }

  public void setUndefinedValueLabel(String label) {
    myUndefinedValueLabel = label;
  }

  public void setTitle(String title) {
    myTitle = title;
  }

  public String getTitle() {
    return myTitle;
  }

  public JComponent getTableComponent() {
    initComponent();
    return resourcesScrollPane;
  }

  public JComponent getActionsComponent() {
    initComponent();
    return myTableAndActions.getActionsComponent();
  }

  public void stopEditing() {
    if (resourcesTable.isEditing()) {
      resourcesTable.getCellEditor().stopCellEditing();
    }
  }

  public JComponent createDefaultComponent() {
    JPanel result = new JPanel(new BorderLayout());
    result.add(getTableComponent(), BorderLayout.CENTER);
    JComponent actionsComponent = getActionsComponent();
    actionsComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
    result.add(actionsComponent, BorderLayout.NORTH);
    result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    return result;
  }

  public AbstractTableAndActionsComponent<T> getTableAndActions() {
    initComponent();
    return myTableAndActions;
  }

  private void initComponent() {
    if (myTableAndActions == null) {
      // JXTable jnTable = new JNTable(myTableModel);
      resourcesTable = new JTable(myTableModel) {
        @Override
        public String getToolTipText(MouseEvent event) {
          try {
            return super.getToolTipText(event);
          } catch (NullPointerException e) {
            return null;
          }
        }
      };
      UIUtil.setupTableUI(resourcesTable);
      resourcesTable.setTableHeader(null);
      resourcesTable.getColumnModel().getColumn(0).setPreferredWidth(240);
      resourcesTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
          assert column == 0;
          super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
          if (row >= myValues.size()) {
            return this;
          }
          T typedValue = EditableList.this.myValues.get(row);
          return EditableList.this.getTableCellRendererComponent(this, typedValue, isSelected, hasFocus, row);
        }
      });
      JTextField editorField = new JTextField();
      resourcesTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(editorField) {
        private boolean isCanceled;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
          isCanceled = false;
          JTextField result = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);
          if (UNDEFINED_VALUE == value) {
            result.setText("");
          }
          return result;
        }

        @Override
        public void cancelCellEditing() {
          super.cancelCellEditing();
          isCanceled = true;
        }

        @Override
        public Object getCellEditorValue() {
          if (isCanceled) {
            return UNDEFINED_VALUE;
          }
          return super.getCellEditorValue();
        }

        @Override
        public boolean stopCellEditing() {
          try {
            boolean result = super.stopCellEditing();
            getComponent().setBackground(UIUtil.getValidFieldColor());
            return result;
          } catch (ValidationException e) {
            getComponent().setBackground(UIUtil.ERROR_BACKGROUND);
            return false;
          }
        }
      });
      if (!myPossibleValues.isEmpty()) {
        setupEditor(myPossibleValues, resourcesTable);
      }
      // resourcesTable.setHighlighters(HighlighterFactory.createSimpleStriping());
      resourcesScrollPane = new JScrollPane(resourcesTable);
      // resourcesScrollPane.setPreferredSize(new Dimension(300, 130));

      myTableAndActions = new TableAndActionsImpl();
    }
  }

  protected Component getTableCellRendererComponent(DefaultTableCellRenderer defaultRenderer, T typedValue,
      boolean isSelected, boolean hasFocus, int row) {
    defaultRenderer.setText(getStringValue(typedValue));
    return defaultRenderer;
  }

  List<T> getSelectedObjects() {
    int[] selectedRows = resourcesTable.getSelectedRows();
    if (selectedRows.length == 0) {
      return Collections.emptyList();
    }
    ArrayList<T> result = new ArrayList<T>();
    for (int nextRow : selectedRows) {
      if (nextRow >= 0 && nextRow < myValues.size()) {
        result.add(myValues.get(nextRow));
      }
    }
    return result;
  }

  public T getSelectedObject() {
    int selIndex = resourcesTable.getSelectedRow();
    return selIndex >= 0 && selIndex < myValues.size() ? myValues.get(selIndex) : null;
  }

  class TableModelImpl extends AbstractTableModel {
    @Override
    public int getColumnCount() {
      return 1;
    }

    @Override
    public int getRowCount() {
      return myValues.size() + 1;
    }

    @Override
    public Object getValueAt(int row, int col) {
      if (row >= 0 && row < myValues.size()) {
        return new ComboItem(myValues.get(row));
      }
      if (row == myValues.size()) {
        return UNDEFINED_VALUE;
      }
      throw new IllegalArgumentException("I can't return data in row=" + row);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
      if (row == myValues.size()) {
        return true;
      }
      return EditableList.this.isEditable(myValues.get(row));
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
      assert col == 0;
      if (value == null) {
        deleteValue(myValues.get(row));
        myValues.remove(row);
        fireTableRowsDeleted(row, row);
        return;
      }
      T prototype = createPrototype(value);
      if (row >= myValues.size()) {
        if (prototype != null) {
          T newValue = createValue(prototype);
          if (newValue != null) {
            myValues.add(newValue);
            fireTableRowsInserted(myValues.size(), myValues.size());
          }
        }
      } else if (row >= 0) {
        if (prototype != myValues.get(row)) {
          T updatedValue = updateValue(prototype, myValues.get(row));
          myValues.set(row, updatedValue);
          fireTableRowsUpdated(row, row);
        }
      } else {
        throw new IllegalArgumentException("I can't set data in row=" + row);
      }
    }
  }

  class ComboItem {
    final String myText;

    final T myObject;

    ComboItem(T t) {
      myObject = t;
      myText = getStringValue(t);
    }

    @Override
    public String toString() {
      return myText;
    }
  }

  protected T createPrototype(Object editValue) {
    ComboItem setItem = null;
    if (ComboItem.class.equals(editValue.getClass())) {
      setItem = (ComboItem) editValue;
    } else {
      for (int i = 0; i < myComboBox.getModel().getSize(); i++) {
        if (((ComboItem) myComboBox.getModel().getElementAt(i)).myText.equals(editValue)) {
          setItem = (ComboItem) myComboBox.getModel().getElementAt(i);
          break;
        }
      }
    }
    return setItem == null ? null : setItem.myObject;
  }

  private void setupEditor(List<T> possibleValues, final JTable table) {
    myComboBox = new JComboBox();
    for (T value : possibleValues) {
      myComboBox.addItem(new ComboItem(value));
    }
    myComboBox.setEditable(true);
    // AutoCompleteDecorator.decorate(myComboBox);
    TableColumn column = table.getColumnModel().getColumn(0);
    column.setCellEditor(new DefaultCellEditor(myComboBox));
  }

  protected String getStringValue(T t) {
    return String.valueOf(t);
  }

  protected boolean isEditable(T t) {
    return true;
  }

  protected abstract T updateValue(T newValue, T curValue);

  protected abstract T createValue(T prototype);

  protected abstract void deleteValue(T value);

  protected void reloadValues() {
  }

  protected void applyValues() {
  }

  class TableAndActionsImpl extends AbstractTableAndActionsComponent<T> {
    TableAndActionsImpl() {
      super(resourcesTable);
    }

    @Override
    protected void onAddEvent() {
      int lastRow = resourcesTable.getRowCount() - 1;
      if (myComboBox != null) {
        resourcesTable.setValueAt(myComboBox.getItemAt(0), lastRow, 0);
        myComboBox.requestFocus();
      } else {
        // resourcesTable.setValueAt("<column name>", lastRow, 0);
      }

      Rectangle cellRect = resourcesTable.getCellRect(lastRow, 0, true);
      resourcesTable.scrollRectToVisible(cellRect);
      resourcesTable.getSelectionModel().setSelectionInterval(lastRow, lastRow);
      resourcesTable.editCellAt(lastRow, 0);
      resourcesTable.getEditorComponent().requestFocus();

    }

    @Override
    protected void onDeleteEvent() {
      for (int selectedRow : mySelectedRows) {
        resourcesTable.getModel().setValueAt(null, selectedRow, 0);
      }
    }

    @Override
    protected void onSelectionChanged() {
      mySelectedRows = resourcesTable.getSelectedRows();
      List<T> selectedObjects = getSelectedObjects();
      fireSelectionChanged(selectedObjects);
    }

  }
}
