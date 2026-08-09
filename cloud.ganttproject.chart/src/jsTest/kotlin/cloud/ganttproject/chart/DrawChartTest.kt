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

import kotlinx.browser.document
import org.khronos.webgl.get
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import kotlin.js.JSON
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies that [drawChart] renders the JSON chart model onto a canvas by reading back
 * the pixels of a real canvas 2D context in a headless browser. The models under test
 * are parsed from JSON, mirroring the way the library is used with the server-side model.
 */
class DrawChartTest {
  private fun newCanvas(): HTMLCanvasElement {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = 200
    canvas.height = 200
    return canvas
  }

  /** Builds a model from a JSON array of primitives, each carrying its "type" property. */
  private fun model(primitives: String = "[]"): ChartModel = JSON.parse(
    """{"primitives": $primitives}"""
  )

  /** The bounding box of the painted pixels, in canvas coordinates, both ends inclusive. */
  private data class InkBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

  /**
   * Returns the bounding box of everything painted on [canvas], or null if the canvas is empty.
   * Useful for the texts, whose exact glyph shapes we do not want to assert on.
   */
  private fun inkBounds(canvas: HTMLCanvasElement): InkBounds? {
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    val data = ctx.getImageData(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble()).data
    var left = canvas.width; var top = canvas.height; var right = -1; var bottom = -1
    for (y in 0 until canvas.height) {
      for (x in 0 until canvas.width) {
        if ((data[(y * canvas.width + x) * 4 + 3].toInt() and 0xFF) == 0) continue
        if (x < left) left = x
        if (x > right) right = x
        if (y < top) top = y
        if (y > bottom) bottom = y
      }
    }
    return if (right < 0) null else InkBounds(left, top, right, bottom)
  }

  /** Returns the [r, g, b, a] channels of the pixel at ([x], [y]). */
  private fun pixel(canvas: HTMLCanvasElement, x: Int, y: Int): IntArray {
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    val data = ctx.getImageData(x.toDouble(), y.toDouble(), 1.0, 1.0).data
    return IntArray(4) { data[it].toInt() and 0xFF }
  }

  @Test
  fun `filled rectangle paints its interior only`() {
    val canvas = newCanvas()
    val model = model("""
      [{"type": "rectangle", "x": 10, "y": 20, "width": 100, "height": 40,
        "style": {"fillColor": "#336699", "strokeColor": "none", "opacity": 100},
        "attributes": {"taskId": "42"}}]
    """)

    drawChart(model, canvas)

    assertEquals(listOf(0x33, 0x66, 0x99, 255), pixel(canvas, 60, 40).toList(), "rectangle interior")
    assertEquals(0, pixel(canvas, 5, 5)[3], "outside the rectangle stays transparent")
  }

  @Test
  fun `rectangle stroke paints the border only`() {
    val canvas = newCanvas()
    val model = model("""
      [{"type": "rectangle", "x": 10, "y": 20, "width": 100, "height": 40,
        "style": {"fillColor": "none", "strokeColor": "#000000", "strokeWidth": 4},
        "attributes": {}}]
    """)

    drawChart(model, canvas)

    val border = pixel(canvas, 60, 20)
    assertTrue(border[3] > 200 && border[0] < 50, "border is painted black: ${border.toList()}")
    assertEquals(0, pixel(canvas, 60, 40)[3], "interior is not filled")
  }

  @Test
  fun `shape opacity maps to the pixel alpha`() {
    val canvas = newCanvas()
    val model = model("""
      [{"type": "rectangle", "x": 10, "y": 20, "width": 100, "height": 40,
        "style": {"fillColor": "#ff0000", "strokeColor": "none", "opacity": 40},
        "attributes": {}}]
    """)

    drawChart(model, canvas)

    val pixel = pixel(canvas, 60, 40)
    assertTrue(pixel[0] > 250, "red channel is preserved: ${pixel.toList()}")
    assertTrue(pixel[3] in 95..110, "alpha is ~40%: ${pixel.toList()}")
  }

