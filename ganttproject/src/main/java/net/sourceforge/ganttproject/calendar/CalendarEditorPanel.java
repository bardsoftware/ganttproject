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

import biz.ganttproject.app.DialogKt;
import biz.ganttproject.app.InternationalizationKt;
import biz.ganttproject.app.LocalizedString;
import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.option.DefaultColorOption;
import biz.ganttproject.core.option.ValidationException;
import biz.ganttproject.core.option.ValidatorsKt;
import biz.ganttproject.core.option.ValueValidator;
import biz.ganttproject.core.time.CalendarFactory;
import com.google.common.base.*;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import javafx.scene.control.ButtonType;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.UIUtil.GPDateCellEditor;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.taskproperties.CommonPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.util.collect.Pair;
import org.apache.commons.lang3.time.DateUtils;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
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
  private static final List<String> TYPE_COLUMN_VALUES = Lists.transform(Arrays.asList(CalendarEvent.Type.values()), eventType -> getI18NedEventType(eventType));
  private static final Runnable NOOP_CALLBACK = () -> {
  };
  private final List<CalendarEvent> myOneOffEvents = Lists.newArrayList();
  private final List<CalendarEvent> myRecurringEvents = Lists.newArrayList();
  private final TableModelImpl myRecurringModel;
  private final TableModelImpl myOneOffModel;

  private final Runnable myOnChangeCallback;
  private final Runnable myOnCreate;
  private final UIFacade myUiFacade;



  private static Predicate<CalendarEvent> recurring(final boolean isRecurring) {
    return event -> event.isRecurring == isRecurring;
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
    myOnCreate = () -> reload(calendar);
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
    Supplier<List<DateFormat>> formatSupplier = Suppliers.ofInstance(Lists.newArrayList(dateFormats));
    final JLabel hintLabel = new JLabel(" "); // non-empty label to occupy some vertical space
    final ValueValidator<Date> realValidator = ValidatorsKt.createStringDateValidator(null, formatSupplier);
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

  interface FocusSetter {
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
      myEditor.setOnCancelCallback(() -> {
        cancelCellEditing();
        myEditor.getJComponent().removeFocusListener(onStartEditing);
        moveFocusToTable(row);
      });
      myEditor.setOnOkCallback(() -> {
        myEditor.getJComponent().removeFocusListener(onStartEditing);
        moveFocusToTable(row);
      });
      return myEditor.getJComponent();
    }

    private void moveFocusToTable(final int row) {
      SwingUtilities.invokeLater(() -> myFocusMover.setFocus(row));
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
      return myDefaultRenderer.getTableCellRendererComponent(table, formattedDate, isSelected, hasFocus,
          row, column);
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
    colorColumn.setCellRenderer(UIUtil.newColorRenderer(() -> tableModel.getRowCount() - 1));


    colorColumn.setCellEditor(new ColorEditor(uiFacade, row -> {
      table.requestFocus();
      table.getSelectionModel().setSelectionInterval(row, row);
    }));

    AbstractTableAndActionsComponent<CalendarEvent> tableAndActions = new AbstractTableAndActionsComponent<>(table) {
      @Override
      protected void onAddEvent() {
        LocalizedString title = InternationalizationKt.getRootLocalizer().create("calendar.editor.datePickerDialog.title");
        DialogKt.dialog(title, controller -> {
          controller.addStyleSheet("/biz/ganttproject/app/Dialog.css");
          controller.addStyleSheet("/biz/ganttproject/lib/MultiDatePicker.css");
          controller.addStyleClass("dlg");
          MultiDatePicker multiDatePicker = new MultiDatePicker();
          multiDatePicker.setValue(LocalDate.now());

          controller.setContent(multiDatePicker.getPopupContent());

          controller.setupButton(ButtonType.APPLY, button -> {
            button.getStyleClass().add("btn-attention");
            button.setText(InternationalizationKt.getRootLocalizer().formatText("add"));
            button.setOnAction(event -> {
              var selectedDates = multiDatePicker.getSelectedDates();
              SwingUtilities.invokeLater(() -> {
                for (LocalDate localDate : selectedDates) {
                  Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                  if (!tableModel.contains(date)) {
                    tableModel.setValueAt(CalendarFactory.createGanttCalendar(date), tableModel.getRowCount() - 1, 0);
                  }
                }
                controller.hide();
              });
            });
            return null;
          });
          return null;
        });
      }

      @Override
      protected void onDeleteEvent() {
        var selected = table.getSelectedRows();
        Arrays.sort(selected);
        for (int i = selected.length - 1; i >= 0; i--) {
          var selectedRow = selected[i];
          if (selectedRow < tableModel.getRowCount() - 1) {
            tableModel.delete(selectedRow);
          }
        }
      }

      @Override
      protected CalendarEvent getValue(int row) {
        return tableModel.getValue(row);
      }
    };

    Function<List<CalendarEvent>, Boolean> isDeleteEnabled = events -> events.size() != 1 || events.get(0) != null;
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

    private enum Column {
      DATES(CalendarEvent.class, null), SUMMARY(String.class, ""), TYPE(String.class, ""), COLOR(Color.class, Color.GRAY);

      private final String myTitle;
      private final Class<?> myClazz;
      private final Object myDefault;

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
        return MoreObjects.firstNonNull(e.getTitle(), "");
      case TYPE:
        return getI18NedEventType(e.getType());
      case COLOR:
        return MoreObjects.firstNonNull(e.getColor(), Column.values()[col].getDefault());
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

    public boolean contains(Date date) {
      for (CalendarEvent event : myEvents) {
        if (DateUtils.isSameDay(event.myDate, date)) {
          return true;
        }
      }
      return false;
    }
  }
}
