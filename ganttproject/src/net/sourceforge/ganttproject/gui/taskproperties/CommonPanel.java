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
package net.sourceforge.ganttproject.gui.taskproperties;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.TableColumn;

import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.UIUtil;

import org.jdesktop.swingx.JXTable;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public abstract class CommonPanel {
  static void setupTableUI(JXTable table) {
    UIUtil.setupTableUI(table, 10);
    UIUtil.setupHighlighters(table);
  }

  public static void setupComboBoxEditor(TableColumn column, Object[] values) {
    DefaultComboBoxModel model = new DefaultComboBoxModel(values);
    JComboBox comboBox = new JComboBox(model);
    comboBox.setEditable(false);
    column.setCellEditor(new DefaultCellEditor(comboBox));
    if (values.length > 1) {
      comboBox.setSelectedIndex(0);
    }
  }

  static JPanel createTableAndActions(JComponent table, JComponent actionsComponent) {
    return AbstractTableAndActionsComponent.createDefaultTableAndActions(table, actionsComponent);
  }
}
