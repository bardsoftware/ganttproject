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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.action.GPAction;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * It is a UI component consisting of a table and a set of actions. Default
 * actions are "add" and "delete" which add a new row to and delete a row the
 * table. One may add other actions.
 *
 * Actions are represented as buttons and this class creates a UI panel holding
 * all actions.
 *
 * A few abstract methods are called when some events happen.
 *
 * @author dbarashev (Dmitry Barashev)
 *
 * @param <T>
 *          type of objects stored in the table
 */
public abstract class AbstractTableAndActionsComponent<T> {
  private static final int ENABLED_WITH_EMPTY_SELECTION = 1;
  private static final int DISABLED_WITH_MULTI_SELECTION = 2;
  public static final String PROPERTY_IS_ENABLED_FUNCTION = AbstractTableAndActionsComponent.class.getName() + ".isEnabledFunction";

  private Function<List<T>, Boolean> createIsEnabledFunction(final int flags) {
    return new Function<List<T>, Boolean>() {
      @Override
      public Boolean apply(List<T> input) {
        if (input.isEmpty()) {
          return (0 != (flags & AbstractTableAndActionsComponent.ENABLED_WITH_EMPTY_SELECTION));
        } else if (input.size() > 1) {
          return (0 == (flags & AbstractTableAndActionsComponent.DISABLED_WITH_MULTI_SELECTION));
        } else {
          return true;
        }
      }
    };
  }

  private int myActionOrientation = SwingConstants.HORIZONTAL;

  private final List<Action> myAdditionalActions = new ArrayList<>();
  private final List<SelectionListener<T>> myListeners = new ArrayList<>();
  private final JTable myTable;
  private JPanel buttonBox;
  private final Action myDeleteAction = new GPAction("delete") {
    @Override
    public void actionPerformed(ActionEvent e) {
      int selectedRow = myTable.getSelectedRow();
      onDeleteEvent();
      if (selectedRow >= myTable.getRowCount()) {
        selectedRow = myTable.getRowCount() - 1;
      }
      myTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
    }
  };

  protected AbstractTableAndActionsComponent(JTable table) {
    myTable = table;
    addAction(getAddResourceAction(), AbstractTableAndActionsComponent.ENABLED_WITH_EMPTY_SELECTION);
    addAction(getDeleteItemAction());
    myTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        onSelectionChanged();
      }
    });
    myTable.getModel().addTableModelListener(new TableModelListener() {
      @Override
      public void tableChanged(TableModelEvent e) {
        onSelectionChanged();
      }
    });
  }

  public void addAction(Action action) {
    addAction(action, 0);
  }

  private void addAction(Action action, int flags) {
    if (action.getValue(PROPERTY_IS_ENABLED_FUNCTION) == null) {
      action.putValue(PROPERTY_IS_ENABLED_FUNCTION, createIsEnabledFunction(flags));
    }
    myAdditionalActions.add(action);

    if (action instanceof SelectionListener) {
      addSelectionListener((SelectionListener<T>) action);
    }
  }

  public void setActionOrientation(int orientation) {
    assert orientation == SwingConstants.VERTICAL || orientation == SwingConstants.HORIZONTAL;
    myActionOrientation = orientation;
  }

  public void setSelectionMode(int selectionMode) {
    myTable.getSelectionModel().setSelectionMode(selectionMode);
  }

  private Action getAddResourceAction() {
    return new GPAction("add") {
      @Override
      public void actionPerformed(ActionEvent e) {
        onAddEvent();
      }
    };
  }

  public Action getDeleteItemAction() {
    return myDeleteAction;
  }

  public void setSelection(int index) {
    if (index == -1) {
      myTable.getSelectionModel().clearSelection();
    } else {
      myTable.getSelectionModel().setSelectionInterval(index, index);
    }
  }

  protected void fireSelectionChanged(List<T> selectedObjects) {
    for (Action action : myAdditionalActions) {
      Function<List<T>, Boolean> isEnabled = (Function<List<T>, Boolean>) action.getValue(PROPERTY_IS_ENABLED_FUNCTION);
      action.setEnabled(isEnabled.apply(selectedObjects));
    }
    for (SelectionListener<T> l : myListeners) {
      l.selectionChanged(selectedObjects);
    }
  }

  protected void onSelectionChanged() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        int[] selectedRows = myTable.getSelectedRows();
        List<T> result = Lists.newArrayList();
        for (int row : selectedRows) {
          T value = getValue(row);
          if (value != null) {
            result.add(getValue(row));
          }
        }
        fireSelectionChanged(result);
      }
    });
  }

  public JComponent getActionsComponent() {
    if (buttonBox == null) {
      buttonBox = myActionOrientation == SwingConstants.HORIZONTAL ? new JPanel(new GridLayout(1,
          myAdditionalActions.size(), 5, 0)) : new JPanel(new GridLayout(myAdditionalActions.size(), 1, 0, 5));
      for (Action action : myAdditionalActions) {
        buttonBox.add(new JButton(action));
      }
      Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
      buttonBox.setBorder(emptyBorder);
    }
    onSelectionChanged();
    return buttonBox;
  }

  public interface SelectionListener<T> {
    void selectionChanged(List<T> selection);
  }

  public void addSelectionListener(SelectionListener<T> listener) {
    myListeners.add(listener);
  }

  protected abstract void onAddEvent();

  protected abstract void onDeleteEvent();

  protected T getValue(int row) {
    return null;
  }

  public static JPanel createDefaultTableAndActions(JComponent table, JComponent actionsComponent) {
    JPanel result = new JPanel(new BorderLayout());
    actionsComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
    JPanel actionsWrapper = new JPanel(new BorderLayout());
    actionsWrapper.add(actionsComponent, BorderLayout.WEST);
    result.add(actionsWrapper, BorderLayout.NORTH);
    JScrollPane scrollPane = new JScrollPane(table);
    result.add(scrollPane, BorderLayout.CENTER);
    return result;
  }

  public JTable getTable() {
    return myTable;
  }

}
