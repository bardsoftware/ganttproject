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
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import net.sourceforge.ganttproject.CustomProperty;
import net.sourceforge.ganttproject.CustomPropertyDefinition;
import net.sourceforge.ganttproject.CustomPropertyHolder;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.ShowHideColumnsDialog;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * This class implements a UI component for editing custom properties.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CustomColumnsPanel {
    private static GanttLanguage language = GanttLanguage.getInstance();

    private final CustomPropertyManager myCustomPropertyManager;

    private final UIFacade myUiFacade;

    private CustomColumnTableModel model;

    private JTable table;

    private CustomPropertyHolder myHolder;

    private TableHeaderUIFacade myTableHeaderFacade;

    public CustomColumnsPanel(CustomPropertyManager manager, UIFacade uifacade, CustomPropertyHolder customPropertyHolder, TableHeaderUIFacade tableHeaderFacade) {
        assert manager != null;
        myCustomPropertyManager = manager;
        myUiFacade = uifacade;
        myHolder = customPropertyHolder;
        myTableHeaderFacade = tableHeaderFacade;
    }

    public JComponent geComponent() {
        model = new CustomColumnTableModel();
        table = new JTable(model);

        CommonPanel.setupTableUI(table);
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(new JButton(new GPAction("columns.manage.label") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ShowHideColumnsDialog dialog = new ShowHideColumnsDialog(
                        myUiFacade, myTableHeaderFacade, myCustomPropertyManager);
                dialog.show();
                model.fireTableStructureChanged();
            }
          }), BorderLayout.WEST);
        return CommonPanel.createTableAndActions(table, buttonPanel);
    }

    private static final String[] COLUMN_NAMES = new String[] {
        CustomColumnsPanel.language.getText("name"),
        CustomColumnsPanel.language.getText("typeClass"),
        CustomColumnsPanel.language.getText("value")
    };

    class CustomColumnTableModel extends DefaultTableModel {
        public CustomColumnTableModel() {
        }

        public void reload() {
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 2;
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public int getRowCount() {
            return myCustomPropertyManager.getDefinitions().size();
        }

        @Override
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
                for (CustomProperty cp : myHolder.getCustomProperties()) {
                    if (cp.getDefinition() == def) {
                        return cp.getValueAsString();
                    }
                }
                return def.getDefaultValue() + " (default)";
            default:
                throw new IllegalStateException();
            }
        }

        @Override
        public void setValueAt(Object o, int row, int col) {
            if (row < 0 || row >= myCustomPropertyManager.getDefinitions().size()) {
                return;
            }
            if (col != 2) {
                throw new IllegalArgumentException();
            }
            CustomPropertyDefinition def = myCustomPropertyManager.getDefinitions().get(row);
            myHolder.addCustomProperty(def, String.valueOf(o));
        }
    }
}
