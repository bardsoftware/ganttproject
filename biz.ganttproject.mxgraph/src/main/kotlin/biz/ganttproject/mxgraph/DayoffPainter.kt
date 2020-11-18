/*
Copyright 2020 Dmitry Kazakov, BarD Software s.r.o

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
package biz.ganttproject.mxgraph

import biz.ganttproject.core.chart.canvas.Canvas
import com.mxgraph.util.mxConstants
import net.sourceforge.ganttproject.chart.ChartUIConfiguration
import java.awt.Color

internal class DayoffPainter(
    private val mxPainterImpl: MxPainterImpl,
    private val uiConfig: ChartUIConfiguration
) : MxGraphPainter.RectanglePainter {

  override fun paint(rectangle: Canvas.Rectangle) {
    val margin = uiConfig.margin - 3
    val color = uiConfig.dayOffColor
    val mxStyle = mapOf(
        mxConstants.STYLE_FILLCOLOR to color.toHexString(),
        mxConstants.STYLE_STROKECOLOR to Color.BLACK.toHexString(),
        mxConstants.STYLE_OPACITY to 40
    )
    mxPainterImpl.paintRectangle(
        rectangle.leftX, rectangle.topY + margin, rectangle.width, rectangle.height - 2 * margin,
        mxStyle, rectangle.attributes
    )
  }

}
