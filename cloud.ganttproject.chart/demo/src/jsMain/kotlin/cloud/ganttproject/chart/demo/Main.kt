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
package cloud.ganttproject.chart.demo

import cloud.ganttproject.chart.ChartModel
import cloud.ganttproject.chart.drawChart
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import kotlin.js.JSON

/**
 * A minimal demo of the cloud.ganttproject.chart library.
 *
 * It builds a small chart model with two task rectangles connected by a finish-start
 * dependency (an orthogonal arrowed connector going out of the first task's finish edge
 * and into the second task's start edge) and renders it onto a canvas via [drawChart].
 *
 * The model is expressed as JSON, exactly like the model produced by the server-side
 * JsonPainterImpl (module biz.ganttproject.mxgraph), and parsed into a [ChartModel]:
 * a single list of primitives which are painted in the listed order.
 */
private val DEMO_MODEL_JSON = """
{
  "primitives": [
    {
      "type": "rectangle",
      "x": 40, "y": 40, "width": 120, "height": 32,
      "style": {"fillColor": "#4a89dc", "strokeColor": "#2f5c96", "opacity": 100, "strokeWidth": 1},
      "attributes": {"name": "Task A"}
    },
    {
      "type": "rectangle",
      "x": 240, "y": 110, "width": 150, "height": 32,
      "style": {"fillColor": "#5cb85c", "strokeColor": "#3d8b3d", "opacity": 100, "strokeWidth": 1},
      "attributes": {"name": "Task B"}
    },
    {
      "type": "line",
      "startX": 160, "startY": 56, "finishX": 200, "finishY": 56,
      "style": {"endArrow": "none", "strokeColor": "#555555", "opacity": 100, "dashed": 0, "strokeWidth": 2},
      "attributes": {"dependencyType": "finish-start"}
    },
    {
      "type": "line",
      "startX": 200, "startY": 56, "finishX": 200, "finishY": 126,
      "style": {"endArrow": "none", "strokeColor": "#555555", "opacity": 100, "dashed": 0, "strokeWidth": 2},
      "attributes": {"dependencyType": "finish-start"}
    },
    {
      "type": "line",
      "startX": 200, "startY": 126, "finishX": 238, "finishY": 126,
      "style": {"endArrow": "classic", "strokeColor": "#555555", "opacity": 100, "dashed": 0, "strokeWidth": 2},
      "attributes": {"dependencyType": "finish-start"}
    }
  ]
}
""".trimIndent()

fun main() {
  val canvas = document.getElementById("chart") as HTMLCanvasElement
  val model: ChartModel = JSON.parse(DEMO_MODEL_JSON)
  drawChart(model, canvas)
}
