/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.chart.mouse;

import java.util.Date;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.walker.WorkingUnitCounter;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.TimeUnitStack;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartModelBase.ScrollingSession;
import net.sourceforge.ganttproject.chart.TimelineChart.VScrollController;
import net.sourceforge.ganttproject.task.TaskManager;

public class TimelineFacadeImpl implements MouseInteraction.TimelineFacade {
  private final ChartModelBase myChartModel;
  private final TaskManager myTaskManager;
  private VScrollController myVScrollController;

  public TimelineFacadeImpl(ChartModelBase chartModel, TaskManager taskManager) {
    myChartModel = chartModel;
    myTaskManager = taskManager;
  }

  @Override
  public Date getDateAt(int x) {
    return myChartModel.getOffsetAt(x).getOffsetStart();
  }

  @Override
  public TimeDuration createTimeInterval(TimeUnit timeUnit, Date startDate, Date endDate) {
    WorkingUnitCounter workingUnitCounter = new WorkingUnitCounter(getCalendar(), timeUnit);
    if (startDate.before(endDate)) {
      return workingUnitCounter.run(startDate, endDate);
    }
    return workingUnitCounter.run(endDate, startDate).reverse();
  }

  @Override
  public TimeUnitStack getTimeUnitStack() {
    return myChartModel.getTimeUnitStack();
  }

  @Override
  public GPCalendarCalc getCalendar() {
    return myTaskManager.getCalendar();
  }

  @Override
  public Date getEndDateAt(int x) {
    return myChartModel.getOffsetAt(x).getOffsetEnd();
  }

  @Override
  public ScrollingSession createScrollingSession(final int xpos, final int ypos) {
    return new ScrollingSession() {
      private final ScrollingSession myDelegate = myChartModel.createScrollingSession(xpos);
      private int myStartYpos = ypos;

      @Override
      public void scrollTo(int xpos, int ypos) {
        myDelegate.scrollTo(xpos, ypos);
        if (myVScrollController != null && myVScrollController.isScrollable()) {
          myVScrollController.scrollBy(myStartYpos - ypos);
        }
        myStartYpos = ypos;
      }

      @Override
      public void finish() {
        myDelegate.finish();
      }
    };
  }

  public void setVScrollController(VScrollController vscrollController) {
    myVScrollController = vscrollController;
  }

}