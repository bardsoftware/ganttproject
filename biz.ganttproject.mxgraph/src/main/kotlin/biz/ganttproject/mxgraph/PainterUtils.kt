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
import biz.ganttproject.core.chart.canvas.TextMetrics
import biz.ganttproject.core.chart.render.Style
import com.mxgraph.util.mxConstants
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font

internal fun Canvas.Shape.hexForegroundColor() = foregroundColor?.toHexString() ?: Color.BLACK.toHexString()

internal fun Style.hexBackgroundColor(shape: Canvas.Shape) = getBackgroundColor(shape)?.get()?.toHexString()

internal fun Style.hexStrokeColor(shape: Canvas.Shape) = getForegroundColor(shape)?.get()?.toHexString() ?: hexBordersColor(shape)

internal fun Style.hexBordersColor(shape: Canvas.Shape) = getBorder(shape)?.top?.color?.toHexString()

internal fun Color.toHexString() = "#" + Integer.toHexString(rgb and 0x00ffffff).padStart(6, '0')

/** The stroke which the chart style prescribes for the borders of [shape], if it defines any. */
internal fun Style.borderStroke(shape: Canvas.Shape): BasicStroke? = getBorder(shape)?.top?.stroke

/**
 * The width of this stroke as an mxGraph style map, empty if the chart style defines no border
 * and thus says nothing about the width.
 */
internal fun BasicStroke?.toMxStrokeWidth(): Map<String, Any> =
    if (this == null) emptyMap() else mapOf(mxConstants.STYLE_STROKEWIDTH to lineWidth)

/**
 * The width and the dashing of this stroke as an mxGraph style map. The dash pattern is written
 * as the space-separated lengths of the dashes and the gaps, the way mxGraph expects it, so that
 * the client reproduces the very dashes of the desktop chart rather than some default ones.
 */
internal fun BasicStroke?.toMxLineStroke(): Map<String, Any> = buildMap {
  putAll(toMxStrokeWidth())
  val dashArray = this@toMxLineStroke?.dashArray
  put(mxConstants.STYLE_DASHED, if (dashArray == null) 0 else 1)
  if (dashArray != null) {
    put(mxConstants.STYLE_DASH_PATTERN, dashArray.joinToString(" ") { it.toString() })
  }
}

internal fun Canvas.HAlignment.toMxAlignment() = when(this) {
  Canvas.HAlignment.CENTER -> mxConstants.ALIGN_CENTER
  Canvas.HAlignment.LEFT -> mxConstants.ALIGN_LEFT
  Canvas.HAlignment.RIGHT -> mxConstants.ALIGN_RIGHT
}

internal fun Canvas.VAlignment.toMxAlignment() = when(this) {
  Canvas.VAlignment.CENTER -> mxConstants.ALIGN_MIDDLE
  Canvas.VAlignment.TOP -> mxConstants.ALIGN_TOP
  Canvas.VAlignment.BOTTOM -> mxConstants.ALIGN_BOTTOM
}

/**
 * We can't calculate any precise text metrics when rendering SVG on the server side,
 * so we provide some stubs here.
 */
object TextMetricsStub : TextMetrics {
  override fun getTextLength(text: String) = text.length * 7

  override fun getTextHeight(text: String) = 10

  override fun getTextHeight(font: Font, text: String) = font.size

  override fun getState() = Object()
}
