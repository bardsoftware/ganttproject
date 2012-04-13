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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.delay.Delay;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.TaskNode;

/**
 * Task tree table.
 *
 * @author bbaranne (Benoit Baranne) - original version
 * @author dbarashev (Dmitry Barashev) - complete rewrite in 2011
 */
public class GanttTreeTable extends GPTreeTableBase {
  private final GanttTreeTableModel ttModel;

  private final UIFacade myUIfacade;

  GanttTreeTable(IGanttProject project, UIFacade uifacade, GanttTreeTableModel model) {
    super(project, uifacade, project.getTaskCustomColumnManager(), model);
    this.ttModel = model;
    myUIfacade = uifacade;
    getTableHeaderUiFacade().createDefaultColumns(DefaultColumn.getColumnStubs());
    initTreeTable();
  }

  private UIFacade getUiFacade() {
    return myUIfacade;
  }

  static enum DefaultColumn {
    TYPE(new TableHeaderUIFacade.ColumnStub("tpd0", null, false, -1, -1)), PRIORITY(new TableHeaderUIFacade.ColumnStub(
        "tpd1", null, false, -1, 50)), INFO(new TableHeaderUIFacade.ColumnStub("tpd2", null, false, -1, -1)), NAME(
        new TableHeaderUIFacade.ColumnStub("tpd3", null, true, 0, 200)), BEGIN_DATE(new TableHeaderUIFacade.ColumnStub(
        "tpd4", null, true, 1, 75)), END_DATE(new TableHeaderUIFacade.ColumnStub("tpd5", null, true, 2, 75)), DURATION(
        new TableHeaderUIFacade.ColumnStub("tpd6", null, false, -1, 50)), COMPLETION(
        new TableHeaderUIFacade.ColumnStub("tpd7", null, false, -1, 50)), COORDINATOR(
        new TableHeaderUIFacade.ColumnStub("tpd8", null, false, -1, 200)), PREDECESSORS(
        new TableHeaderUIFacade.ColumnStub("tpd9", null, false, -1, 200)), ID(new TableHeaderUIFacade.ColumnStub(
        "tpd10", null, false, -1, 20)), ;

    private final Column myDelegate;

    private DefaultColumn(TableHeaderUIFacade.Column delegate) {
      myDelegate = delegate;
    }

    Column getStub() {
      return myDelegate;
    }

    static List<Column> getColumnStubs() {
      List<Column> result = new ArrayList<Column>();
      for (DefaultColumn dc : values()) {
        result.add(dc.myDelegate);
      }
      return result;
    }
  }

  @Override
  protected List<Column> getDefaultColumns() {
    return DefaultColumn.getColumnStubs();
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
    getVerticalScrollBar().addAdjustmentListener(new VscrollAdjustmentListener(true) {
      @Override
      protected TimelineChart getChart() {
        return myUIfacade.getGanttChart();
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

  void setDelay(TaskNode taskNode, Delay delay) {
    try {
      int indexInfo = getTable().getColumnModel().getColumnIndex(GanttTreeTableModel.strColInfo);
      indexInfo = getTable().convertColumnIndexToModel(indexInfo);
      ttModel.setValueAt(delay, taskNode, indexInfo);
    } catch (IllegalArgumentException e) {
    }
  }

  /**
   * This class repaints the GraphicArea and the table every time the table
   * model has been modified. TODO Add the refresh functionality when available.
   *
   * @author Benoit Baranne
   */
  private class ModelListener implements TableModelListener {
    @Override
    public void tableChanged(TableModelEvent e) {
      getUiFacade().getGanttChart().reset();
    }
  }

  void editSelectedTask() {
    TreePath selectedPath = getTree().getTreeSelectionModel().getSelectionPath();
    Column column = getTableHeaderUiFacade().findColumnByID(DefaultColumn.NAME.getStub().getID());
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
