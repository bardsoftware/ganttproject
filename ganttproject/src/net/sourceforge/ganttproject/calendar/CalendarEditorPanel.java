/*
Copyright 2013 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.sourceforge.ganttproject.calendar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.UIUtil.GPDateCellEditor;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.ValueValidator;
import net.sourceforge.ganttproject.gui.taskproperties.CommonPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.CalendarEvent.Type;
import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.option.ValidationException;
import biz.ganttproject.core.time.CalendarFactory;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Implements a calendar editor component which consists of a table with calendar events (three columns: date, title, type)
 * and Add/Delete buttons
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CalendarEditorPanel {
  private static String getI18NedEventType(CalendarEvent.Type type) {
    return GanttLanguage.getInstance().getText(
        "calendar.editor.column." + TableModelImpl.Column.TYPE.name().toLowerCase() + ".value." + type.name().toLowerCase());
  }
  private static List<String> TYPE_COLUMN_VALUES = Lists.transform(Arrays.asList(CalendarEvent.Type.values()), new Function<CalendarEvent.Type, String>() {
    @Override
    public String apply(Type eventType) {
      return getI18NedEventType(eventType);
    }
  });
  private static final Runnable NOOP_CALLBACK = new Runnable() {
    @Override public void run() {
    }
  };
  private final List<CalendarEvent> myEvents = Lists.newArrayList();
  private JTable myTable;
  private TableModelImpl myModel;

  private final Runnable myOnChangeCallback;

  public CalendarEditorPanel(List<CalendarEvent> events, Runnable onChange) {
    myEvents.addAll(events);
    myOnChangeCallback = onChange == null ? NOOP_CALLBACK : onChange;
  }

  public CalendarEditorPanel(GPCalendar calendar, Runnable onChange) {
    myOnChangeCallback = onChange == null ? NOOP_CALLBACK : onChange;
    reload(calendar);
  }

  public void reload(GPCalendar calendar) {
    int size = myEvents.size();
    myEvents.clear();
    if (myModel != null) {
      myModel.fireTableRowsDeleted(0, size);
    }
    myEvents.addAll(calendar.getPublicHolidays());
    if (myModel != null) {
      myModel.fireTableRowsInserted(0, myEvents.size());
    }
  }
  public JPanel createComponent() {
    myModel = new TableModelImpl(myEvents, myOnChangeCallback);
    myTable = new JTable(myModel);

    UIUtil.setupTableUI(myTable);
    CommonPanel.setupComboBoxEditor(
        myTable.getColumnModel().getColumn(TableModelImpl.Column.TYPE.ordinal()),
        TYPE_COLUMN_VALUES.toArray(new String[0]));
    myTable.getColumnModel().getColumn(TableModelImpl.Column.RECURRING.ordinal()).setCellRenderer(myTable.getDefaultRenderer(TableModelImpl.Column.RECURRING.getColumnClass()));
    // We'll show a hint label under the table if user types something which we can't parse
    Date today = CalendarFactory.newCalendar().getTime();
    final String hint = GanttLanguage.getInstance().formatText("calendar.editor.dateHint",
        GanttLanguage.getInstance().getMediumDateFormat().format(today), GanttLanguage.getInstance().getShortDateFormat().format(today));
    final JLabel hintLabel = new JLabel(" "); // non-empty label to occupy some vertical space
    final ValueValidator<Date> realValidator = UIUtil.createStringDateValidator(
        null, GanttLanguage.getInstance().getLongDateFormat(), GanttLanguage.getInstance().getShortDateFormat());
    ValueValidator<Date> decorator = new ValueValidator<Date>() {
      @Override
      public Date parse(String text) throws ValidationException {
        try {
          Date result = realValidator.parse(text);
          hintLabel.setText("");
          return result;
        } catch (ValidationException e) {
          hintLabel.setText(hint);
          throw e;
        }
      }
    };

    TableColumn dateColumn = myTable.getColumnModel().getColumn(TableModelImpl.Column.DATES.ordinal());
    GPDateCellEditor dateEditor = new GPDateCellEditor(null, true, decorator);
    dateColumn.setCellEditor(dateEditor);

    class DateCellRendererImpl implements TableCellRenderer {
      private DefaultTableCellRenderer myDefaultRenderer = new DefaultTableCellRenderer();

      @Override
      public Component getTableCellRendererComponent(
          JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        assert value == null || value instanceof CalendarEvent : (value == null)
            ? "value is null" : String.format("value=%s class=%s", value, value.getClass());
        final String formattedDate;
        if (value == null) {
          formattedDate = "";
        } else {
          CalendarEvent e = (CalendarEvent) value;
          if (e.isRecurring) {
            formattedDate = new SimpleDateFormat("MMM dd").format(e.myDate);
          } else {
            formattedDate = GanttLanguage.getInstance().getShortDateFormat().format(e.myDate);
          }
        }
        JLabel result = (JLabel) myDefaultRenderer.getTableCellRendererComponent(table, formattedDate, isSelected, hasFocus,
            row, column);
        return result;
      }
    }

    dateColumn.setCellRenderer(new DateCellRendererImpl());
    AbstractTableAndActionsComponent<CalendarEvent> tableAndActions = new AbstractTableAndActionsComponent<CalendarEvent>(myTable) {
      @Override
      protected void onAddEvent() {
        int lastRow = myModel.getRowCount() - 1;
        Rectangle cellRect = myTable.getCellRect(lastRow, 0, true);
        myTable.scrollRectToVisible(cellRect);
        myTable.getSelectionModel().setSelectionInterval(lastRow, lastRow);
        myTable.editCellAt(lastRow, 0);
        myTable.getEditorComponent().requestFocus();
      }

      @Override
      protected void onDeleteEvent() {
        if (myTable.getSelectedRow() < myModel.getRowCount() - 1) {
          myModel.delete(myTable.getSelectedRow());
        }
      }

      @Override
      protected CalendarEvent getValue(int row) {
        return myModel.getValue(row);
      }
    };
    Function<List<CalendarEvent>, Boolean> isDeleteEnabled = new Function<List<CalendarEvent>, Boolean>() {
      public Boolean apply(List<CalendarEvent> events) {
        if (events.size() == 1 && events.get(0) == null) {
          return false;
        }
        return true;
      }
    };
    tableAndActions.getDeleteItemAction().putValue(AbstractTableAndActionsComponent.PROPERTY_IS_ENABLED_FUNCTION, isDeleteEnabled);
    JPanel result = AbstractTableAndActionsComponent.createDefaultTableAndActions(myTable, tableAndActions.getActionsComponent());
    result.add(hintLabel, BorderLayout.SOUTH);
    return result;
  }

  public List<CalendarEvent> getEvents() {
    return myEvents;
  }

  private static class TableModelImpl extends AbstractTableModel {
    private static enum Column {
      DATES(CalendarEvent.class, null), RECURRING(Boolean.class, false), SUMMARY(String.class, ""), TYPE(String.class, "");

      private String myTitle;
      private Class<?> myClazz;
      private Object myDefault;

      Column(Class<?> clazz, Object defaultValue) {
        myTitle = GanttLanguage.getInstance().getText("calendar.editor.column." + name().toLowerCase() + ".title");
        myClazz = clazz;
        myDefault = defaultValue;
      }

      public String getTitle() {
        return myTitle;
      }

      public Class<?> getColumnClass() {
        return myClazz;
      }

      public Object getDefault() {
        return myDefault;
      }
    }
    private final List<CalendarEvent> myEvents;
    private final Runnable myOnChangeCallback;

    public TableModelImpl(List<CalendarEvent> events, Runnable onChangeCallback) {
      myEvents = events;
      myOnChangeCallback = onChangeCallback;
    }

    CalendarEvent getValue(int row) {
      return row < myEvents.size() ? myEvents.get(row) : null;
    }

    void delete(int row) {
      myEvents.remove(row);
      fireTableRowsDeleted(row, row);
      myOnChangeCallback.run();
    }

    @Override
    public int getColumnCount() {
      return Column.values().length;
    }

    @Override
    public int getRowCount() {
      return myEvents.size() + 1;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return Column.values()[columnIndex].getColumnClass();
    }

    @Override
    public String getColumnName(int column) {
      return Column.values()[column].getTitle();
    }

    @Override
    public Object getValueAt(int row, int col) {
      if (row < 0 || row >= getRowCount()) {
        return null;
      }
      if (row == getRowCount() - 1) {
        return Column.values()[col].getDefault();
      }
      CalendarEvent e = myEvents.get(row);
      switch (Column.values()[col]) {
      case DATES:
        return e;
      case RECURRING:
        return Boolean.valueOf(e.isRecurring);
      case SUMMARY:
        return Objects.firstNonNull(e.getTitle(), "");
      case TYPE:
        return getI18NedEventType(e.getType());
      }
      return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return true;
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
      if (row < 0 || row >= getRowCount()) {
        return;
      }
      String value = String.valueOf(aValue);
      if (row == getRowCount() - 1) {
        myEvents.add(CalendarEvent.newEvent(null, false, CalendarEvent.Type.HOLIDAY, ""));
      }
      CalendarEvent e = myEvents.get(row);
      CalendarEvent newEvent = null;
      switch (Column.values()[col]) {
      case DATES:
        try {
          Date date = GanttLanguage.getInstance().getShortDateFormat().parse(value);
          newEvent = CalendarEvent.newEvent(date, e.isRecurring, e.getType(), e.getTitle());
        } catch (ParseException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        break;
      case SUMMARY:
        newEvent = CalendarEvent.newEvent(e.myDate, e.isRecurring, e.getType(), value);
        break;
      case TYPE:
        for (CalendarEvent.Type eventType : CalendarEvent.Type.values()) {
          if (getI18NedEventType(eventType).equals(value)) {
            newEvent = CalendarEvent.newEvent(e.myDate, e.isRecurring, eventType, e.getTitle());
          }
        }
        break;
      case RECURRING:
        newEvent = CalendarEvent.newEvent(e.myDate, Boolean.valueOf(value), e.getType(), e.getTitle());
        break;
      }
      if (newEvent != null) {
        myEvents.set(row,  newEvent);
        fireTableRowsUpdated(row, row + 1);
        myOnChangeCallback.run();
      }
    }
  }
}
