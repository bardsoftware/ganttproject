/*
Copyright 2026

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
package net.sourceforge.ganttproject.chart.item;

import net.sourceforge.ganttproject.chart.gantt.HiddenTaskGap;

/**
 * What the pointer is over when it rests on a HIDDEN-TASKS MARKER.
 *
 * ITS TASK IS NULL, and that is not an omission. A gap stands for tasks the view was asked to leave
 * out; naming one of them here would put it into the tooltip, into the status bar and into every
 * other place that asks a chart item what task it belongs to. There is no task under this mark —
 * that is the whole statement it makes.
 *
 * `MouseMotionListenerImpl` therefore finds it in the branch for „no task under the pointer", right
 * beside the calendar events, which are the other thing on this chart that is not a task.
 */
public class HiddenGapChartItem extends ChartItem {
  private final HiddenTaskGap myGap;

  public HiddenGapChartItem(HiddenTaskGap gap) {
    super(null);
    myGap = gap;
  }

  public HiddenTaskGap getGap() {
    return myGap;
  }
}
