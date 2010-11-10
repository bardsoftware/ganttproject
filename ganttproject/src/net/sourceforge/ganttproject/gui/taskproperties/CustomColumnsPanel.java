/* LICENSE: GPL2
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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.GanttDialogCustomColumn;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

import org.jdesktop.jdnc.JNTable;
import org.jdesktop.swing.decorator.AlternateRowHighlighter;
import org.jdesktop.swing.decorator.Highlighter;
import org.jdesktop.swing.decorator.HighlighterPipeline;

/**
 * This class implements a UI component for editing custom properties.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CustomColumnsPanel extends JPanel {
    private static GanttLanguage language = GanttLanguage.getInstance();

    private final CustomPropertyManager myCustomPropertyManager;

    private final UIFacade myUIfacade;

    private CustomColumnTableModel model;

    private JNTable table;

    public CustomColumnsPanel(CustomPropertyManager manager, UIFacade uifacade) {
    	assert manager != null;
        myCustomPropertyManager = manager;
        myUIfacade = uifacade;
        this.initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        model = new CustomColumnTableModel();
        table = new JNTable(model);
        table.setPreferredVisibleRowCount(10);
        table.setHighlighters(new HighlighterPipeline(new Highlighter[] {
                AlternateRowHighlighter.floralWhite,
                AlternateRowHighlighter.quickSilver }));
        table.getTable().setSortable(false);
        AbstractTableAndActionsComponent<CustomPropertyDefinition> tableAndActions =
            new AbstractTableAndActionsComponent<CustomPropertyDefinition>(table.getTable()) {
                @Override
                protected void onAddEvent() {
                    myUIfacade.getUndoManager().undoableEdit(
                            "TaskPropertyNewColumn",
                            new Runnable() {
                                public void run() {
                                    CustomColumnsPanel.this.onAddEvent();
                                }
                            });
                }

                @Override
                protected void onDeleteEvent() {
                    CustomColumnsPanel.this.onDeleteEvent();
                }

                @Override
                protected void onSelectionChanged() {
                }
        };
        this.add(tableAndActions.getActionsComponent(), BorderLayout.NORTH);
        JScrollPane scrollPane = new JScrollPane(table);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    private void onAddEvent() {
        GanttDialogCustomColumn d = new GanttDialogCustomColumn(myUIfacade, myCustomPropertyManager);
        d.setVisible(true);
        if (d.isOk()) {
            model.reload();
        }
    }

    private void onDeleteEvent() {
        int selectedRowsIndexes[] = table.getTable().getSelectedRows();
        for (int i = 0; i < selectedRowsIndexes.length; i++) {
            String nameToDel = (String) model.getValueAt(selectedRowsIndexes[i], 0);
            CustomPropertyDefinition def = myCustomPropertyManager.getCustomPropertyDefinition(nameToDel);
            myCustomPropertyManager.deleteDefinition(def);
        }
        model.reload();
    }

    private static final String[] COLUMN_NAMES = new String[] {
        CustomColumnsPanel.language.getText("name"),
        CustomColumnsPanel.language.getText("typeClass"),
        CustomColumnsPanel.language.getText("default")
    };

    class CustomColumnTableModel extends DefaultTableModel {
        public CustomColumnTableModel() {
        }

        public void reload() {
            fireTableDataChanged();
        }

        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        public Class getColumnClass(int column) {
            return String.class;
        }

        public boolean isCellEditable(int row, int col) {
            return col != 1;
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public int getRowCount() {
            return myCustomPropertyManager.getDefinitions().size();
        }

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
                return def.getDefaultValueAsString();
            default:
                throw new IllegalStateException();
            }
        }

        public void setValueAt(Object o, int row, int col) {
            if (row < 0 || row >= myCustomPropertyManager.getDefinitions().size()) {
                return;
            }
            CustomPropertyDefinition def = myCustomPropertyManager.getDefinitions().get(row);
            switch (col) {
            case 0:
                String newName = (String)o;
                if (columnExists(newName)) {
                    return;
                }
                def.setName(newName);
                break;
            case 2:
                String newValue = (String)o;
                def.setDefaultValueAsString(newValue);
                break;
            default:
                throw new IllegalStateException();
            }
        }

        private boolean columnExists(String columnName) {
            for (CustomPropertyDefinition def : myCustomPropertyManager.getDefinitions()) {
                if (def.getName().equals(columnName)) {
                    return true;
                }
            }
            return false;
        }
    }
}
