/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.taskproperties;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;

import org.jdesktop.jdnc.JNTable;
import org.jdesktop.swing.decorator.AlternateRowHighlighter;
import org.jdesktop.swing.decorator.Highlighter;
import org.jdesktop.swing.decorator.HighlighterPipeline;

/**
 * @author dbarashev (Dmitry Barashev)
 */
abstract class CommonPanel {
    static void setupTableUI(JNTable table) {
        table.setPreferredVisibleRowCount(10);
        table.setHighlighters(new HighlighterPipeline(new Highlighter[] {
                AlternateRowHighlighter.floralWhite,
                AlternateRowHighlighter.quickSilver }));
        table.getTable().setSortable(false);
    }

    static void setupComboBoxEditor(TableColumn column, Object[] values) {
        DefaultComboBoxModel model = new DefaultComboBoxModel(values);
        JComboBox comboBox = new JComboBox(model);
        comboBox.setEditable(false);
        column.setCellEditor(new DefaultCellEditor(comboBox));
        if (values.length > 1) {
            comboBox.setSelectedIndex(0);
        }
    }

    static void setupTableUI(JTable table) {
        table.setPreferredScrollableViewportSize(new Dimension(
                table.getPreferredScrollableViewportSize().width,
                table.getRowHeight() * 10));
    }

    static JPanel createTableAndActions(JComponent table, JComponent actionsComponent) {
        JPanel result = new JPanel(new BorderLayout());
        actionsComponent.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));
        JPanel actionsWrapper = new JPanel(new BorderLayout());
        actionsWrapper.add(actionsComponent, BorderLayout.WEST);
        result.add(actionsWrapper, BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(table);
        result.add(scrollPane, BorderLayout.CENTER);
        return result;
    }
}
