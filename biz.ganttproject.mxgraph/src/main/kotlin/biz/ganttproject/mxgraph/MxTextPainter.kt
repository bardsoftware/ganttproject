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

import biz.ganttproject.core.chart.canvas.Canvas.Text
import biz.ganttproject.core.chart.canvas.Canvas.Label
import biz.ganttproject.core.chart.canvas.TextMetrics
import biz.ganttproject.core.chart.render.AbstractTextPainter
import biz.ganttproject.core.chart.render.Style
import com.mxgraph.util.mxConstants
import java.awt.Color
import java.awt.Font
import java.util.*

internal class MxTextPainter(
    private val mxPainterImpl: MxPainterImpl,
    private val properties: Properties,
    baseFont: () -> Font
) : AbstractTextPainter(properties, baseFont) {

  override fun paint(text: Text) {
    paint(text, text.textSelector.getLabels(TextMetricsStub).firstOrNull(), text.leftX, text.bottomY, emptyMap())
  }

  override fun paint(text: Text, label: Label?, x: Int, y: Int, styles: Map<String, Any>) {
    if (label == null) {
      return
    }
    val chartStyle = Style.getStyle(properties, text.style)
    val style = mapOf(
        mxConstants.STYLE_VERTICAL_ALIGN to text.vAlignment.toMxAlignment(),
        mxConstants.STYLE_ALIGN to text.hAlignment.toMxAlignment(),
        mxConstants.STYLE_TEXT_OPACITY to (text.opacity ?: 1f) * 100,
        mxConstants.STYLE_FONTCOLOR to text.hexForegroundColor(),
        mxConstants.STYLE_SPACING_BOTTOM to chartStyle.padding.bottom,
        mxConstants.STYLE_SPACING_TOP to chartStyle.padding.top,
        mxConstants.STYLE_SPACING_LEFT to chartStyle.padding.left,
        mxConstants.STYLE_SPACING_RIGHT to chartStyle.padding.right
    )
    text.attributes["text"] = label.text
    mxPainterImpl.paintText(x, y, text.attributes, styles + style)
  }

  override fun getFontStyles(font: Font, color: Color) = mapOf(
      mxConstants.STYLE_FONTSIZE to font.size,
      mxConstants.STYLE_FONTCOLOR to color.toHexString(),
      mxConstants.STYLE_FONTFAMILY to font.name
  )

  override fun getTextMetrics(): TextMetrics = TextMetricsStub
  override fun getTextMetrics(styles: Map<String, Any>) = TextMetricsStub
}
