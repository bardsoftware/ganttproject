/*
Copyright 2021 BarD Software s.r.o

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

import net.sourceforge.ganttproject.GPTreeTableBase
import java.awt.Component
import java.awt.Graphics2D

/**
 * This is an interface of the tree table component for the chart export purposes.
 *
 * @author dbarashev@bardsoftware.com
 */
data class TreeTableApi(
  val rowHeight: ()->Int,
  val tableHeaderHeight: ()->Int,
  val width: (fullWidthNotViewport: Boolean)->Int,
  val tableHeaderComponent: ()-> Component?,
  val tableComponent: () -> Component?,
  val tablePainter: ((Graphics2D) -> Unit)? = null,
  val verticalOffset: ()->Int = { 0 }
)

/**
 * Converts GPTreeTableBase instance to TreeTableApi
 */
fun GPTreeTableBase.asTreeTableApi(): TreeTableApi = TreeTableApi(
  rowHeight = { this.rowHeight },
  tableHeaderHeight = { this.tableHeader.height },
  width = { this.width },
  tableHeaderComponent = { this.tableHeader },
  tableComponent = { this.table },
  verticalOffset = { -1 }
).also {
  this.expandAll()
}
