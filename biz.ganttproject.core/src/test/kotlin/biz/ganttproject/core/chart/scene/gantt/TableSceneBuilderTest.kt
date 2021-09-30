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

import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.table.HEADER_HEIGHT_DECREMENT
import biz.ganttproject.core.table.TEXT_PADDING
import biz.ganttproject.core.table.TableSceneBuilder
import biz.ganttproject.core.table.TableSceneBuilder.*
import biz.ganttproject.core.table.TableSceneBuilder.Table.*

class TableSceneBuilderTest {
//  @Test
//  fun `test width when max width comes from large indent row`() {
//    val rowHeight = 10
//    val horizontalOffset = 5
//    val cols = listOf(Column("1", width = 100, isTreeColumn = true))
//    val values = mapOf(cols[0] to "1")
//    val rows = listOf(
//      Row(values, 10), Row(values, 1), Row(values, 4)
//    )
//    val table = Table(cols, rows)
//
//    test(rowHeight, horizontalOffset, table, 100)
//  }
//
//  private fun test(rowHeight: Int, horizontalOffset: Int, table: Table, expectedWidth: Int) {
//    val canvas = spy(Canvas())
//    val sceneBuilder = TableSceneBuilder(Config(rowHeight, rowHeight, horizontalOffset, TextMetricsStub), table, canvas)
//    sceneBuilder.build()
//    for (i in 1..table.rows.size) {
//      verify(canvas).createRectangle(TEXT_PADDING, (i-1) * rowHeight, expectedWidth, rowHeight - HEADER_HEIGHT_DECREMENT)
//    }
//  }
}
