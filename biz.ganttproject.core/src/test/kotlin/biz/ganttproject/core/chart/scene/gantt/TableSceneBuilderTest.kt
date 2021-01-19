/*
Copyright 2021 BarD Software s.r.o, Dmitry Kazakov

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
package biz.ganttproject.core.chart.scene.gantt

import org.junit.Test
import org.mockito.Mockito.*

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.scene.gantt.TableSceneBuilder.*

class TableSceneBuilderTest {
  @Test
  fun `test width when max width comes from large indent row`() {
    val rowHeight = 10
    val horizontalOffset = 5
    val rows = listOf(
      Row(10, "", 0), Row(1, "", 10), Row(4, "", 5),
    )
    test(rowHeight, horizontalOffset, rows)
  }

  @Test
  fun `test width when max width comes from large text width row`() {
    val rowHeight = 10
    val horizontalOffset = 5
    val rows = listOf(
      Row(12, "", 0), Row(1, "", 10), Row(4, "", 5),
    )
    test(rowHeight, horizontalOffset, rows)
  }

  private fun test(rowHeight: Int, horizontalOffset: Int, rows: List<Row>) {
    val canvas = spy(Canvas())
    val sceneBuilder = TableSceneBuilder(Config(rowHeight, horizontalOffset))
    sceneBuilder.build(rows, canvas)
    val expectedWidth = rows.map { it.indent + it.width + 2 * horizontalOffset }.maxOrNull() ?: 0

    rows.forEachIndexed { i, row ->
      verify(canvas).createRectangle(0, i * rowHeight, expectedWidth, rowHeight)
    }
  }
}
