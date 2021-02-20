package biz.ganttproject.mxgraph

import biz.ganttproject.core.chart.canvas.Canvas
import com.mxgraph.util.mxConstants
import java.awt.Color

internal class ColouredRectanglePainter(
  private val mxPainterImpl: MxPainterImpl,
  private val color: Color
) : MxGraphPainter.RectanglePainter  {
  override fun paint(rectangle: Canvas.Rectangle) {
    mxPainterImpl.paintRectangle(
      rectangle.leftX, rectangle.topY,
      rectangle.width, rectangle.height,
      mapOf(mxConstants.STYLE_FILLCOLOR to color), rectangle.attributes
    )
  }
}
