package net.sourceforge.ganttproject;

import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.text.AttributedCharacterIterator;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;

import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.util.DateUtils;

import org.jdesktop.jdnc.JNTreeTable;
import org.jdesktop.swing.JXTreeTable;
import org.jdesktop.swing.table.TableColumnExt;
import org.jdesktop.swing.treetable.TreeTableModel;

class GPTreeTableBase extends JNTreeTable{
	private final IGanttProject myProject;
	protected IGanttProject getProject() {
		return myProject;
	}

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
    	if (defaultEditor!=null) {
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
                			for (char c = iter.first(); c!=iter.DONE; c = iter.next()) {
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
}