  @Test
  fun `solid line with classic arrow paints the shaft and the arrow head`() {
    val canvas = newCanvas()
    val model = model("""
      [{"type": "line", "startX": 10, "startY": 50, "finishX": 90, "finishY": 50,
        "style": {"endArrow": "classic", "strokeColor": "#ff0000", "opacity": 100, "dashed": 0},
        "attributes": {}}]
    """)

    drawChart(model, canvas)

    val shaft = pixel(canvas, 50, 50)
    assertTrue(shaft[0] > 200 && shaft[3] > 80, "the shaft is red: ${shaft.toList()}")
    val arrow = pixel(canvas, 85, 50)
    assertTrue(arrow[0] > 200 && arrow[3] > 200, "the arrow head is filled: ${arrow.toList()}")
    assertEquals(0, pixel(canvas, 95, 50)[3], "nothing is painted beyond the arrow tip")
  }

  @Test
  fun `dashed line leaves gaps`() {
    val canvas = newCanvas()
    // The dash pattern is [3, 3] starting at x=10: segments [10,13), [16,19), ... are painted.
    val model = model("""
      [{"type": "line", "startX": 10, "startY": 70, "finishX": 90, "finishY": 70,
        "style": {"endArrow": "none", "strokeColor": "#00ff00", "dashed": 1},
        "attributes": {}}]
    """)

    drawChart(model, canvas)

    assertTrue(pixel(canvas, 11, 70)[3] > 0, "dash segment is painted")
    assertEquals(0, pixel(canvas, 14, 70)[3], "gap between dashes is transparent")
    assertTrue(pixel(canvas, 17, 70)[3] > 0, "next dash segment is painted")
  }

  @Test
  fun `line without stroke color falls back to black`() {
    val canvas = newCanvas()
    val model = model("""
      [{"type": "line", "startX": 10, "startY": 60, "finishX": 90, "finishY": 60,
        "style": {"endArrow": "none"}, "attributes": {}}]
    """)

    drawChart(model, canvas)

    val pixel = pixel(canvas, 50, 60)
    assertTrue(pixel[3] > 80 && pixel[0] < 50 && pixel[1] < 50 && pixel[2] < 50,
        "the line is black: ${pixel.toList()}")
  }

  @Test
  fun `rhombus paints the diamond inside its bounding box`() {
    val canvas = newCanvas()
    val model = model("""
      [{"type": "rhombus", "x": 30, "y": 80, "width": 20, "height": 20,
        "style": {"fillColor": "#123456", "strokeColor": "none"},
        "attributes": {}}]
    """)

    drawChart(model, canvas)

    assertEquals(listOf(0x12, 0x34, 0x56, 255), pixel(canvas, 40, 90).toList(), "rhombus center")
    assertEquals(0, pixel(canvas, 32, 82)[3], "bounding box corner is outside the diamond")
  }

  @Test
  fun `primitives are painted in the model order`() {
    val canvas = newCanvas()
    // A line drawn before a rectangle which covers it, and a rectangle drawn before a line
    // which crosses it: whatever comes last in the list wins.
    val model = model("""
      [{"type": "line", "startX": 10, "startY": 40, "finishX": 110, "finishY": 40,
        "style": {"endArrow": "none", "strokeColor": "#ff0000"}, "attributes": {}},
       {"type": "rectangle", "x": 10, "y": 20, "width": 100, "height": 40,
        "style": {"fillColor": "#0000ff", "strokeColor": "none"}, "attributes": {}},
       {"type": "line", "startX": 10, "startY": 50, "finishX": 110, "finishY": 50,
        "style": {"endArrow": "none", "strokeColor": "#00ff00", "strokeWidth": 4}, "attributes": {}}]
    """)

    drawChart(model, canvas)

    assertEquals(listOf(0x00, 0x00, 0xff, 255), pixel(canvas, 60, 40).toList(),
        "the rectangle covers the line painted before it")
    val overRect = pixel(canvas, 60, 50)
    assertTrue(overRect[1] > 200 && overRect[2] < 50,
        "the line painted after the rectangle is on top of it: ${overRect.toList()}")
  }

