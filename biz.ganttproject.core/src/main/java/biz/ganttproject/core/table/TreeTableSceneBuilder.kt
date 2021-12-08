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
package biz.ganttproject.core.table

import biz.ganttproject.core.table.TableSceneBuilder.*
import biz.ganttproject.core.table.TableSceneBuilder.Table.*
import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.TextMetrics

class TreeTableSceneBuilder(private val input: InputApi) {
  private val tableSceneConfig = Config(input.headerHeight, input.rowHeight, input.horizontalOffset, input.textMetrics)

  fun build(columns: List<Column>, items: List<Item>, canvas: Canvas = Canvas()): Canvas {
    val rows = toRow(items)
    val table = Table(columns, rows)
    val tableSceneBuilder = TableSceneBuilder(tableSceneConfig, table, canvas)
    return tableSceneBuilder.build()
  }

  private fun toRow(items: List<Item>, indent: Int = 0): List<Row> {
    return items.flatMap {
      listOf(Row(it.values, indent)) + toRow(it.subitems, indent + input.depthIndent)
    }
  }

  data class Item(
    val values: Map<Column, String>,
    val subitems: MutableList<Item> = mutableListOf()
  ) {
    companion object {
      val EMPTY = Item(emptyMap())
    }
  }

  data class InputApi(
    val textMetrics: TextMetrics,
    val headerHeight: Int,
    val rowHeight: Int,
    val depthIndent: Int,
    val horizontalOffset: Int,
  ) {
  }
}
