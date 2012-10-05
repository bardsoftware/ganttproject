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
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.tree.TreePath;

import biz.ganttproject.core.table.ColumnList.Column;

import net.sourceforge.ganttproject.chart.Chart;
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
    getTableHeaderUiFacade().createDefaultColumns(TaskDefaultColumn.getColumnStubs());
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
    Column column = getTableHeaderUiFacade().findColumnByID(TaskDefaultColumn.NAME.getStub().getID());
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
