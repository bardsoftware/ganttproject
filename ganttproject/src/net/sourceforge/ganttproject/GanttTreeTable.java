/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
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

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.table.ColumnList.Column;
import com.google.common.base.Supplier;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;

import javax.swing.*;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Task tree table.
 *
 * @author bbaranne (Benoit Baranne) - original version
 * @author dbarashev (Dmitry Barashev) - complete rewrite in 2011
 */
public class GanttTreeTable extends GPTreeTableBase {
  private final UIFacade myUIfacade;
  private static final NumberFormat ID_FORMAT = (NumberFormat) NumberFormat.getIntegerInstance().clone();

  static {
    ID_FORMAT.setGroupingUsed(false);
  }

  GanttTreeTable(IGanttProject project, final UIFacade uifacade, GanttTreeTableModel model) {
    super(project, uifacade, project.getTaskCustomColumnManager(), model);
    myUIfacade = uifacade;
    getTableHeaderUiFacade().createDefaultColumns(TaskDefaultColumn.getColumnStubs());
    setDropMode(DropMode.ON);
    final GPTreeTransferHandler transferHandler = new GPTreeTransferHandler(this, project.getTaskManager(), new Supplier<GanttChart>() {
      @Override
      public GanttChart get() {
        return uifacade.getGanttChart();
      }
    }, uifacade.getUndoManager());
    setTransferHandler(transferHandler);
    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(MouseEvent e) {
        transferHandler.exportAsDrag(getTable(), e, TransferHandler.MOVE);
      }
    });
  }

  private UIFacade getUiFacade() {
    return myUIfacade;
  }

  @Override
  protected List<Column> getDefaultColumns() {
    return TaskDefaultColumn.getColumnStubs();
  }

  @Override
  protected Chart getChart() {
    return myUIfacade.getGanttChart();
  }

  @Override
  protected void doInit() {
    super.doInit();
    getTable().getColumnModel().addColumnModelListener((TableColumnModelListener) this.getTreeTableModel());
    getTable().getModel().addTableModelListener(new ModelListener());
    VscrollAdjustmentListener vscrollListener = new VscrollAdjustmentListener(myUIfacade.getGanttChart(), true);
    getVerticalScrollBar().addAdjustmentListener(vscrollListener);
    myUIfacade.getGanttChart().setVScrollController(vscrollListener);
    TableCellRenderer idRenderer = new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                     int row, int column) {
        if (value instanceof Integer) {
          value = ID_FORMAT.format(value);
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      }
    };
    getTableHeaderUiFacade().findColumnByID(TaskDefaultColumn.ID.getStub().getID())
        .getTableColumnExt().setCellRenderer(idRenderer);

    getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        int index = getTable().columnAtPoint(mouseEvent.getPoint());
        if (index == -1) {
          return;
        }

        if (mouseEvent.isPopupTrigger() || mouseEvent.getButton() != MouseEvent.BUTTON1) {
          return;
        }
        if (mouseEvent.isAltDown() || mouseEvent.isShiftDown() || mouseEvent.isControlDown()) {
          return;
        }
        final TableHeaderUiFacadeImpl tableHeader = getTableHeaderUiFacade();
        final ColumnImpl column = tableHeader.findColumnByViewIndex(index);
        final TaskDefaultColumn taskColumn = TaskDefaultColumn.find(column.getID());

        getUiFacade().getUndoManager().undoableEdit(GanttLanguage.getInstance().getText("task.sort"), new Runnable() {
          @Override
          public void run() {
            if (taskColumn == TaskDefaultColumn.BEGIN_DATE || taskColumn == TaskDefaultColumn.END_DATE) {
              for (int i = 0; i < tableHeader.getSize(); i++) {
                Column c = tableHeader.getField(i);
                if (c != column) {
                  c.setSort(SortOrder.UNSORTED);
                }
              }

              if (column.getSort() == SortOrder.ASCENDING) {
                column.setSort(SortOrder.DESCENDING);
                getProject().getTaskManager().getTaskHierarchy().sort(
                    Collections.reverseOrder((Comparator<Task>) taskColumn.getSortComparator())
                );
              } else {
                column.setSort(SortOrder.ASCENDING);
                getProject().getTaskManager().getTaskHierarchy().sort((Comparator<Task>) taskColumn.getSortComparator());
              }
            }
          }
        });
      }
    });


  }

  void centerViewOnSelectedCell() {
    int row = getTable().getSelectedRow();
    int col = getTable().getEditingColumn();
    if (col == -1) {
      col = getTable().getSelectedColumn();
    }
    Rectangle rect = getTable().getCellRect(row, col, true);
    getHorizontalScrollBar().scrollRectToVisible(rect);
    getScrollPane().getViewport().scrollRectToVisible(rect);
  }

  private class ModelListener implements TableModelListener {
    @Override
    public void tableChanged(TableModelEvent e) {
      getUiFacade().getGanttChart().reset();
    }
  }

  void editSelectedTask() {
    TreePath selectedPath = getTree().getTreeSelectionModel().getSelectionPath();
    Column column = getTableHeaderUiFacade().findColumnByID(TaskDefaultColumn.NAME.getStub().getID());
    putClientProperty("GPTreeTableBase.selectAll", true);
    putClientProperty("GPTreeTableBase.clearText", false);
    editCellAt(getTree().getRowForPath(selectedPath), column.getOrder());
  }

  @Override
  protected void onProjectCreated() {
    super.onProjectCreated();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        getUiFacade().getGanttChart().reset();
      }
    });
  }
}
