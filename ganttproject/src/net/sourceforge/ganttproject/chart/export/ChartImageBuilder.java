/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

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

import java.awt.Dimension;

import biz.ganttproject.core.chart.grid.OffsetBuilder;
import biz.ganttproject.core.chart.grid.OffsetList;

import net.sourceforge.ganttproject.GPTreeTableBase;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.chart.ChartModelBase;

public class ChartImageBuilder {
  private final ChartModelBase myChartModel;
  private final GanttExportSettings mySettings;
  private final GPTreeTableBase myTreeTable;
  private final ChartDimensions myDimensions;

  public ChartImageBuilder(GanttExportSettings settings, ChartModelBase chartModel, GPTreeTableBase treeTable) {
    myChartModel = chartModel;
    mySettings = settings;
    myTreeTable = treeTable;
    myDimensions = new ChartDimensions(settings, treeTable);
  }

  public void buildImage(ChartImageVisitor visitor) {
    if (mySettings.getZoomLevel() != null) {
      myChartModel.setBottomTimeUnit(mySettings.getZoomLevel().getTimeUnitPair().getBottomTimeUnit());
      myChartModel.setTopTimeUnit(mySettings.getZoomLevel().getTimeUnitPair().getTopTimeUnit());
      myChartModel.setBottomUnitWidth(mySettings.getZoomLevel().getBottomUnitWidth());
    }
    OffsetBuilder.Factory factory = myChartModel.createOffsetBuilderFactory()
        .withViewportStartDate(mySettings.getStartDate())
        .withStartDate(myChartModel.getBottomUnit().jumpLeft(mySettings.getStartDate()))
        .withEndDate(mySettings.getEndDate())
        .withEndOffset(mySettings.getWidth() < 0 ? Integer.MAX_VALUE : mySettings.getWidth());

    OffsetBuilder offsetBuilder = factory.build();
    OffsetList bottomOffsets = new OffsetList();
    offsetBuilder.constructOffsets(null, bottomOffsets);
    myDimensions.setChartWidth(bottomOffsets.getEndPx());
    myChartModel.setStartDate(mySettings.getStartDate());
    myChartModel.setBounds(new Dimension(myDimensions.getChartWidth(), myDimensions.getChartHeight()));

    myChartModel.setHeaderHeight(myDimensions.getLogoHeight() + myDimensions.getTableHeaderHeight() - 1);
    myChartModel.setVisibleTasks(mySettings.getVisibleTasks());

    visitor.acceptLogo(myDimensions, mySettings.getLogo());
    visitor.acceptTable(myDimensions, myTreeTable.getTable().getTableHeader(), myTreeTable.getTable());

    visitor.acceptChart(myDimensions, myChartModel);
  }
}
