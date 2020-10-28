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
package net.sourceforge.ganttproject.action.scroll;

import java.awt.event.ActionEvent;

import biz.ganttproject.core.time.TimeDuration;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.task.TaskManager;

public class ScrollTimeIntervalAction extends GPAction {
  private final ScrollingManager myScrollingManager;
  private final int myInterval;
  private final TaskManager myTaskManager;
  private final ChartModel myChartModel;

  public ScrollTimeIntervalAction(String name, int interval, TaskManager taskManager, ChartModel chartModel,
      ScrollingManager scrollingManager) {
    super(name);
    myScrollingManager = scrollingManager;
    myInterval = interval;
    myTaskManager = taskManager;
    myChartModel = chartModel;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    myScrollingManager.scrollBy(createTimeInterval());
  }

  private TimeDuration createTimeInterval() {
    return myTaskManager.createLength(myChartModel.getBottomUnit(), myInterval);
  }

}
