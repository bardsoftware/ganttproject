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

internal class ResourceLoadPainter(
    private val mxPainterImpl: MxPainterImpl,
    private val uiConfig: ChartUIConfiguration
) : MxGraphPainter.RectanglePainter {

  override fun paint(rectangle: Canvas.Rectangle) {
    val color = when {
      rectangle.style.indexOf("overload") > 0 -> uiConfig.resourceOverloadColor
      rectangle.style.indexOf("underload") > 0 -> uiConfig.resourceUnderLoadColor
      else -> uiConfig.resourceNormalLoadColor
    }.toHexString()
    mxPainterImpl.paintRectangle(
        rectangle.leftX, rectangle.topY + uiConfig.margin,
        rectangle.width, rectangle.height - 2 * uiConfig.margin,
        mapOf(mxConstants.STYLE_FILLCOLOR to color), rectangle.attributes
    )

    val mxLineStyle = mapOf(
        mxConstants.STYLE_ENDARROW to mxConstants.NONE,
        mxConstants.STYLE_STROKECOLOR to Color.BLACK.toHexString()
    )
    if (rectangle.style.indexOf(".first") > 0) {
      mxPainterImpl.paintLine(
          rectangle.leftX, rectangle.topY + uiConfig.margin,
          rectangle.leftX, rectangle.bottomY - uiConfig.margin,
          mxLineStyle, rectangle.attributes
      )
    }
    if (rectangle.style.indexOf(".last") > 0) {
      mxPainterImpl.paintLine(
          rectangle.rightX, rectangle.topY + uiConfig.margin,
          rectangle.rightX, rectangle.bottomY - uiConfig.margin,
          mxLineStyle, rectangle.attributes
      )
    }

    mxPainterImpl.paintLine(
        rectangle.leftX, rectangle.topY + uiConfig.margin,
        rectangle.rightX, rectangle.topY + uiConfig.margin,
        mxLineStyle, rectangle.attributes
    )
    mxPainterImpl.paintLine(
        rectangle.leftX, rectangle.bottomY - uiConfig.margin,
        rectangle.rightX, rectangle.bottomY - uiConfig.margin,
        mxLineStyle, rectangle.attributes
    )
  }
}
