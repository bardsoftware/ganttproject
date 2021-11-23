/*
Copyright 2011-2021 BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.chart.export

import biz.ganttproject.core.chart.grid.OffsetList
import net.sourceforge.ganttproject.GanttExportSettings
import net.sourceforge.ganttproject.chart.ChartModelBase
import java.awt.Dimension

class ChartImageBuilder(
  private val mySettings: GanttExportSettings,
  private val myChartModel: ChartModelBase,
  private val myTreeTable: TreeTableApi
) {
  private val myDimensions: ChartDimensions = ChartDimensions(mySettings, myTreeTable)

  fun buildImage(visitor: ChartImageVisitor) {
    if (mySettings.zoomLevel != null) {
      myChartModel.setBottomTimeUnit(mySettings.zoomLevel.timeUnitPair.bottomTimeUnit)
      myChartModel.setTopTimeUnit(mySettings.zoomLevel.timeUnitPair.topTimeUnit)
      myChartModel.bottomUnitWidth = mySettings.zoomLevel.bottomUnitWidth
    }
    val factory = myChartModel.createOffsetBuilderFactory()
      .withViewportStartDate(mySettings.startDate)
      .withStartDate(myChartModel.bottomUnit.jumpLeft(mySettings.startDate))
      .withEndDate(mySettings.endDate)
      .withEndOffset(if (mySettings.width < 0) Int.MAX_VALUE else mySettings.width)
    val offsetBuilder = factory.build()
    val bottomOffsets = OffsetList()
    offsetBuilder.constructOffsets(null, bottomOffsets)
    myDimensions.chartWidth = bottomOffsets.endPx
    myChartModel.startDate = mySettings.startDate
    myChartModel.bounds = Dimension(myDimensions.chartWidth, myDimensions.chartHeight)
    myChartModel.setHeaderHeight(myDimensions.logoHeight + myDimensions.tableHeaderHeight - 1)
    myChartModel.setRowHeight(myTreeTable.rowHeight())
    myChartModel.verticalOffset = myTreeTable.verticalOffset()
    myChartModel.setVisibleTasks(mySettings.visibleTasks)
    visitor.acceptLogo(myDimensions, mySettings.logo)
    visitor.acceptTable(myDimensions, myTreeTable)
    visitor.acceptChart(myDimensions, myChartModel)
  }

}
