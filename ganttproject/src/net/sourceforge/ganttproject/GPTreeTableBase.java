/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 GanttProject Team

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
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

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
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomColumn;

import org.jdesktop.jdnc.JNTreeTable;
import org.jdesktop.swing.JXTreeTable;
import org.jdesktop.swing.table.TableColumnExt;
import org.jdesktop.swing.treetable.TreeTableModel;

public class GPTreeTableBase extends JNTreeTable{
    private final IGanttProject myProject;

    protected GPTreeTableBase(IGanttProject project, TreeTableModel model) {
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
        myProject = project;
    }

    protected IGanttProject getProject() {
        return myProject;
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
        TableCellEditor defaultEditor = getTreeTable().getDefaultEditor(getTreeTableModel().getColumnClass(modelIndex));
        if (defaultEditor != null) {
            result.setCellEditor(new TreeTableCellEditorImpl(defaultEditor));
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
                for (DateFormat format : formats) {
                    try {
                        Date typedDate = format.parse(dateString);
                        Calendar typedCal = CalendarFactory.newCalendar();
                        typedCal.setTime(typedDate);
                        Calendar projectStartCal = CalendarFactory.newCalendar();
                        projectStartCal.setTime(myProject.getTaskManager().getProjectStart());
                        int yearDiff = Math.abs(typedCal.get(Calendar.YEAR) - projectStartCal.get(Calendar.YEAR));
                        if (yearDiff > 1500) {
                            AttributedCharacterIterator iter = format.formatToCharacterIterator(typedDate);
                            int additionalZeroes = -1;
                            StringBuffer result = new StringBuffer();
                            for (char c = iter.first(); c != AttributedCharacterIterator.DONE; c = iter.next()) {
                                if (iter.getAttribute(DateFormat.Field.YEAR)!=null && additionalZeroes==-1) {
                                    additionalZeroes = iter.getRunLimit(DateFormat.Field.YEAR) - iter.getIndex();
                                    for (int j = 0; j < additionalZeroes; j++) {
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
                        // Continue with next format and hope for the best
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
        /** normal textfield background color */
        private final Color colorNormal = null;

        /** error textfield background color (when the date isn't correct) */
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
            final String dateString = ((JTextComponent) getComponent()).getText();
            Date parsedDate = parseDate(dateString);
            if (parsedDate == null) {
                getComponent().setBackground(colorError);
                return false;
            } else {
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
            if (getChart() != null) {
                if (isMod) {
                    getChart().getModel().setVerticalOffset(
                            e.getValue() % getTreeTable().getRowHeight());
                } else {
                    getChart().getModel().setVerticalOffset(e.getValue());
                }
                getChart().reset();
            }
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
}
