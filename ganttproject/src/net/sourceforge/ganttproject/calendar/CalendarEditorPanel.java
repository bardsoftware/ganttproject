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

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.taskproperties.CommonPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.CalendarEvent.Type;
import biz.ganttproject.core.calendar.GPCalendar;

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
  private final List<CalendarEvent> myEvents;

  public CalendarEditorPanel(GPCalendar calendar) {
    myEvents = Lists.newArrayList(calendar.getPublicHolidays());
  }

  public JPanel createComponent() {
    final TableModelImpl model = new TableModelImpl(myEvents);
    final JTable table = new JTable(model);
    UIUtil.setupTableUI(table);
    CommonPanel.setupComboBoxEditor(
        table.getColumnModel().getColumn(TableModelImpl.Column.TYPE.ordinal()),
        TYPE_COLUMN_VALUES.toArray(new String[0]));
    AbstractTableAndActionsComponent<CalendarEvent> tableAndActions = new AbstractTableAndActionsComponent<CalendarEvent>(table) {
      @Override
      protected void onAddEvent() {
        table.editCellAt(model.getRowCount() - 1, 0);
      }

      @Override
      protected void onDeleteEvent() {
        //model.delete(table.getSelectedRows());
      }

      @Override
      protected void onSelectionChanged() {
      }
    };
    return AbstractTableAndActionsComponent.createDefaultTableAndActions(table, tableAndActions.getActionsComponent());
  }

  public List<CalendarEvent> getEvents() {
    return myEvents;
  }

  private static class TableModelImpl extends AbstractTableModel {
    private static enum Column {
      DATES(String.class), SUMMARY(String.class), TYPE(String.class);

      private String myTitle;
      private Class<?> myClazz;

      Column(Class<?> clazz) {
        myTitle = GanttLanguage.getInstance().getText("calendar.editor.column." + name().toLowerCase() + ".title");
        myClazz = clazz;
      }

      public String getTitle() {
        return myTitle;
      }

      public Class<?> getColumnClass() {
        return myClazz;
      }
    }
    private final List<CalendarEvent> myEvents;

    public TableModelImpl(List<CalendarEvent> events) {
      myEvents = events;
    }

    @Override
    public int getColumnCount() {
      return Column.values().length;
    }

    @Override
    public int getRowCount() {
      return myEvents.size();
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
      CalendarEvent e = myEvents.get(row);
      switch (Column.values()[col]) {
      case DATES:
        return GanttLanguage.getInstance().getShortDateFormat().format(e.myDate);
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
      }
      if (newEvent != null) {
        myEvents.set(row,  newEvent);
        fireTableRowsUpdated(row, row + 1);
      }
    }
  }
}
