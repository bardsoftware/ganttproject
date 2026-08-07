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

  private fun model(
    rectangles: String = "[]",
    lines: String = "[]",
    rhombuses: String = "[]"
  ): ChartModel = JSON.parse(
    """{"rectangles": $rectangles, "lines": $lines, "rhombuses": $rhombuses}"""
  )

  /** Returns the [r, g, b, a] channels of the pixel at ([x], [y]). */
  private fun pixel(canvas: HTMLCanvasElement, x: Int, y: Int): IntArray {
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    val data = ctx.getImageData(x.toDouble(), y.toDouble(), 1.0, 1.0).data
    return IntArray(4) { data[it].toInt() and 0xFF }
  }

  @Test
  fun `filled rectangle paints its interior only`() {
    val canvas = newCanvas()
    val model = model(rectangles = """
      [{"x": 10, "y": 20, "width": 100, "height": 40,
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
    val model = model(rectangles = """
      [{"x": 10, "y": 20, "width": 100, "height": 40,
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
    val model = model(rectangles = """
      [{"x": 10, "y": 20, "width": 100, "height": 40,
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
    val model = model(lines = """
      [{"startX": 10, "startY": 50, "finishX": 90, "finishY": 50,
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
    val model = model(lines = """
      [{"startX": 10, "startY": 70, "finishX": 90, "finishY": 70,
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
    val model = model(lines = """
      [{"startX": 10, "startY": 60, "finishX": 90, "finishY": 60,
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
    val model = model(rhombuses = """
      [{"x": 30, "y": 80, "width": 20, "height": 20,
        "style": {"fillColor": "#123456", "strokeColor": "none"},
        "attributes": {}}]
    """)

    drawChart(model, canvas)

    assertEquals(listOf(0x12, 0x34, 0x56, 255), pixel(canvas, 40, 90).toList(), "rhombus center")
    assertEquals(0, pixel(canvas, 32, 82)[3], "bounding box corner is outside the diamond")
  }

  @Test
  fun `drawChart clears the canvas before painting`() {
    val canvas = newCanvas()
    drawChart(model(rectangles = """
      [{"x": 0, "y": 0, "width": 200, "height": 200,
        "style": {"fillColor": "#000000", "strokeColor": "none"}, "attributes": {}}]
    """), canvas)
    assertTrue(pixel(canvas, 60, 40)[3] > 0)

    drawChart(model(), canvas)
    assertEquals(0, pixel(canvas, 60, 40)[3], "the previous painting is cleared")
  }
}
