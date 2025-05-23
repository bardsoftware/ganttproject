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

import biz.ganttproject.core.option.FontOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.IntegerOption;
import biz.ganttproject.print.PrintChartApi;
import net.sourceforge.ganttproject.IGanttProject;
import org.eclipse.core.runtime.IAdaptable;

import java.util.Date;

public interface Chart extends IAdaptable {
  IGanttProject getProject();

  void init(IGanttProject project, IntegerOption dpiOption, FontOption chartFontOption);

  Date getStartDate();

  void setStartDate(Date startDate);

  Date getEndDate();

  void setDimensions(int height, int width);

  String getName();

  /** Repaints the chart */
  void reset();

  GPOptionGroup[] getOptionGroups();

  void addSelectionListener(ChartSelectionListener listener);

  void removeSelectionListener(ChartSelectionListener listener);

  void focus();

  PrintChartApi asPrintChartApi();
}
