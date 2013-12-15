package net.sourceforge.ganttproject.calendar;

import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.GPCalendar;
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponent;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class CalendarEditorPanel {
  private final GPCalendar myCalendar;

  public CalendarEditorPanel(GPCalendar calendar) {
    myCalendar = calendar;
  }

  public JPanel createComponent() {
    final TableModelImpl model = new TableModelImpl(myCalendar);
    final JTable table = new JTable(model);
    UIUtil.setupTableUI(table);
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

  private static class TableModelImpl extends AbstractTableModel {
    private final List<CalendarEvent> myEvents;

    public TableModelImpl(GPCalendar calendar) {
      myEvents = Lists.newArrayList(calendar.getPublicHolidays());
    }

    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public int getRowCount() {
      return myEvents.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
      if (row < 0 || row >= getRowCount()) {
        return null;
      }
      CalendarEvent e = myEvents.get(row);
      switch (col) {
      case 0:
        return GanttLanguage.getInstance().getShortDateFormat().format(e.myDate);
      case 1:
        return Objects.firstNonNull(e.getTitle(), "");
      case 2:
        return e.getType().name();
      }
      return null;
    }
  }
}
