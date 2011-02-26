/*
GanttProject is an opensource project management tool.
Copyright (C) 2010-2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.ganttproject.action.GPAction;

/**
 * It is a UI component consisting of a table and a set of actions. Default actions
 * are "add" and "delete" which add a new row to and delete a row the table. One may
 * add other actions.
 *
 * Actions are represented as buttons and this class creates a UI panel holding
 * all actions.
 *
 * A few abstract methods are called when some events happen.
 *
 * @author dbarashev (Dmitry Barashev)
 *
 * @param <T> type of objects stored in the table
 */
public abstract class AbstractTableAndActionsComponent<T> {
    public static final int ENABLED_WITH_EMPTY_SELECTION = 1;
    public static final int DISABLED_WITH_MULTI_SELECTION = 2;
    private int myActionOrientation = SwingConstants.HORIZONTAL;

    private static class InternalAction<T> {
        final Action action;
        final int flags;
        InternalAction(Action action, int flags) {
            this.action = action;
            this.flags = flags;
        }

        public void updateEnabledState(List<T> selectedObjects) {
            if (selectedObjects.isEmpty()) {
                action.setEnabled(0 != (flags & AbstractTableAndActionsComponent.ENABLED_WITH_EMPTY_SELECTION));
            } else if (selectedObjects.size() > 1) {
                action.setEnabled(0 == (flags & AbstractTableAndActionsComponent.DISABLED_WITH_MULTI_SELECTION));
            } else {
                action.setEnabled(true);
            }
        }
    }
    private final List<InternalAction<T>> myAdditionalActions = new ArrayList<InternalAction<T>>();
    private final List<SelectionListener<T>> myListeners = new ArrayList<SelectionListener<T>>();
    private final JTable myTable;
    private Box buttonBox;

    protected AbstractTableAndActionsComponent(JTable table) {
        myTable = table;
        addAction(getAddResourceAction(), AbstractTableAndActionsComponent.ENABLED_WITH_EMPTY_SELECTION);
        addAction(getDeleteResourceAction());
        myTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        myTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                onSelectionChanged();
            }
        });
    }

    public void addAction(Action action) {
        addAction(action, 0);
    }

    public void addAction(Action action, int flags) {
        myAdditionalActions.add(new InternalAction<T>(action, flags));
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
            protected String getLocalizedName() {
                return getI18n("add");
            }
            @Override
            protected String getIconFilePrefix() {
                return null;
            }
            public void actionPerformed(ActionEvent e) {
                onAddEvent();
            }
        };
    };

    private Action getDeleteResourceAction() {
        return new GPAction("delete") {
            @Override
            protected String getLocalizedName() {
                return getI18n("delete");
            }
            @Override
            protected String getIconFilePrefix() {
                return null;
            }
            public void actionPerformed(ActionEvent e) {
                onDeleteEvent();
            }
        };
    }

    public void setSelection(int index) {
        if (index == -1) {
            myTable.getSelectionModel().clearSelection();
        } else {
            myTable.getSelectionModel().setSelectionInterval(index, index);
        }
    }
    protected void fireSelectionChanged(List<T> selectedObjects) {
        for (SelectionListener<T> l : myListeners) {
            l.selectionChanged(selectedObjects);
        }
        for (InternalAction<T> internalAction : myAdditionalActions) {
            internalAction.updateEnabledState(selectedObjects);
        }
    }

    public JComponent getActionsComponent() {
        if (buttonBox == null) {
            buttonBox = myActionOrientation == SwingConstants.HORIZONTAL ? Box.createHorizontalBox() : Box.createVerticalBox();
            Component strut = null;
            for (InternalAction<T> internalAction: myAdditionalActions) {
                if (strut != null) {
                    buttonBox.add(strut);
                }
                buttonBox.add(new JButton(internalAction.action));
                strut = myActionOrientation == SwingConstants.HORIZONTAL ? Box.createHorizontalStrut(5) : Box.createVerticalStrut(5);
            }
            Border emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 0);
            buttonBox.setBorder(emptyBorder);
        }
        onSelectionChanged();
        return buttonBox;
    }

    public static interface SelectionListener<T> {
        void selectionChanged(List<T> selection);
    }

    public void addSelectionListener(SelectionListener<T> listener) {
        myListeners.add(listener);
    }

    protected abstract void onAddEvent();
    protected abstract void onDeleteEvent();
    protected abstract void onSelectionChanged();
}
