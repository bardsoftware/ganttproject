/*
GanttProject is an opensource project management tool.
Copyright (C) 2009 Dmitry Barashev

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
package org.ganttproject.impex.htmlpdf.itext;

import net.sourceforge.ganttproject.gui.UIUtil;
import org.ganttproject.impex.htmlpdf.itext.FontSubstitutionModel.FontSubstitution;
import org.jdesktop.swingx.JXTable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;

public class FontSubstitutionPanel {

  private FontSubstitutionModel myModel;
  private JComboBox myFamiliesComboBox;
  private JLabel myMessage;

  public FontSubstitutionPanel(FontSubstitutionModel fontConfigurationModel) {
    myModel = fontConfigurationModel;
  }

  public Component getComponent() {
    TableModel tableModel = new AbstractTableModel() {
      @Override
      public int getColumnCount() {
        return 2;
      }

      @Override
      public int getRowCount() {
        return myModel.getSubstitutions().size();
      }

      @Override
      public Object getValueAt(int rowIndex, int columnIndex) {
        FontSubstitution substitution = getSubstitution(rowIndex);
        switch (columnIndex) {
          case 0:
            return substitution.myOriginalFamily;
          case 1:
            return substitution.getSubstitutionFamily();
          default:
            assert false;
            throw new IllegalStateException();
        }
      }

      @Override
      public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
      }

      @Override
      public void setValueAt(Object value, int rowIndex, int columnIndex) {
        assert columnIndex == 1;
        assert value instanceof String;
        getSubstitution(rowIndex).setSubstitutionFamily((String) value);
        updateFontStatusMessage();
      }

      @Override
      public String getColumnName(int column) {
        switch (column) {
          case 0:
            return "Theme font";
          case 1:
            return "Substitution";
          default:
            assert false;
            throw new IllegalStateException();
        }
      }
    };
    JTable table = new JXTable(tableModel);
    UIUtil.setupTableUI(table);

    class CellRendererImpl implements TableCellRenderer {
      private DefaultTableCellRenderer myDefaultRenderer = new DefaultTableCellRenderer();

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
        JLabel result = (JLabel) myDefaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus,
            row, column);
        FontSubstitution substitution = getSubstitution(row);
        if (!substitution.isResolved() || column == 0) {
          result.setFont(result.getFont().deriveFont(16f));
        } else {
          result.setFont(substitution.getSubstitutionFont().deriveFont(16f));
        }
        if (substitution.isResolved()) {
          result.setForeground(Color.BLACK);
        } else {
          result.setForeground(Color.RED);
        }
        return result;
      }
    }

    myFamiliesComboBox = new JComboBox(myModel.getAvailableSubstitutionFamilies().toArray(new String[0]));
    table.getColumnModel().getColumn(0).setCellRenderer(new CellRendererImpl());

    table.getColumnModel().getColumn(1).setCellRenderer(new CellRendererImpl());
    table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(myFamiliesComboBox));
    table.getTableHeader().setVisible(true);

    myMessage = new JLabel(getMessageText());
    myMessage.setAlignmentX(0);
    Box result = Box.createVerticalBox();
    result.add(myMessage);
    result.add(Box.createVerticalStrut(5));
    result.add(new JScrollPane(table));
    return result;
  }

  private void updateFontStatusMessage() {
    myMessage.setText(getMessageText());
  }

  private String getMessageText() {
    return myModel.hasUnresolvedFonts() ? "<html><p><b>Some fonts used in the selected theme have not been found</b></p>"
        + "<p>You may define substitutions in the table</p></html>"
        : "<html>All fonts have been found.</html>";
  }

  private FontSubstitution getSubstitution(int row) {
    return myModel.getSubstitution(row);
  }
}
