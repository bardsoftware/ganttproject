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
package biz.ganttproject.mxgraph

import biz.ganttproject.core.chart.canvas.Canvas
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mxgraph.util.mxConstants
import net.sourceforge.ganttproject.chart.ChartUIConfiguration
import net.sourceforge.ganttproject.gui.UIConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.awt.Color

/**
 * Verifies that [MxGraphPainter] backed by [JsonPainterImpl] produces the expected JSON model.
 * The style objects carry mxGraph style keys, since [MxGraphPainter] resolves the styles the
 * same way regardless of the backend.
 */
class JsonPainterTest {
  private val mapper = ObjectMapper()

  private fun uiConfig() = ChartUIConfiguration(UIConfiguration(Color.BLUE, false))

  private fun render(build: (Canvas) -> Unit): JsonNode {
    val canvas = Canvas()
    build(canvas)
    val impl = JsonPainterImpl()
    val painter = MxGraphPainter(uiConfig(), impl)
    painter.paint { canvas.paint(painter) }
    return mapper.readTree(impl.toJson())
  }

  @Test
  fun `rectangle is serialized with coordinates, colors and attributes`() {
    val model = render { canvas ->
      val rectangle = canvas.createRectangle(10, 20, 100, 40)
      rectangle.style = "task"
      rectangle.setBackgroundColor(Color(0x33, 0x66, 0x99))
      rectangle.setForegroundColor(Color(0x00, 0x00, 0x00))
      rectangle.setOpacity(0.5f)
      rectangle.attributes["taskId"] = "42"
    }

    val rectangles = model["rectangles"].toList()
    assertEquals(1, rectangles.size)
    assertTrue(model["lines"].isEmpty)
    assertTrue(model["rhombuses"].isEmpty)
    val rect = rectangles.single()
    assertEquals(10, rect["x"].asInt())
    assertEquals(20, rect["y"].asInt())
    assertEquals(100, rect["width"].asInt())
    assertEquals(40, rect["height"].asInt())
    assertEquals("#336699", rect["style"][mxConstants.STYLE_FILLCOLOR].asText())
    assertEquals("#000000", rect["style"][mxConstants.STYLE_STROKECOLOR].asText())
    // Opacity is expressed as a percentage in the mxGraph style model.
    assertEquals(50.0, rect["style"][mxConstants.STYLE_OPACITY].asDouble(), 1e-6)
    assertEquals("42", rect["attributes"]["taskId"].asText())
  }

  @Test
  fun `line is serialized with endpoints and arrow flag`() {
    val model = render { canvas ->
      val withArrow = canvas.createLine(0, 0, 50, 60)
      withArrow.style = "dependency"
      withArrow.setForegroundColor(Color.RED)
      withArrow.setArrow(Canvas.Arrow.FINISH)

      canvas.createLine(1, 2, 3, 4).style = "dependency"
    }

    val lines = model["lines"].toList()
    assertEquals(2, lines.size)

    val arrowLine = lines[0]
    assertEquals(0, arrowLine["startX"].asInt())
    assertEquals(0, arrowLine["startY"].asInt())
    assertEquals(50, arrowLine["finishX"].asInt())
    assertEquals(60, arrowLine["finishY"].asInt())
    assertEquals(mxConstants.ARROW_CLASSIC, arrowLine["style"][mxConstants.STYLE_ENDARROW].asText())
    assertEquals("#ff0000", arrowLine["style"][mxConstants.STYLE_STROKECOLOR].asText())
    assertEquals(0, arrowLine["style"][mxConstants.STYLE_DASHED].asInt())

    val plainLine = lines[1]
    assertEquals(mxConstants.NONE, plainLine["style"][mxConstants.STYLE_ENDARROW].asText())
    // No color set on the shape: falls back to black.
    assertEquals("#000000", plainLine["style"][mxConstants.STYLE_STROKECOLOR].asText())
  }

  @Test
  fun `rhombus is serialized with coordinates and colors`() {
    val model = render { canvas ->
      val rhombus = canvas.createRhombus(30, 40, 20, 20)
      rhombus.style = "milestone"
      rhombus.setBackgroundColor(Color(0x12, 0x34, 0x56))
      rhombus.setForegroundColor(Color(0x65, 0x43, 0x21))
    }

    val rhombus = model["rhombuses"].single()
    assertEquals("#123456", rhombus["style"][mxConstants.STYLE_FILLCOLOR].asText())
    // Rhombus has size derived from its diagonals.
    assertTrue(rhombus["width"].asInt() > 0)
    assertTrue(rhombus["height"].asInt() > 0)
  }

  @Test
  fun `text painting is not implemented`() {
    val canvas = Canvas()
    canvas.createText(0, 0, "hello")
    val impl = JsonPainterImpl()
    val painter = MxGraphPainter(uiConfig(), impl)
    assertThrows(NotImplementedError::class.java) {
      painter.paint { canvas.paint(painter) }
    }
  }
}
