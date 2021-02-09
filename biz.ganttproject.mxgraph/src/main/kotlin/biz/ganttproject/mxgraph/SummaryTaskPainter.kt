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
import biz.ganttproject.core.chart.render.Style
import com.mxgraph.util.mxConstants
import java.util.*

internal class SummaryTaskPainter(
    private val mxPainterImpl: MxPainterImpl,
    private val props: Properties
) : MxGraphPainter.RectanglePainter {

  override fun paint(rectangle: Canvas.Rectangle) {
    val style = Style.getStyle(props, rectangle.style)
    val mxStyle = mapOf(
      mxConstants.STYLE_FILLCOLOR to style.getBackgroundColor(rectangle),
      mxConstants.STYLE_STROKECOLOR to mxConstants.NONE
    )

    with(style.padding) {
      mxPainterImpl.paintRectangle(
        rectangle.leftX + left, rectangle.topY + top,
        rectangle.width - (left + right), rectangle.height - (top + bottom),
        mxStyle, rectangle.attributes
      )

      val notchWidth = rectangle.height - (top + bottom)
      if (rectangle.hasStyle("task.summary.open")) {
        mxPainterImpl.paintRectangle(
          rectangle.leftX, rectangle.topY, notchWidth, rectangle.height,
          mxStyle, emptyMap()
        )
      }
      if (rectangle.hasStyle("task.summary.close")) {
        mxPainterImpl.paintRectangle(
          rectangle.rightX - notchWidth, rectangle.topY, notchWidth, rectangle.height,
          mxStyle, emptyMap()
        )
      }
    }
  }
}
