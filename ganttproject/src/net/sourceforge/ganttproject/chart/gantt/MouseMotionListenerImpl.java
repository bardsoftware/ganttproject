/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.chart.gantt;

import java.awt.Cursor;
import java.awt.event.MouseEvent;

import net.sourceforge.ganttproject.ChartComponentBase;
import net.sourceforge.ganttproject.GanttGraphicArea;
import net.sourceforge.ganttproject.chart.ChartModelImpl;
import net.sourceforge.ganttproject.chart.item.ChartItem;
import net.sourceforge.ganttproject.chart.item.TaskBoundaryChartItem;
import net.sourceforge.ganttproject.chart.item.TaskProgressChartItem;
import net.sourceforge.ganttproject.chart.mouse.MouseMotionListenerBase;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;

class MouseMotionListenerImpl extends MouseMotionListenerBase {
  private final ChartComponentBase myChartComponent;
  private GanttChartController myChartController;

  public MouseMotionListenerImpl(GanttChartController chartImplementation, ChartModelImpl chartModel,
      UIFacade uiFacade, ChartComponentBase chartComponent) {
    super(uiFacade, chartImplementation);
    myChartController = chartImplementation;
    myChartComponent = chartComponent;
  }

  // Move the move on the area
  @Override
  public void mouseMoved(MouseEvent e) {
    ChartItem itemUnderPoint = myChartController.getChartItemUnderMousePoint(e.getX(), e.getY());
    Task taskUnderPoint = itemUnderPoint == null ? null : itemUnderPoint.getTask();
    // System.err.println("[OldMouseMotionListenerImpl] mouseMoved:
    // taskUnderPoint="+taskUnderPoint);
    if (taskUnderPoint == null) {
      myChartComponent.setDefaultCursor();
    } else {
      if (itemUnderPoint instanceof TaskBoundaryChartItem) {
        Cursor cursor = ((TaskBoundaryChartItem) itemUnderPoint).isStartBoundary() ? GanttGraphicArea.W_RESIZE_CURSOR
            : GanttGraphicArea.E_RESIZE_CURSOR;
        myChartComponent.setCursor(cursor);
      }
      // special cursor
      else if (itemUnderPoint instanceof TaskProgressChartItem) {
        myChartComponent.setCursor(GanttGraphicArea.CHANGE_PROGRESS_CURSOR);
      } else {
        myChartComponent.setDefaultCursor();
      }
    }
  }
}