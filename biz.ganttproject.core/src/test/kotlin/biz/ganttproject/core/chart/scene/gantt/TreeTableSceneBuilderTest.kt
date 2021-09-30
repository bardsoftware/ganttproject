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

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.TextMetrics
import biz.ganttproject.core.table.HEADER_HEIGHT_DECREMENT
import biz.ganttproject.core.table.TEXT_PADDING
import biz.ganttproject.core.table.TableSceneBuilder
import biz.ganttproject.core.table.TreeTableSceneBuilder.*
import biz.ganttproject.core.table.TableSceneBuilder.Table.*
import biz.ganttproject.core.table.TreeTableSceneBuilder

import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.awt.Font

class TreeTableSceneBuilderTest {
  @Test
  fun `test indents`() {
    val input = InputApi(
      textMetrics = TextMetricsStub,
      rowHeight = 10,
      depthIndent = 15,
      horizontalOffset = 2,
      headerHeight = 10
    )
    val sceneBuilder = TreeTableSceneBuilder(input)
    val column = Column("name", width = 50, isTreeColumn = true)
    val tasks = listOf(
      Item(mapOf(column to "1")),
      Item(mapOf(column to "2"), mutableListOf(
        Item(mapOf(column to "3")),
        Item(mapOf(column to "4"), mutableListOf(Item(mapOf(column to "5"))))
      ))
    )
    val canvas = spy(Canvas())
    sceneBuilder.build(listOf(column), tasks, canvas)

    val halfRowHeight = input.rowHeight / 2 - HEADER_HEIGHT_DECREMENT
    val expectedTextLeft = input.horizontalOffset + TEXT_PADDING
    verify(canvas).createText(expectedTextLeft, input.headerHeight - input.headerHeight/2 - HEADER_HEIGHT_DECREMENT, "name")
    verify(canvas).createText(expectedTextLeft, input.rowHeight + halfRowHeight, "1")
    verify(canvas).createText(expectedTextLeft, 2 * input.rowHeight + halfRowHeight, "2")
    verify(canvas).createText(expectedTextLeft + input.depthIndent, 3 * input.rowHeight + halfRowHeight, "3")
    verify(canvas).createText(expectedTextLeft + input.depthIndent, 4 * input.rowHeight + halfRowHeight, "4")
    verify(canvas).createText(expectedTextLeft + input.depthIndent * 2, 5 * input.rowHeight + halfRowHeight, "5")
  }
}

object TextMetricsStub : TextMetrics {
  override fun getTextLength(text: String) = text.length * 7

  override fun getTextHeight(text: String) = 10

  override fun getTextHeight(font: Font, text: String) = font.size

  override fun getState() = Object()
}
