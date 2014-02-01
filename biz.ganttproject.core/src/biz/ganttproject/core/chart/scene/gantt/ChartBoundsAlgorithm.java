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
package biz.ganttproject.core.chart.scene.gantt;

import java.util.Date;

import biz.ganttproject.core.chart.scene.BarChartActivity;

public class ChartBoundsAlgorithm {
  public static class Result {
    public final Date lowerBound;

    public final Date upperBound;

    private Result(Date lowerBound, Date upperBound) {
      this.lowerBound = lowerBound;
      this.upperBound = upperBound;
    }
  }

  public Result getBounds(Iterable<BarChartActivity<?>> activities) {
    Date lowerBound = null;
    Date upperBound = null;
    for (BarChartActivity<?> activity : activities) {
      Date start = activity.getStart();
      Date end = activity.getEnd();
      if (lowerBound == null || lowerBound.after(start)) {
        lowerBound = start;
      }
      if (upperBound == null || upperBound.before(end)) {
        upperBound = end;
      }
    }
    return new Result(lowerBound, upperBound);
  }
}