  @Test
  fun `primitives of unknown kinds are skipped`() {
    val canvas = newCanvas()
    val model = model("""
      [{"type": "textGroup", "x": 10, "y": 40, "style": {}, "attributes": {}},
       {"type": "rectangle", "x": 10, "y": 20, "width": 100, "height": 40,
        "style": {"fillColor": "#336699", "strokeColor": "none"}, "attributes": {}}]
    """)

    drawChart(model, canvas)

    assertEquals(listOf(0x33, 0x66, 0x99, 255), pixel(canvas, 60, 40).toList(),
        "the supported primitives are still painted")
  }

  @Test
  fun `text is painted in its font color above the anchor point`() {
    val canvas = newCanvas()
    val model = model("""
      [{"type": "text", "x": 20, "y": 100, "text": "Hello",
        "style": {"align": "left", "verticalAlign": "bottom", "fontColor": "#ff0000",
                  "fontSize": 20, "fontFamily": "sans-serif", "textOpacity": 100},
        "attributes": {"text": "Hello"}}]
    """)

    drawChart(model, canvas)

    val ink = inkBounds(canvas)!!
    assertTrue(ink.left >= 20, "the label starts at the anchor x: $ink")
    assertTrue(ink.bottom <= 100, "the label sits above the anchor y: $ink")
    assertTrue(ink.top > 100 - 30, "the label height is about the font size: $ink")
    val glyph = pixel(canvas, ink.left, (ink.top + ink.bottom) / 2)
    assertTrue(glyph[0] > 100 && glyph[1] < 100, "the glyphs are red: ${glyph.toList()}")
  }

  @Test
  fun `text alignment places the label relative to the anchor point`() {
    val textAt = { align: String, vAlign: String ->
      val canvas = newCanvas()
      drawChart(model("""
        [{"type": "text", "x": 100, "y": 100, "text": "Hello",
          "style": {"align": "$align", "verticalAlign": "$vAlign", "fontColor": "#000000",
                    "fontSize": 20},
          "attributes": {}}]
      """), canvas)
      inkBounds(canvas)!!
    }

    val left = textAt("left", "bottom")
    val center = textAt("center", "bottom")
    val right = textAt("right", "bottom")
    assertTrue(left.left >= 100, "left-aligned label starts at the anchor: $left")
    assertTrue(right.right <= 101, "right-aligned label ends at the anchor: $right")
    assertTrue(center.left < 100 && center.right > 100, "centered label spans the anchor: $center")

    val top = textAt("left", "top")
    val middle = textAt("left", "middle")
    assertTrue(top.top >= 100, "top-aligned label is below the anchor: $top")
    assertTrue(middle.top < 100 && middle.bottom > 100, "middle-aligned label spans the anchor: $middle")
  }

  @Test
  fun `text opacity maps to the pixel alpha`() {
    val canvas = newCanvas()
    drawChart(model("""
      [{"type": "text", "x": 20, "y": 100, "text": "Hello",
        "style": {"align": "left", "verticalAlign": "bottom", "fontColor": "#000000",
                  "fontSize": 20, "textOpacity": 30},
        "attributes": {}}]
    """), canvas)

    val ink = inkBounds(canvas)!!
    val maxAlpha = (ink.left..ink.right).maxOf { x ->
      (ink.top..ink.bottom).maxOf { y -> pixel(canvas, x, y)[3] }
    }
    assertTrue(maxAlpha in 60..90, "the most opaque pixel of the label is at ~30%: $maxAlpha")
  }

  @Test
  fun `text primitive without a label paints nothing`() {
    val canvas = newCanvas()
    drawChart(model("""
      [{"type": "text", "x": 20, "y": 100,
        "style": {"align": "left", "verticalAlign": "bottom", "fontSize": 20},
        "attributes": {}}]
    """), canvas)

    assertEquals(null, inkBounds(canvas), "nothing is painted")
  }

  @Test
  fun `drawChart clears the canvas before painting`() {
    val canvas = newCanvas()
    drawChart(model("""
      [{"type": "rectangle", "x": 0, "y": 0, "width": 200, "height": 200,
        "style": {"fillColor": "#000000", "strokeColor": "none"}, "attributes": {}}]
    """), canvas)
    assertTrue(pixel(canvas, 60, 40)[3] > 0)

    drawChart(model(), canvas)
    assertEquals(0, pixel(canvas, 60, 40)[3], "the previous painting is cleared")
  }
}
