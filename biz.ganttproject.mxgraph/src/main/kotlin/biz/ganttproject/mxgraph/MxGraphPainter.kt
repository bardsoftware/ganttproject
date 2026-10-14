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

import biz.ganttproject.core.chart.canvas.Canvas.*
import biz.ganttproject.core.chart.canvas.Painter
import biz.ganttproject.core.chart.render.Style
import com.mxgraph.util.mxConstants
import net.sourceforge.ganttproject.chart.ChartUIConfiguration
import net.sourceforge.ganttproject.font.Fonts
import net.sourceforge.ganttproject.util.PropertiesUtil
import java.awt.Color
import java.util.*

/**
 * This class exports GanttProject's chart canvas to a serializable model which can be rendered
 * on the client side.
 *
 * This code mostly prepares styles for painting and delegates the real work of accumulating and
 * serializing the primitives to a [PainterImpl] instance passed to the constructor. Depending on
 * the implementation, the same painting logic produces either an mxGraph model ([MxPainterImpl],
 * serialized to XML) or a JSON model ([JsonPainterImpl]). The result is obtained from the
 * [PainterImpl] instance.
 */
class MxGraphPainter(uiConfig: ChartUIConfiguration, private val painter: PainterImpl) : Painter {
  val chartProperties = Properties().also {
    PropertiesUtil.loadProperties(it, "/resources/chart.properties")
  }
  private val containerRectanglePainter = SummaryTaskPainter(painter, chartProperties)
  private val resourceLoadPainter = ResourceLoadPainter(painter, uiConfig)
  private val dayoffPainter = DayoffPainter(painter, uiConfig)
  private val textPainter = MxTextPainter(painter, chartProperties) { Fonts.DEFAULT_CHART_FONT }
  private val styleToPainter = mapOf(
      "task.progress" to ColouredRectanglePainter(painter, Color.BLACK),
      "task.progress.end" to ColouredRectanglePainter(painter, Color.BLACK),
      "task.supertask" to containerRectanglePainter,
      "load.normal" to resourceLoadPainter,
      "load.normal.first" to resourceLoadPainter,
      "load.normal.last" to resourceLoadPainter,
      "load.normal.first.last" to resourceLoadPainter,
      "load.overload" to resourceLoadPainter,
      "load.overload.first" to resourceLoadPainter,
      "load.overload.last" to resourceLoadPainter,
      "load.overload.first.last" to resourceLoadPainter,
      "load.underload" to resourceLoadPainter,
      "load.underload.first" to resourceLoadPainter,
      "load.underload.last" to resourceLoadPainter,
      "load.underload.first.last" to resourceLoadPainter,
      "dayoff" to dayoffPainter
  )

  override fun prePaint() {}

  fun paint(paintFunction: () -> Unit) {
    painter.clear()
    painter.beginUpdate()
    try {
      paintFunction.invoke()
    } catch (exception: Exception) {
      throw exception
    } finally {
      painter.endUpdate()
    }
  }

  override fun paint(rectangle: Rectangle) {
    val rectanglePainter = styleToPainter[rectangle.style]
    if (rectanglePainter != null) {
      rectanglePainter.paint(rectangle)
      return
    }

    val chartStyle = Style.getStyle(chartProperties, rectangle.style)
    val mxStyle = mapOf(
        mxConstants.STYLE_FILLCOLOR to (chartStyle.hexBackgroundColor(rectangle) ?: mxConstants.NONE),
        mxConstants.STYLE_STROKECOLOR to (chartStyle.hexStrokeColor(rectangle) ?: mxConstants.NONE),
        mxConstants.STYLE_OPACITY to (rectangle.opacity ?: 1f) * 100
    ) + chartStyle.borderStroke(rectangle).toMxStrokeWidth()
    painter.paintRectangle(rectangle.leftX, rectangle.topY, rectangle.width, rectangle.height, mxStyle, rectangle.attributes)
  }

  override fun paint(line: Line) {
    val chartStyle = Style.getStyle(chartProperties, line.style)
    val style = mapOf(
        mxConstants.STYLE_ENDARROW to
            if (line.arrow.length == 0 && line.arrow.width == 0) mxConstants.NONE else mxConstants.ARROW_CLASSIC,
        mxConstants.STYLE_STROKECOLOR to (chartStyle.hexStrokeColor(line) ?: Color.BLACK.toHexString()),
        mxConstants.STYLE_OPACITY to (line.opacity ?: 1f) * 100
    ) + chartStyle.borderStroke(line).toMxLineStroke()
    painter.paintLine(line.startX, line.startY, line.finishX, line.finishY, style, line.attributes)
  }

  override fun paint(text: Text) = textPainter.paint(text)

  override fun paint(textGroup: TextGroup) = textPainter.paint(textGroup)

  override fun paint(rhombus: Rhombus) {
    val chartStyle = Style.getStyle(chartProperties, rhombus.style)
    val style = mapOf(
        mxConstants.STYLE_FILLCOLOR to chartStyle.hexBackgroundColor(rhombus),
        mxConstants.STYLE_STROKECOLOR to chartStyle.hexBordersColor(rhombus),
        mxConstants.STYLE_OPACITY to (rhombus.opacity ?: 1f) * 100
    ) + chartStyle.borderStroke(rhombus).toMxStrokeWidth()
    painter.paintRhombus(rhombus.leftX, rhombus.topY, rhombus.width, rhombus.height, style, rhombus.attributes)
  }

  internal interface RectanglePainter {
    fun paint(rectangle: Rectangle)
  }
}
