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

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.scene.AbstractSceneBuilder;

/**
 * @author bard
 */
public class ChartRendererBase extends AbstractSceneBuilder {
  private ChartModel myChartModel;

  private boolean isEnabled = true;

  protected ChartRendererBase() {
  }

  public ChartRendererBase(ChartModel model) {
    this();
    myChartModel = model;
  }

  public ChartRendererBase(ChartModelBase chartModel, Canvas canvas) {
    super(canvas);
    myChartModel = chartModel;
  }

  protected int getWidth() {
    return (int) getChartModel().getBounds().getWidth();
  }

  protected ChartUIConfiguration getConfig() {
    return getChartModel().getChartUIConfiguration();
  }

  public Canvas getPrimitiveContainer() {
    return getCanvas();
  }

  protected ChartModel getChartModel() {
    return myChartModel;
  }

  protected GPCalendarCalc getCalendar() {
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

  @Override
  public void build() {
    render();
  }

  public void render() {
  }
}
