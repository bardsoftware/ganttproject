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

import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.CalendarEvent.Type;
import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.option.ValidationException;
import biz.ganttproject.core.time.CalendarFactory;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.UIUtil.GPDateCellEditor;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder.ValueValidator;
import net.sourceforge.ganttproject.gui.taskproperties.CommonPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.collect.Pair;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
  private final List<CalendarEvent> myOneOffEvents = Lists.newArrayList();
  private final List<CalendarEvent> myRecurringEvents = Lists.newArrayList();
  private final TableModelImpl myRecurringModel;
  private final TableModelImpl myOneOffModel;

  private final Runnable myOnChangeCallback;
  private final Runnable myOnCreate;
  private final UIFacade myUiFacade;

  private static Predicate<CalendarEvent> recurring(final boolean isRecurring) {
    return new Predicate<CalendarEvent>() {
      @Override
      public boolean apply(CalendarEvent event) {
        return event.isRecurring == isRecurring;
      }
    };
  }
  public CalendarEditorPanel(UIFacade uifacade, List<CalendarEvent> events, Runnable onChange) {
    myOneOffEvents.addAll(Collections2.filter(events, recurring(false)));
    myRecurringEvents.addAll(Collections2.filter(events, recurring(true)));
    myOnChangeCallback = onChange == null ? NOOP_CALLBACK : onChange;
    myOnCreate = NOOP_CALLBACK;
    myUiFacade = uifacade;
    myRecurringModel = new TableModelImpl(myRecurringEvents, myOnChangeCallback, true);
    myOneOffModel = new TableModelImpl(myOneOffEvents, myOnChangeCallback, false);
  }

  public CalendarEditorPanel(UIFacade uifacade, final GPCalendar calendar, Runnable onChange) {
    myUiFacade = uifacade;
    myOnChangeCallback = onChange == null ? NOOP_CALLBACK : onChange;
    myOnCreate = new Runnable() {
      @Override
      public void run() {
        reload(calendar);
      }
    };
    myRecurringModel = new TableModelImpl(myRecurringEvents, myOnChangeCallback, true);
    myOneOffModel = new TableModelImpl(myOneOffEvents, myOnChangeCallback, false);
  }

  public void reload(GPCalendar calendar) {
    reload(calendar, myOneOffEvents, myOneOffModel);
    reload(calendar, myRecurringEvents, myRecurringModel);
  }

  private static void reload(GPCalendar calendar, List<CalendarEvent> events, TableModelImpl model) {
    int size = events.size();
    events.clear();
    model.fireTableRowsDeleted(0, size);
    events.addAll(Collections2.filter(calendar.getPublicHolidays(), recurring(model.isRecurring())));
    model.fireTableRowsInserted(0, events.size());
  }

  public JComponent createComponent() {
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab(GanttLanguage.getInstance().getText("calendar.editor.tab.oneoff.title"),
        createNonRecurringComponent());
    tabbedPane.addTab(GanttLanguage.getInstance().getText("calendar.editor.tab.recurring.title"),
        createRecurringComponent());
    myOnCreate.run();
    return tabbedPane;
  }

  private Component createRecurringComponent() {
    DateFormat dateFormat = GanttLanguage.getInstance().getRecurringDateFormat();
    AbstractTableAndActionsComponent<CalendarEvent> tableAndActions = createTableComponent(myRecurringModel, dateFormat, myUiFacade);
    JPanel result = AbstractTableAndActionsComponent.createDefaultTableAndActions(tableAndActions.getTable(), tableAndActions.getActionsComponent());

    Date today = CalendarFactory.newCalendar().getTime();
    final String hint = GanttLanguage.getInstance().formatText("calendar.editor.dateHint", dateFormat.format(today));
    Pair<JLabel,? extends TableCellEditor> validator = createDateValidatorComponents(hint, dateFormat);
    TableColumn dateColumn = tableAndActions.getTable().getColumnModel().getColumn(TableModelImpl.Column.DATES.ordinal());
    dateColumn.setCellEditor(validator.second());
    result.add(validator.first(), BorderLayout.SOUTH);
    result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    return result;
  }

  public JPanel createNonRecurringComponent() {
    AbstractTableAndActionsComponent<CalendarEvent> tableAndActions = createTableComponent(myOneOffModel, GanttLanguage.getInstance().getShortDateFormat(), myUiFacade);
    JPanel result = AbstractTableAndActionsComponent.createDefaultTableAndActions(tableAndActions.getTable(), tableAndActions.getActionsComponent());

    Date today = CalendarFactory.newCalendar().getTime();
    final String hint = GanttLanguage.getInstance().formatText("calendar.editor.dateHint",
        GanttLanguage.getInstance().getMediumDateFormat().format(today), GanttLanguage.getInstance().getShortDateFormat().format(today));

    Pair<JLabel,? extends TableCellEditor> validator = createDateValidatorComponents(hint, GanttLanguage.getInstance().getMediumDateFormat(), GanttLanguage.getInstance().getShortDateFormat());
    TableColumn dateColumn = tableAndActions.getTable().getColumnModel().getColumn(TableModelImpl.Column.DATES.ordinal());
    dateColumn.setCellEditor(validator.second());
    result.add(validator.first(), BorderLayout.SOUTH);
    result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    return result;
  }

  private static Pair<JLabel, ? extends TableCellEditor> createDateValidatorComponents(final String hint, DateFormat... dateFormats) {
    Supplier<List<DateFormat>> formatSupplier = Suppliers.<List<DateFormat>>ofInstance(Lists.newArrayList(dateFormats));
    final JLabel hintLabel = new JLabel(" "); // non-empty label to occupy some vertical space
    final ValueValidator<Date> realValidator = UIUtil.createStringDateValidator(null, formatSupplier);
    ValueValidator<Date> decorator = new ValueValidator<Date>() {
      @Override
      public Date parse(String text) throws ValidationException {
        try {
          Date result = realValidator.parse(text);
          hintLabel.setText("");
          return result;
        } catch (ValidationException e) {
          e.printStackTrace();
          hintLabel.setText(hint);
          throw e;
        }
      }
    };
    GPDateCellEditor dateEditor = new GPDateCellEditor(null, true, decorator, formatSupplier);
    return Pair.create(hintLabel, dateEditor);
  }

  static interface FocusSetter {
    void setFocus(int row);
  }
  static class ColorEditor extends AbstractCellEditor implements TableCellEditor {
    private final OptionsPageBuilder.ColorComponent myEditor;
    private final DefaultColorOption myOption;
    private final FocusSetter myFocusMover;

    ColorEditor(UIFacade uiFacade, FocusSetter focusSetter) {
      myOption = new DefaultColorOption("sadf") {
        @Override
        protected void resetValue(Color value, boolean resetInitial, Object clientId) {
          super.resetValue(value, resetInitial, clientId);
          if (clientId != this) {
            stopCellEditing();
          }
        }
      };
      OptionsPageBuilder builder = new OptionsPageBuilder();
      builder.setUiFacade(uiFacade);
      myEditor = builder.createColorComponent(myOption);
      myFocusMover = focusSetter;
    }


    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, final int row, int column) {
      TableCellRenderer renderer = table.getCellRenderer(row, column);
      Component c = renderer.getTableCellRendererComponent(table, value, isSelected, true, row, column);
      if (c != null) {
        myEditor.getJComponent().setOpaque(true);
        myEditor.getJComponent().setBackground(c.getBackground());
        if (c instanceof JComponent) {
          myEditor.getJComponent().setBorder(((JComponent) c).getBorder());
        }
      } else {
        myEditor.getJComponent().setOpaque(false);
      }
      myOption.setValue((Color)value, this);
      final FocusListener onStartEditing = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
          myEditor.getJComponent().removeFocusListener(this);
          myEditor.openChooser();
        }
      };
      myEditor.getJComponent().addFocusListener(onStartEditing);
      myEditor.setOnCancelCallback(new Runnable() {
        @Override
        public void run() {
          cancelCellEditing();
          myEditor.getJComponent().removeFocusListener(onStartEditing);
          moveFocusToTable(row);
        }
      });
      myEditor.setOnOkCallback(new Runnable() {
        @Override
        public void run() {
          myEditor.getJComponent().removeFocusListener(onStartEditing);
          moveFocusToTable(row);
        }
      });
      return myEditor.getJComponent();
    }

    private void moveFocusToTable(final int row) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          myFocusMover.setFocus(row);
        }
      });
    }

    @Override
    public Object getCellEditorValue() {
      return myOption.getValue();
    }
  }

  static class DateCellRendererImpl implements TableCellRenderer {
    private final DefaultTableCellRenderer myDefaultRenderer = new DefaultTableCellRenderer();
    private final DateFormat myDateFormat;

    DateCellRendererImpl(DateFormat dateFormat) {
      myDateFormat = dateFormat;
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      assert (value == null || value instanceof CalendarEvent) : (value == null)
          ? "value is null" : String.format("value=%s class=%s", value, value.getClass());
      final String formattedDate;
      if (value == null) {
        formattedDate = "";
      } else {
        CalendarEvent e = (CalendarEvent) value;
        formattedDate = myDateFormat.format(e.myDate);
      }
      JLabel result = (JLabel) myDefaultRenderer.getTableCellRendererComponent(table, formattedDate, isSelected, hasFocus,
          row, column);
      return result;
    }
  }


  private static AbstractTableAndActionsComponent<CalendarEvent> createTableComponent(final TableModelImpl tableModel, final DateFormat dateFormat, UIFacade uiFacade) {
    final JTable table = new JTable(tableModel);

    UIUtil.setupTableUI(table);
    CommonPanel.setupComboBoxEditor(
        table.getColumnModel().getColumn(TableModelImpl.Column.TYPE.ordinal()),
        TYPE_COLUMN_VALUES.toArray(new String[0]));
    //myTable.getColumnModel().getColumn(TableModelImpl.Column.RECURRING.ordinal()).setCellRenderer(myTable.getDefaultRenderer(TableModelImpl.Column.RECURRING.getColumnClass()));
    // We'll show a hint label under the table if user types something which we can't parse

    TableColumn dateColumn = table.getColumnModel().getColumn(TableModelImpl.Column.DATES.ordinal());
    dateColumn.setCellRenderer(new DateCellRendererImpl(dateFormat));

    TableColumn colorColumn = table.getColumnModel().getColumn(TableModelImpl.Column.COLOR.ordinal());
    colorColumn.setCellRenderer(UIUtil.newColorRenderer(new Supplier<Integer>() {
      @Override
      public Integer get() {
        return tableModel.getRowCount() - 1;
      }
    }));


    colorColumn.setCellEditor(new ColorEditor(uiFacade, new FocusSetter() {
      @Override
      public void setFocus(int row) {
        table.requestFocus();
        table.getSelectionModel().setSelectionInterval(row, row);
      }
    }));

    AbstractTableAndActionsComponent<CalendarEvent> tableAndActions = new AbstractTableAndActionsComponent<CalendarEvent>(table) {
      @Override
      protected void onAddEvent() {
        int lastRow = tableModel.getRowCount() - 1;
        Rectangle cellRect = table.getCellRect(lastRow, 0, true);
        table.scrollRectToVisible(cellRect);
        table.getSelectionModel().setSelectionInterval(lastRow, lastRow);
        table.editCellAt(lastRow, 0);
        table.getEditorComponent().requestFocus();
      }

      @Override
      protected void onDeleteEvent() {
        if (table.getSelectedRow() < tableModel.getRowCount() - 1) {
          tableModel.delete(table.getSelectedRow());
        }
      }

      @Override
      protected CalendarEvent getValue(int row) {
        return tableModel.getValue(row);
      }
    };
    Function<List<CalendarEvent>, Boolean> isDeleteEnabled = new Function<List<CalendarEvent>, Boolean>() {
      @Override
      public Boolean apply(List<CalendarEvent> events) {
        if (events.size() == 1 && events.get(0) == null) {
          return false;
        }
        return true;
      }
    };
    tableAndActions.getDeleteItemAction().putValue(AbstractTableAndActionsComponent.PROPERTY_IS_ENABLED_FUNCTION, isDeleteEnabled);
    return tableAndActions;
  }

  public List<CalendarEvent> getEvents() {
    List<CalendarEvent> result = Lists.newArrayList();
    result.addAll(myOneOffEvents);
    result.addAll(myRecurringEvents);
    return result;
  }

  private static class TableModelImpl extends AbstractTableModel {
    private static enum Column {
      DATES(CalendarEvent.class, null), SUMMARY(String.class, ""), TYPE(String.class, ""), COLOR(Color.class, Color.GRAY);

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
    private final boolean isRecurring;

    TableModelImpl(List<CalendarEvent> events, Runnable onChangeCallback, boolean recurring) {
      myEvents = events;
      myOnChangeCallback = onChangeCallback;
      isRecurring = recurring;
    }

    boolean isRecurring() {
      return isRecurring;
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
      case SUMMARY:
        return Objects.firstNonNull(e.getTitle(), "");
      case TYPE:
        return getI18NedEventType(e.getType());
      case COLOR:
        return Objects.firstNonNull(e.getColor(), Column.values()[col].getDefault());
      }
      return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return rowIndex < myEvents.size() || columnIndex == Column.DATES.ordinal();
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
      if (row < 0 || row >= getRowCount()) {
        return;
      }
      if (aValue == null) {
        return;
      }
      String value = String.valueOf(aValue);
      if (row == getRowCount() - 1) {
        myEvents.add(CalendarEvent.newEvent(null, isRecurring, CalendarEvent.Type.HOLIDAY, "", null));
      }
      CalendarEvent e = myEvents.get(row);
      CalendarEvent newEvent = null;
      switch (Column.values()[col]) {
      case DATES:
        try {
          Date date = GanttLanguage.getInstance().getShortDateFormat().parse(value);
          newEvent = CalendarEvent.newEvent(date, e.isRecurring, e.getType(), e.getTitle(), e.getColor());
        } catch (ParseException e1) {
          GPLogger.log(e1);
        }
        break;
      case SUMMARY:
        newEvent = CalendarEvent.newEvent(e.myDate, e.isRecurring, e.getType(), value, e.getColor());
        break;
      case TYPE:
        for (CalendarEvent.Type eventType : CalendarEvent.Type.values()) {
          if (getI18NedEventType(eventType).equals(value)) {
            newEvent = CalendarEvent.newEvent(e.myDate, e.isRecurring, eventType, e.getTitle(), e.getColor());
          }
        }
        break;
      case COLOR:
        assert aValue instanceof Color : "Bug: we expect Color but we get " + aValue.getClass();
        newEvent = CalendarEvent.newEvent(e.myDate, e.isRecurring, e.getType(), e.getTitle(), (Color)aValue);
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
