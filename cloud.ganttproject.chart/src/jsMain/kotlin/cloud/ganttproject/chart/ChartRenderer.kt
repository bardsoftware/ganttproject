/*
Copyright 2026 BarD Software s.r.o

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
package cloud.ganttproject.chart

import org.w3c.dom.ALPHABETIC
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.CENTER
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.LEFT
import org.w3c.dom.RIGHT
import kotlin.math.sqrt

/** The mxGraph `none` sentinel: the corresponding fill or stroke must not be painted. */
private const val MX_NONE = "none"

/** The mxGraph horizontal alignment values, see mxConstants.ALIGN_*. */
private const val MX_ALIGN_CENTER = "center"
private const val MX_ALIGN_RIGHT = "right"

/** The mxGraph vertical alignment values, see mxConstants.ALIGN_*. */
private const val MX_ALIGN_TOP = "top"
private const val MX_ALIGN_MIDDLE = "middle"

/** Font fallbacks for the texts whose style says nothing about the font. */
private const val DEFAULT_FONT_SIZE = 10.0
private const val DEFAULT_FONT_FAMILY = "sans-serif"

/** Text color fallback, see the TextStyle.fontColor contract in chart_model.ts. */
private const val DEFAULT_TEXT_COLOR = "#000000"

/** Descent fallback, as a fraction of the font size, if the canvas reports no font metrics. */
private const val DESCENT_RATIO = 0.2

/** The mxGraph `classic` arrow head shape. */
private const val MX_ARROW_CLASSIC = "classic"

/** Line color fallback, see the LineStyle.strokeColor contract in chart_model.ts. */
private const val DEFAULT_LINE_COLOR = "#000000"

/** The mxGraph default dash pattern is "3 3". */
private val DASH_PATTERN = arrayOf(3.0, 3.0)

/** Dimensions of the `classic` arrow head, in pixels. */
private const val ARROW_LENGTH = 10.0
private const val ARROW_HALF_WIDTH = 4.0

/**
 * Renders the chart [model] onto the 2D context of the [canvas] element.
 * The canvas is cleared first, so repeated calls do not stack the painting.
 *
 * The primitives are drawn in the model order, which is the order in which the canvas emitted
 * them on the server side, so that the shapes overlap the same way as in the desktop chart.
 * The primitive kinds which the renderer does not support yet (e.g. text groups, should they
 * ever be emitted as a kind of their own) are skipped.
 */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
fun drawChart(model: ChartModel, canvas: HTMLCanvasElement) {
  val ctx = canvas.getContext("2d") as? CanvasRenderingContext2D ?: return
  ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
  model.primitives.forEach { primitive ->
    when (primitive.type) {
      PrimitiveType.RECTANGLE -> drawRectangle(ctx, primitive.unsafeCast<RectanglePrimitive>())
      PrimitiveType.LINE -> drawLine(ctx, primitive.unsafeCast<LinePrimitive>())
      PrimitiveType.RHOMBUS -> drawRhombus(ctx, primitive.unsafeCast<RhombusPrimitive>())
      PrimitiveType.TEXT -> drawText(ctx, primitive.unsafeCast<TextPrimitive>())
      else -> {}
    }
  }
}

private fun drawRectangle(ctx: CanvasRenderingContext2D, rect: RectanglePrimitive) =
  ctx.withShapeStyle(rect.style) {
    rect.style.fillColor.paintOrNull()?.let { fill ->
      ctx.fillStyle = fill
      ctx.fillRect(rect.x, rect.y, rect.width, rect.height)
    }
    rect.style.strokeColor.paintOrNull()?.let { stroke ->
      ctx.strokeStyle = stroke
      ctx.strokeRect(rect.x, rect.y, rect.width, rect.height)
    }
  }

private fun drawRhombus(ctx: CanvasRenderingContext2D, rhombus: RhombusPrimitive) =
  ctx.withShapeStyle(rhombus.style) {
    ctx.beginPath()
    ctx.moveTo(rhombus.x + rhombus.width / 2, rhombus.y)
    ctx.lineTo(rhombus.x + rhombus.width, rhombus.y + rhombus.height / 2)
    ctx.lineTo(rhombus.x + rhombus.width / 2, rhombus.y + rhombus.height)
    ctx.lineTo(rhombus.x, rhombus.y + rhombus.height / 2)
    ctx.closePath()
    rhombus.style.fillColor.paintOrNull()?.let { fill ->
      ctx.fillStyle = fill
      ctx.fill()
    }
    rhombus.style.strokeColor.paintOrNull()?.let { stroke ->
      ctx.strokeStyle = stroke
      ctx.stroke()
    }
  }

private fun drawLine(ctx: CanvasRenderingContext2D, line: LinePrimitive) =
  ctx.withOpacity(line.style.opacity) {
    val stroke = line.style.strokeColor.paintOrNull() ?: DEFAULT_LINE_COLOR
    ctx.strokeStyle = stroke
    ctx.fillStyle = stroke
    line.style.strokeWidth?.let { ctx.lineWidth = it }
    if (line.style.dashed == 1.0) {
      ctx.setLineDash(DASH_PATTERN)
    }
    ctx.beginPath()
    ctx.moveTo(line.startX, line.startY)
    ctx.lineTo(line.finishX, line.finishY)
    ctx.stroke()
    if (line.style.endArrow == MX_ARROW_CLASSIC) {
      drawArrowHead(ctx, line)
    }
  }

