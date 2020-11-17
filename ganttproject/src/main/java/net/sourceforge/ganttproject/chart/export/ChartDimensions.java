/*
GanttProject is an opensource project management tool. License: GPL3
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
package net.sourceforge.ganttproject.chart.export;

import net.sourceforge.ganttproject.GPTreeTableBase;
import net.sourceforge.ganttproject.GanttExportSettings;

public class ChartDimensions {
  private final int logoHeight;
  private final int treeHeight;
  private final int tableHeaderHeight;
  private final int treeWidth;
  private int chartWidth;

  ChartDimensions(GanttExportSettings settings, GPTreeTableBase treeTable) {
    logoHeight = settings.getLogo().getHeight(null);
    treeHeight = treeTable.getRowHeight() * (settings.getRowCount());
    tableHeaderHeight = treeTable.getTable().getTableHeader().getHeight();
    treeWidth = treeTable.getTable().getWidth();
  }

  public int getChartHeight() {
    return treeHeight + tableHeaderHeight + logoHeight;
  }

  public void setChartWidth(int chartWidth) {
    this.chartWidth = chartWidth;
  }

  public int getChartWidth() {
    return chartWidth;
  }

  public int getTreeWidth() {
    return treeWidth;
  }

  public int getLogoHeight() {
    return logoHeight;
  }

  public int getTableHeaderHeight() {
    return tableHeaderHeight;
  }

  public int getFullWidth() {
    return chartWidth + treeWidth;
  }
}