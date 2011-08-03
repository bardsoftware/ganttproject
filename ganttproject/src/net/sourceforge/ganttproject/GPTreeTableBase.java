/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomColumn;

import org.jdesktop.jdnc.JNTreeTable;
import org.jdesktop.swing.JXTreeTable;
import org.jdesktop.swing.table.TableColumnExt;
import org.jdesktop.swing.treetable.DefaultTreeTableModel;
import org.jdesktop.swing.treetable.TreeTableModel;

public class GPTreeTableBase extends JNTreeTable{
    private final IGanttProject myProject;
    private UIFacade myUiFacade;
    private final TableHeaderUiFacadeImpl myTableHeaderFacade = new TableHeaderUiFacadeImpl();

    protected class TableHeaderUiFacadeImpl implements TableHeaderUIFacade {
        private final List<ColumnImpl> myColumns = new ArrayList<ColumnImpl>();
        @Override
        public int getSize() {
            return myColumns.size();
        }
        @Override
        public Column getField(int index) {
            return myColumns.get(index);
        }
        @Override
        public void clear() {
        }
        @Override
        public void add(String name, int order, int width) {
        }
        @Override
        public void importData(TableHeaderUIFacade source) {
        }

        protected void createDefaultColumns(List<TableHeaderUIFacade.Column> stubs) {
            for (int i = 0; i < stubs.size(); i++) {
                createColumn(i, stubs.get(i));
            }
            Collections.sort(myColumns, new Comparator<ColumnImpl>() {
                @Override
                public int compare(ColumnImpl left, ColumnImpl right) {
                    if (!left.getStub().isVisible() && !right.getStub().isVisible()) {
                        return left.getName().compareTo(right.getName());
                    }
                    return left.getStub().getOrder() - right.getStub().getOrder();
                }
            });
            for (ColumnImpl column : myColumns) {
                if (column.getStub().isVisible()) {
                    insertColumnIntoUi(column);
                }
            }
        }

        protected ColumnImpl createColumn(int modelIndex, TableHeaderUIFacade.Column stub) {
            TableColumnExt tableColumn = newTableColumnExt(modelIndex);
            tableColumn.setPreferredWidth(stub.getWidth());
            ColumnImpl result = new ColumnImpl(getTreeTable(), tableColumn, stub);
            myColumns.add(result);
            return result;
        }

        protected void insertColumnIntoUi(ColumnImpl column) {
            getTable().addColumn(column.myTableColumn);
            int align = getTable().getModel().getColumnClass(column.myTableColumn.getModelIndex()).equals(GregorianCalendar.class)
                ? SwingConstants.RIGHT : SwingConstants.CENTER;
            setColumnHorizontalAlignment(column.getName(), align);
        }

        protected void clearColumns() {
            List<TableColumn> columns = Collections.list(getTable().getColumnModel().getColumns());
            for (int i = 0; i < columns.size(); i++) {
                getTable().removeColumn(columns.get(i));
            }
            myColumns.clear();
        }

        protected void renameColumn(CustomPropertyDefinition definition) {
            ColumnImpl c = findColumnByID(definition.getID());
            if (c == null) {
                return;
            }
            c.setName(definition.getName());
        }

        protected void deleteColumn(CustomPropertyDefinition definition) {
            ColumnImpl c = findColumnByID(definition.getID());
            if (c == null) {
                return;
            }
            getTable().removeColumn(c.myTableColumn);
            myColumns.remove(c);
            for (ColumnImpl column : myColumns) {
                if (column.myTableColumn.getModelIndex() > c.myTableColumn.getModelIndex()) {
                    column.myTableColumn.setModelIndex(column.myTableColumn.getModelIndex() - 1);
                }
            }
        }

        protected ColumnImpl findColumnByID(String id) {
            for (ColumnImpl c : myColumns) {
                if (c.getID().equals(id)) {
                    return c;
                }
            }
            return null;
        }

        protected ColumnImpl findColumnByViewIndex(int index) {
            for (ColumnImpl c : myColumns) {
                if (c.getOrder() == index) {
                    return c;
                }
            }
            return null;
        }

    }
    protected static class ColumnImpl implements TableHeaderUIFacade.Column {
        private final JXTreeTable myTable;
        private final TableColumnExt myTableColumn;
        private final Column myStub;

        protected ColumnImpl(JXTreeTable table, TableColumnExt tableColumn, TableHeaderUIFacade.Column stub) {
            myTable = table;
            myTableColumn = tableColumn;
            myStub = stub;
        }

        private TreeTableModel getTableModel() {
            return myTable.getTreeTableModel();
        }

        @Override
        public String getID() {
            return myStub.getID();
        }
        @Override
        public String getName() {
            return getTableModel().getColumnName(myTableColumn.getModelIndex());
        }

        private void setName(String name) {
            myTableColumn.setTitle(name);
        }

        @Override
        public int getOrder() {
            return myTable.convertColumnIndexToView(myTableColumn.getModelIndex());
        }
        @Override
        public int getWidth() {
            return myTableColumn.getWidth();
        }
        @Override
        public boolean isVisible() {
            return getOrder() >= 0;
        }
        @Override
        public void setVisible(boolean visible) {
            if (visible && !isVisible()) {
                myTable.addColumn(myTableColumn);
            } else if (!visible && isVisible()) {
                myTable.getColumnModel().removeColumn(myTableColumn);
            }
        }

