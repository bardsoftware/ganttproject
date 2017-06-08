/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject;

import biz.ganttproject.core.option.ValidationException;
import net.sourceforge.ganttproject.gui.UIUtil;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

class TreeTableCellEditorImpl implements TableCellEditor {
  private final DefaultCellEditor myProxiedEditor;
  private final JTable myTable;

  TreeTableCellEditorImpl(DefaultCellEditor proxiedEditor, JTable table) {
    assert proxiedEditor != null;
    myProxiedEditor = proxiedEditor;
    myTable = table;
  }

  @Override
  public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
    final Component result = myProxiedEditor.getTableCellEditorComponent(arg0, arg1, arg2, arg3, arg4);
    if (result instanceof JTextComponent) {
      ((JTextComponent) result).selectAll();
      //myFocusCommand = createSelectAllCommand((JTextComponent)result);
    }
    return result;
  }

  @Override
  public Object getCellEditorValue() {
    return myProxiedEditor.getCellEditorValue();
  }

  @Override
  public boolean isCellEditable(EventObject event) {
    if (event instanceof MouseEvent) {
      MouseEvent mouseEvent = (MouseEvent) event;
      if (mouseEvent.getClickCount() == 2) {
        return false;
      }
      if (mouseEvent.getClickCount() == 1
          && myTable.rowAtPoint(mouseEvent.getPoint()) == myTable.getSelectedRow()
          && myTable.columnAtPoint(mouseEvent.getPoint()) == myTable.getSelectedColumn()) {
        return myProxiedEditor.isCellEditable(null);
      }
    }
    return myProxiedEditor.isCellEditable(event);
  }

  @Override
  public boolean shouldSelectCell(EventObject arg0) {
    return myProxiedEditor.shouldSelectCell(arg0);
  }

  @Override
  public boolean stopCellEditing() {
    try {
      return myProxiedEditor.stopCellEditing();
    } catch (ValidationException e) {
      GPLogger.log(e.getMessage());
      myProxiedEditor.getComponent().setBackground(UIUtil.INVALID_VALUE_BACKGROUND);
      return false;
    }
  }

  @Override
  public void cancelCellEditing() {
    myProxiedEditor.cancelCellEditing();
  }

  @Override
  public void addCellEditorListener(CellEditorListener arg0) {
    myProxiedEditor.addCellEditorListener(arg0);
  }

  @Override
  public void removeCellEditorListener(CellEditorListener arg0) {
    myProxiedEditor.removeCellEditorListener(arg0);
  }

  static Runnable createSelectAllCommand(final JTextComponent textComponent) {
    return createOnFocusGained(textComponent, new Runnable() {
      @Override
      public void run() {
        textComponent.selectAll();
      }
    });
  }

  public static Runnable createUnselectAllCommand(final JTextComponent textComponent) {
    return createOnFocusGained(textComponent, new Runnable() {
      @Override
      public void run() {
        textComponent.select(Integer.MAX_VALUE, Integer.MAX_VALUE);
      }
    });
  }

  private static Runnable createOnFocusGained(final JTextComponent textComponent, final Runnable onFocusGained) {
    final FocusListener focusListener = new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent arg0) {
        super.focusGained(arg0);
        SwingUtilities.invokeLater(onFocusGained);
        textComponent.removeFocusListener(this);
      }
    };
    textComponent.addFocusListener(focusListener);
    return new Runnable() {
      @Override
      public void run() {
        textComponent.requestFocus();
      }
    };
  }
}
