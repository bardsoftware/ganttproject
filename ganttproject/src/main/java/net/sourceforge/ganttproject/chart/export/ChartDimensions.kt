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

import net.sourceforge.ganttproject.GanttExportSettings

class ChartDimensions internal constructor(settings: GanttExportSettings, treeTable: TreeTableApi) {
  val logoHeight: Int = settings.logo.getHeight(null)
  val treeHeight: Int = treeTable.rowHeight() * settings.rowCount
  val tableHeaderHeight: Int = treeTable.tableHeaderHeight()
  val treeWidth: Int = treeTable.width(settings.isCommandLineMode)
  var chartWidth = 0
  val chartHeight: Int
    get() = treeHeight + tableHeaderHeight + logoHeight
  val fullWidth: Int
    get() = chartWidth + treeWidth

}
