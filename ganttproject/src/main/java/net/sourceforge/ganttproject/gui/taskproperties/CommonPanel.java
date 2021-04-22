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

import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.TableModelExt;
import net.sourceforge.ganttproject.gui.UIUtil;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.util.Map;

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

  static JPanel createTableAndActions(JTable table, TableModelExt model) {
    return AbstractTableAndActionsComponent.createDefaultTableAndActions(table, model);
  }

  public static void saveColumnWidths(JTable table, Map<Integer, Integer> column2pctgsWidth) {
    int totalWidth = table.getColumnModel().getTotalColumnWidth();
    table.getColumnModel().getColumns().asIterator().forEachRemaining(column ->
        column2pctgsWidth.put(column.getModelIndex(), column.getWidth()*100 / totalWidth)
    );
  }

  public static void loadColumnWidth(JTable table, Map<Integer, Integer> column2pctgWidth) {
    int totalWidth = table.getColumnModel().getTotalColumnWidth();
    table.getColumnModel().getColumns().asIterator().forEachRemaining(column -> {
      int columnPercentage = column2pctgWidth.getOrDefault(
          column.getModelIndex(), 100/table.getColumnModel().getColumnCount());
      column.setPreferredWidth(totalWidth*columnPercentage/100);
    });
  }
}
