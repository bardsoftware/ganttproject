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

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Date;
import java.util.List;

import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.TimeUnitStack;

import net.sourceforge.ganttproject.chart.ChartModelBase.OptionEventDispatcher;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * @author dbarashev
 */
public interface ChartModel {
  ChartHeader getChartHeader();

  void setBounds(Dimension bounds);

  Dimension getBounds();

  // Dimension getMaxBounds();
  void setStartDate(Date startDate);

  /**
   * This method calculates the end date of this chart. It is a function of
   * (start date, bounds, bottom time unit, top time unit, bottom unit width) so
   * it expects that all these parameters are set correctly.
   */
  Date getEndDate();

  Date getStartDate();

  void setBottomUnitWidth(int pixelsWidth);

  int getBottomUnitWidth();

  void setRowHeight(int rowHeight);

  void setTopTimeUnit(TimeUnit topTimeUnit);

  void setBottomTimeUnit(TimeUnit bottomTimeUnit);

  public TimeUnit getBottomUnit();

  void setVisibleTasks(List<Task> visibleTasks);

  void paint(Graphics g);

  void setVerticalOffset(int i);

  ChartUIConfiguration getChartUIConfiguration();

  void addRenderer(ChartRendererBase renderer);

  List<Offset> getTopUnitOffsets();

  List<Offset> getBottomUnitOffsets();

  List<Offset> getDefaultUnitOffsets();

  Offset getOffsetAt(int x);

  TaskManager getTaskManager();

  TimeUnitStack getTimeUnitStack();

  OptionEventDispatcher getOptionEventDispatcher();
}