        Column getStub() {
            return myStub;
        }
    }

    protected IGanttProject getProject() {
        return myProject;
    }

    protected GPTreeTableBase(IGanttProject project, UIFacade uiFacade, DefaultTreeTableModel model) {
        super(new JXTreeTable(model) {
            protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
                if (e.isAltDown() || e.isControlDown()) {
                    putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
                }
                boolean result = super.processKeyBinding(ks, e, condition, pressed);
                putClientProperty("JTable.autoStartsEdit", Boolean.TRUE);
                return result;
            }

        });
        myUiFacade = uiFacade;
        myProject = project;
        getTable().getTableHeader().addMouseListener(new HeaderMouseListener(project.getTaskCustomColumnManager()));
        getTable().getColumnModel().addColumnModelListener(
            new TableColumnModelListener() {
                public void columnMoved(TableColumnModelEvent e) {
                    if (e.getFromIndex() != e.getToIndex()) {
                        myProject.setModified();
                    }
                }
                public void columnAdded(TableColumnModelEvent e) {
                    myProject.setModified();
                }
                public void columnRemoved(TableColumnModelEvent e) {
                    myProject.setModified();
                }
                public void columnMarginChanged(ChangeEvent e) {
                    myProject.setModified();
                }
                public void columnSelectionChanged(ListSelectionEvent e) {
                }
            });

    }

    public TableHeaderUIFacade getVisibleFields() {
        return getTableHeaderUiFacade();
    }

    protected TableHeaderUiFacadeImpl getTableHeaderUiFacade() {
        return myTableHeaderFacade;
    }

    protected TableColumnExt newTableColumnExt(int modelIndex, CustomColumn customColumn) {
        TableColumnExt result = new TableColumnExt(modelIndex);
        TableCellEditor defaultEditor = getTreeTable().getDefaultEditor(customColumn.getType());
        if (defaultEditor!=null) {
            result.setCellEditor(new TreeTableCellEditorImpl(defaultEditor));
        }
        return result;
    }

    protected TableColumnExt newTableColumnExt(int modelIndex) {
        TableColumnExt result = new TableColumnExt(modelIndex);
        Class columnClass = getTreeTableModel().getColumnClass(modelIndex);
        TableCellEditor editor = columnClass.equals(GregorianCalendar.class)
            ? newDateCellEditor() : getTreeTable().getDefaultEditor(columnClass);
        if (editor!=null) {
            result.setCellEditor(new TreeTableCellEditorImpl(editor));
        }
        return result;
    }

    protected TableCellEditor newDateCellEditor() {
        return new DateCellEditor() {
            protected Date parseDate(String dateString) {
                DateFormat[] formats = new DateFormat[] {
                        GanttLanguage.getInstance().getLongDateFormat(),
                        GanttLanguage.getInstance().getMediumDateFormat(),
                        GanttLanguage.getInstance().getShortDateFormat(),
                };
                for (int i=0; i<formats.length; i++) {
                    try {
                        Date typedDate = formats[i].parse(dateString);
                        Calendar typedCal = CalendarFactory.newCalendar();
                        typedCal.setTime(typedDate);
                        Calendar projectStartCal = CalendarFactory.newCalendar();
                        projectStartCal.setTime(myProject.getTaskManager().getProjectStart());
                        int yearDiff = Math.abs(typedCal.get(Calendar.YEAR) - projectStartCal.get(Calendar.YEAR));
                        if (yearDiff > 1500) {
                            AttributedCharacterIterator iter = formats[i].formatToCharacterIterator(typedDate);
                            int additionalZeroes = -1;
                            StringBuffer result = new StringBuffer();
                            for (char c = iter.first(); c!=AttributedCharacterIterator.DONE; c = iter.next()) {
                                if (iter.getAttribute(DateFormat.Field.YEAR)!=null && additionalZeroes==-1) {
                                    additionalZeroes = iter.getRunLimit(DateFormat.Field.YEAR) - iter.getIndex();
                                    for (int j=0; j<additionalZeroes; j++) {
                                        result.append('0');
                                    }
                                }
                                result.append(c);
                            }
                            if (!result.toString().equals(dateString)) {
                                typedCal.add(Calendar.YEAR, 2000);
                                return typedCal.getTime();
                            }
                        }
                        return typedDate;
                    }
                    catch (ParseException e) {
                        if (i+1 == formats.length) {
                            return null;
                        }
                    }
                }
                return null;

            }
        };
    }

    public JScrollBar getVerticalScrollBar() {
        return scrollPane.getVerticalScrollBar();
    }

    public JScrollPane getScrollPane() {
        return scrollPane;
    }


    private static abstract class DateCellEditor extends DefaultCellEditor {
        // normal textfield background color
        private final Color colorNormal = null;

        // error textfield background color (when the date isn't correct
        private final Color colorError = new Color(255, 125, 125);

        private Date myDate;

        public DateCellEditor() {
            super(new JTextField());
        }

        public Component getTableCellEditorComponent(JTable arg0, Object arg1, boolean arg2, int arg3, int arg4) {
            JTextField result = (JTextField) super.getTableCellEditorComponent(arg0, arg1, arg2, arg3, arg4);
            result.selectAll();
            return result;
        }

        public Object getCellEditorValue() {
            return new GanttCalendar(myDate == null ? new Date() : myDate);
        }

        protected abstract Date parseDate(String dateString);

        public boolean stopCellEditing() {
            final String dateString = ((JTextComponent)getComponent()).getText();
            Date parsedDate = parseDate(dateString);
            if (parsedDate==null) {
                getComponent().setBackground(colorError);
                return false;
            }
            else {
                myDate = parsedDate;
                getComponent().setBackground(colorNormal);
                super.fireEditingStopped();
                return true;
            }
        }
    }

    protected abstract class VscrollAdjustmentListener implements AdjustmentListener {
        private final boolean isMod;

        protected VscrollAdjustmentListener(boolean calculateMod) {
            isMod = calculateMod;
        }
        protected abstract TimelineChart getChart();

        public void adjustmentValueChanged(AdjustmentEvent e) {
            if (getChart() == null) {
                return;
            }
            if (isMod) {
                getChart().getModel().setVerticalOffset(e.getValue() % getTreeTable().getRowHeight());
            } else {
                getChart().getModel().setVerticalOffset(e.getValue());
            }
            getChart().reset();
        }
    }

    void insertWithLeftyScrollBar(JComponent container) {
        JScrollPane scrollpane = new JScrollPane();
        container.add(scrollpane, BorderLayout.CENTER);
        scrollpane.getViewport().add(this);
        scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        final JPanel jp = new JPanel(new BorderLayout());
        jp.add(getVerticalScrollBar(), BorderLayout.CENTER);
        jp.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        jp.setVisible(false);
        getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                if (getSize().getHeight() - 20 < e.getAdjustable().getMaximum()) {
                    jp.setVisible(true);
                } else {
                    jp.setVisible(false);
                }
                repaint();
              }
              });
        container.add(jp, BorderLayout.WEST);
    }

    void addAction(Action action, KeyStroke keyStroke) {
        if (action != null) {
            InputMap inputMap = getInputMap();
            inputMap.put(keyStroke, action.getValue(Action.NAME));
            getActionMap().put(action.getValue(Action.NAME), action);
        }
    }

    void addActionWithKeyStroke(Action action) {
        if (action != null) {
            addAction(action, (KeyStroke) action.getValue(Action.ACCELERATOR_KEY));
        }
    }

    void setupActionMaps(Action up, Action down, Action indent, Action outdent, Action newArtifact,
            Action cut, Action copy, Action paste, Action properties, Action delete) {
        addAction(up, GPAction.getKeyStroke("moveUp.shortcut"));
        addAction(down, GPAction.getKeyStroke("moveDown.shortcut"));

        addAction(indent, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        addAction(outdent, KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK));
        addAction(newArtifact, GPAction.getKeyStroke("newArtifact.shortcut"));
        addAction(properties, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK));
        addAction(delete, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        addActionWithKeyStroke(cut);
        addActionWithKeyStroke(copy);
        addActionWithKeyStroke(paste);
    }

    private class HeaderMouseListener extends MouseAdapter {
        private final CustomPropertyManager myCustomPropertyManager;

        public HeaderMouseListener(CustomPropertyManager customPropertyManager) {
            super();
            myCustomPropertyManager = customPropertyManager;
        }

        /**
         * @inheritDoc Shows the popupMenu to hide/show columns and to add
         *             custom columns.
         */
        public void mousePressed(MouseEvent e) {
            handlePopupTrigger(e);
        }

        public void mouseReleased(MouseEvent e) {
            handlePopupTrigger(e);
        }

        private void handlePopupTrigger(MouseEvent e) {
            if (e.isPopupTrigger()) {
                GPAction[] popupActions = createPopupActions(e);
                myUiFacade.showPopupMenu(e.getComponent(), popupActions, e.getX(), e.getY());
            }
        }

        private GPAction[] createPopupActions(final MouseEvent mouseEvent) {
            final int columnAtPoint = getTable().columnAtPoint(mouseEvent.getPoint());
            final Column column = getTableHeaderUiFacade().findColumnByViewIndex(columnAtPoint);
            GPAction hideAction = new GPAction("columns.hide.label") {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    assert column.isVisible() : "how come it is at mouse click point?";
                    column.setVisible(false);
                }
              };
            if (columnAtPoint == -1) {
                hideAction.setEnabled(false);
            } else {
                hideAction.putValue(Action.NAME, GanttLanguage.getInstance().formatText("columns.hide.label", column.getName()));
            }
            return new GPAction[] {
              new GPAction("columns.manage.label") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ShowHideColumnsDialog dialog = new ShowHideColumnsDialog(myUiFacade, myTableHeaderFacade, myCustomPropertyManager);
                    dialog.show();
                }
              },
              hideAction
            };
        }
    }

}
