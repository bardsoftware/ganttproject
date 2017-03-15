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

public interface TimelineChart extends Chart {
  interface VScrollController {
    boolean isScrollable();

    void scrollBy(int pixels);
  }

  void setBottomUnitWidth(int width);

  void setTopUnit(TimeUnit topUnit);

  void setBottomUnit(TimeUnit bottomUnit);

  // void paintChart(Graphics g);

  void addRenderer(ChartRendererBase renderer);

  void resetRenderers();

  /**
   * Scrolls the chart by a number of days
   *
   * @param days
   *          are the number of days to scroll. If days < 0 it scrolls to the
   *          right otherwise to the left.
   */
  public void scrollBy(TimeDuration duration);

  void setVScrollController(VScrollController vscrollController);

  ChartModel getModel();

  ChartUIConfiguration getStyle();

  void setStartOffset(int pixels);

  void setTimelineHeight(int height);

}