/**
 * Draws the label of [text] with its font and color, placing it relative to the primitive
 * anchor point according to the alignment and the spacings of its style.
 *
 * The placement formulas are the ones of TextPainter (biz.ganttproject.core), the renderer of
 * the desktop chart, so that the same model produces the same layout in the browser.
 */
private fun drawText(ctx: CanvasRenderingContext2D, text: TextPrimitive) =
  ctx.withOpacity(text.style.textOpacity) {
    val label = text.text
    if (label.isNullOrEmpty()) {
      return@withOpacity
    }
    val style = text.style
    val fontSize = style.fontSize ?: DEFAULT_FONT_SIZE
    ctx.font = "${fontSize}px ${style.fontFamily ?: DEFAULT_FONT_FAMILY}"
    ctx.fillStyle = style.fontColor.paintOrNull() ?: DEFAULT_TEXT_COLOR
    ctx.textBaseline = CanvasTextBaseline.ALPHABETIC

    val spacingTop = style.spacingTop ?: 0.0
    val spacingBottom = style.spacingBottom ?: 0.0
    val spacingLeft = style.spacingLeft ?: 0.0
    val spacingRight = style.spacingRight ?: 0.0
    // The vertical padding, matching Padding.getY() on the desktop side.
    val spacingY = spacingTop + spacingBottom

    // The canvas alignment does the same shifting by the label width as TextPainter does with
    // the measured length, so only the paddings are left to apply to the anchor point.
    val x = when (style.align) {
      MX_ALIGN_CENTER -> {
        ctx.textAlign = CanvasTextAlign.CENTER
        text.x + (spacingLeft - spacingRight) / 2
      }
      MX_ALIGN_RIGHT -> {
        ctx.textAlign = CanvasTextAlign.RIGHT
        text.x - spacingRight
      }
      else -> {
        ctx.textAlign = CanvasTextAlign.LEFT
        text.x + spacingLeft
      }
    }
    val baselineY = when (style.verticalAlign) {
      MX_ALIGN_TOP -> text.y + fontSize + spacingY + spacingTop
      MX_ALIGN_MIDDLE -> text.y + (fontSize + spacingY) / 2 - spacingBottom
      // Bottom alignment, the default: the anchor point is the bottom of the label box, so the
      // baseline goes up by the descent of the font.
      else -> text.y - spacingBottom - ctx.fontDescent(fontSize)
    }
    ctx.fillText(label, x, baselineY)
  }

/**
 * Returns the descent of the current font, falling back to a fraction of [fontSize] if the
 * browser reports no font bounding box in the text metrics.
 */
private fun CanvasRenderingContext2D.fontDescent(fontSize: Double): Double =
  measureText("Hg").asDynamic().fontBoundingBoxDescent as? Double ?: (fontSize * DESCENT_RATIO)

/** Draws a filled triangular arrow head at the finish point of [line], using the current fill style. */
private fun drawArrowHead(ctx: CanvasRenderingContext2D, line: LinePrimitive) {
  val dx = line.finishX - line.startX
  val dy = line.finishY - line.startY
  val length = sqrt(dx * dx + dy * dy)
  if (length == 0.0) return
  // Unit vector along the line and its normal.
  val ux = dx / length
  val uy = dy / length
  val nx = -uy
  val ny = ux
  val baseX = line.finishX - ux * ARROW_LENGTH
  val baseY = line.finishY - uy * ARROW_LENGTH
  ctx.beginPath()
  ctx.moveTo(line.finishX, line.finishY)
  ctx.lineTo(baseX + nx * ARROW_HALF_WIDTH, baseY + ny * ARROW_HALF_WIDTH)
  ctx.lineTo(baseX - nx * ARROW_HALF_WIDTH, baseY - ny * ARROW_HALF_WIDTH)
  ctx.closePath()
  ctx.fill()
}

/**
 * Runs [block] with the opacity, stroke width and dash style applied, restoring the
 * previous context state afterwards.
 */
private inline fun CanvasRenderingContext2D.withShapeStyle(
  style: ShapeStyle,
  block: () -> Unit
) = withOpacity(style.opacity) {
  style.strokeWidth?.let { lineWidth = it }
  block()
}

private inline fun CanvasRenderingContext2D.withOpacity(opacity: Double?, block: () -> Unit) {
  save()
  globalAlpha = ((opacity ?: 100.0) / 100.0).coerceIn(0.0, 1.0)
  block()
  restore()
}

/** Returns the color to paint with, or null if the value is missing or is the mxGraph `none` sentinel. */
private fun String?.paintOrNull(): String? = this?.takeIf { it.isNotBlank() && it != MX_NONE }
