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
package biz.ganttproject.core.chart.scene

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.TextMetrics

private const val OFFSET = 5

class ResourcesColumnSceneBuilder(
    private val resources: List<Resource>,
    private val input: InputApi,
    val canvas: Canvas = Canvas()
) {
  val width = resources.map { input.textMetrics.getTextLength(it.name) }.maxOrNull() ?: 0 + OFFSET * 2

  fun build() {
    canvas.clear()
    var y = input.yCanvasOffset
    var isOddRow = false
    resources.forEach {
      val rectangle = canvas.createRectangle(0, y, width, input.rowHeight)
      rectangle.attributes["email"] = it.email
      if (isOddRow) {
        rectangle.style = "odd-row"
      }
      val text = canvas.createText(OFFSET, rectangle.middleY, it.name)
      text.setAlignment(Canvas.HAlignment.LEFT, Canvas.VAlignment.CENTER)
      text.attributes["email"] = it.email
      y += input.rowHeight
      isOddRow = !isOddRow
    }
  }

  data class Resource(val name: String, val email: String)

  interface InputApi {
    val textMetrics: TextMetrics
    val yCanvasOffset: Int
    val rowHeight: Int
  }
}
