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

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.chart.canvas.Canvas;

/**
 * @author bard
 */
public class ChartRendererBase {
  private int myHeight;

  private ChartModel myChartModel;

  private final Canvas myPrimitiveContainer;

  private boolean isEnabled = true;

  protected ChartRendererBase() {
    myPrimitiveContainer = new Canvas();
  }

  public ChartRendererBase(ChartModel model) {
    this();
    myChartModel = model;
  }

  public ChartRendererBase(ChartModelBase chartModel, Canvas canvas) {
    myChartModel = chartModel;
    myPrimitiveContainer = canvas;
  }

  public void setHeight(int height) {
    myHeight = height;
  }

  protected int getHeight() {
    return myHeight;
  }

  protected int getWidth() {
    return (int) getChartModel().getBounds().getWidth();
  }

  protected ChartUIConfiguration getConfig() {
    return getChartModel().getChartUIConfiguration();
  }

  public Canvas getPrimitiveContainer() {
    return myPrimitiveContainer;
  }

  protected ChartModel getChartModel() {
    return myChartModel;
  }

  protected GPCalendar getCalendar() {
    return myChartModel.getTaskManager().getCalendar();
  }

  public boolean isEnabled() {
    return isEnabled;
  }

  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  public void clear() {
    getPrimitiveContainer().clear();
  }

  public void render() {
  }
}
