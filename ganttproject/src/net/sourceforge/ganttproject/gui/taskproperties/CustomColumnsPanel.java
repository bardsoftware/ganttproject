package net.sourceforge.ganttproject.gui.taskproperties;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import net.sourceforge.ganttproject.gui.GanttDialogCustomColumn;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.CustomColumnsStorage;

import org.jdesktop.jdnc.JNTable;
import org.jdesktop.swing.decorator.AlternateRowHighlighter;
import org.jdesktop.swing.decorator.Highlighter;
import org.jdesktop.swing.decorator.HighlighterPipeline;

/**
 * @author bbaranne Mar 4, 2005
 */
public class CustomColumnsPanel extends JPanel {
    private static GanttLanguage language = GanttLanguage.getInstance();

    private final CustomColumnsStorage customColumnStorage;

    private Vector titles = null;

    private JNTable table = null;

    private JButton buttonAdd = null;

    private JButton buttonDel = null;

	private final CustomColumnsManager myColumnManager;

	private final UIFacade myUIfacade;

    public CustomColumnsPanel(CustomColumnsManager manager, CustomColumnsStorage customColHandler, UIFacade uifacade) {
    	myColumnManager = manager;
        customColumnStorage = customColHandler;
        myUIfacade = uifacade;
        this.initComponents();
    }

    private void initComponents() {
        this.titles = new Vector(3);
        this.titles.add(language.getText("name"));
        this.titles.add(language.getText("typeClass"));
        this.titles.add(language.getText("default"));

        final CustomColumnTableModel model = new CustomColumnTableModel(
                customColumnStorage);
        table = new JNTable(model);
        table.setPreferredVisibleRowCount(10);
        this.add(table);
        table.getColumn((String) titles.get(0)).setPreferredWidth(150);
        table.getColumn((String) titles.get(1)).setPreferredWidth(150);
        table.getColumn((String) titles.get(2)).setPreferredWidth(200);

        table.setHighlighters(new HighlighterPipeline(new Highlighter[] {
                AlternateRowHighlighter.floralWhite,
                AlternateRowHighlighter.quickSilver }));

        table.getTable().setSortable(false);

        buttonAdd = new JButton(language.getText("add"));
        buttonAdd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                myUIfacade.getUndoManager().undoableEdit("TaskPropertyNewColumn",
                        new Runnable() {
                            public void run() {
                                CustomColumn customColumn = new CustomColumn();
                                GanttDialogCustomColumn d = new GanttDialogCustomColumn(
                                		myUIfacade, customColumn);
                                d.setVisible(true);
                                if (d.isOk()) {
	                                myColumnManager.addNewCustomColumn(customColumn);
	                                model.refreshData(customColumnStorage);
	                                repaint();
                                }
                            }
                        });
            }
        });
        this.add(buttonAdd, BorderLayout.EAST);

        buttonDel = new JButton(language.getText("delete"));
        buttonDel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedRowsIndexes[] = table.getTable().getSelectedRows();
                for (int i = 0; i < selectedRowsIndexes.length; i++) {
                    String nameToDel = (String) model.getValueAt(
                            selectedRowsIndexes[i], 0);
                    myColumnManager.deleteCustomColumn(
                            nameToDel);
                }
                model.refreshData(customColumnStorage);
                repaint();
            }
        });
        this.add(buttonDel, BorderLayout.EAST);

    }

    /*
     * ----- INNER CLASSES -----
     */

    class CustomColumnTableModel extends DefaultTableModel {
        CustomColumnsStorage customColumnsHandler = null;

        public CustomColumnTableModel(CustomColumnsStorage ccHandler) {
            customColumnsHandler = ccHandler;
            this.columnIdentifiers = titles;

            this.dataVector = new Vector();

            Iterator it = customColumnsHandler.getCustomColums().iterator();
            while (it.hasNext()) {
                CustomColumn cc = (CustomColumn) it.next();
                Object[] o = { cc.getName(), cc.getType(), cc.getDefaultValue() };
                this.addRow(o);
            }
        }

        public void refreshData(CustomColumnsStorage cch) {
            customColumnsHandler = cch;
            this.dataVector = new Vector();

            Iterator it = customColumnsHandler.getCustomColums().iterator();
            while (it.hasNext()) {
                CustomColumn cc = (CustomColumn) it.next();
                Object[] o = { cc.getName(), cc.getType(), cc.getDefaultValue() };
                this.addRow(o);
            }
        }

        public String getColumnName(int column) {
            return (String) columnIdentifiers.get(column);
        }

        public Class getColumnClass(int column) {
            return String.class;
        }

        public boolean isCellEditable(int row, int col) {
            return col != 1;
        }

        public Object getValueAt(int row, int col) {
            String colName = (String) ((Vector) this.dataVector.get(row))
                    .get(0);
            CustomColumn res = customColumnsHandler.getCustomColumn(colName);
            if (res != null) {
                switch (col) {
                case 0:
                    return res.getName();
                case 1: {
                    Class cl = res.getType();
                    if (cl.equals(String.class))
                        return language.getText("text");
                    if (GregorianCalendar.class.isAssignableFrom(cl))
                        return language.getText("date");
                    if (cl.equals(Boolean.class))
                        return language.getText("boolean");
                    if (cl.equals(Integer.class))
                        return language.getText("integer");
                    if (cl.equals(Double.class))
                        return language.getText("double");
                }
                case 2:
                    return res.getDefaultValue();
                }
            }
            return null;
        }

        public void setValueAt(Object o, int row, int col) {

            String oldName;
            Vector v = (Vector) dataVector.get(row);
            oldName = (String) v.get(0);

            if (col == 0) {
                if (!((String) o).equals(oldName)) {
                    if (!customColumnStorage.exists((String) o)) {
                        v.setElementAt(o, col);
                        myColumnManager
                                .changeCustomColumnName(oldName, (String) o);
                    }

                }
            }
            if (col == 2) {

                try {
                    myColumnManager
                            .changeCustomColumnDefaultValue(oldName, o);
                    v.setElementAt(o, col);
                } catch (CustomColumnsException e) {
                	myUIfacade.showErrorDialog(e);
                }
            }
        }
    }
}
