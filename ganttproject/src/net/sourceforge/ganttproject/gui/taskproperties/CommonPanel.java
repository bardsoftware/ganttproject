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

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.table.TableColumn;

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
        comboBox.setSelectedIndex(0);
        comboBox.setEditable(false);
        column.setCellEditor(new DefaultCellEditor(comboBox));
	}
}
