/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomEvent;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager.ZoomState;

import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

/**
 * @author bard
 */
public class ChartViewState implements ScrollingListener, ZoomListener {
  private ZoomState myCurrentZoomState;
  private UIFacade myUIFacade;

  private final TimelineChart myChart;
  private int myOffsetPixels;

  public ChartViewState(TimelineChart chart, UIFacade uiFacade) {
    myChart = chart;
    myUIFacade = uiFacade;
  }

  @Override
  public void scrollBy(TimeDuration duration) {
    myChart.scrollBy(duration);
    myOffsetPixels = 0;
    myChart.setStartOffset(myOffsetPixels);
  }

  @Override
  public void scrollBy(int pixels) {
    myOffsetPixels += pixels;
    myChart.setStartOffset(myOffsetPixels);
  }

  @Override
  public void scrollTo(Date date) {
    myChart.setStartDate(date);
  }

  @Override
  public void zoomChanged(ZoomEvent e) {
    myCurrentZoomState = e.getNewZoomState();
    Date date;
    var activeView = myUIFacade.getViewManager().getActiveView();
    if (activeView != null && Objects.equals(activeView.getId(), String.valueOf(UIFacade.GANTT_INDEX))) {
      date = myUIFacade.getTaskSelectionManager().getSelectedTasks().stream()
          .map(t -> t.getStart().getTime())
          .min(Comparator.naturalOrder()).orElse(myChart.getStartDate());
    } else {
      date = myChart.getStartDate();
    }

    myChart.setTopUnit(getTopTimeUnit());
    myChart.setBottomUnit(getBottomTimeUnit());
    myChart.setBottomUnitWidth(getBottomUnitWidth());
    myChart.setStartDate(date == null ? new Date() : date);
  }

  public int getBottomUnitWidth() {
    return getCurrentZoomState().getBottomUnitWidth();
  }

  public TimeUnit getTopTimeUnit() {
    return getCurrentZoomState().getTimeUnitPair().getTopTimeUnit();
  }

  public TimeUnit getBottomTimeUnit() {
    return getCurrentZoomState().getTimeUnitPair().getBottomTimeUnit();
  }

  public ZoomManager.ZoomState getCurrentZoomState() {
    return myCurrentZoomState;
  }
}
