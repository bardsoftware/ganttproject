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

import biz.ganttproject.core.chart.scene.gantt.TableSceneBuilder.*
import biz.ganttproject.core.chart.scene.gantt.TableSceneBuilder.Table.*
import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.TextMetrics

class TaskTableSceneBuilder(
  private val input: InputApi
) {
  private val tableSceneConfig = Config(input.rowHeight, input.horizontalOffset, input.textMetrics)

  fun build(columns: List<Column>, tasks: List<Task>, canvas: Canvas = Canvas()): Canvas {
    val rows = toRow(tasks)
    val table = Table(columns, rows)
    val tableSceneBuilder = TableSceneBuilder(tableSceneConfig, table, canvas)
    return tableSceneBuilder.build()
  }

  private fun toRow(tasks: List<Task>, indent: Int = 0): List<Row> {
    return tasks.flatMap {
      listOf(Row(it.values, indent)) + toRow(it.subtasks, indent + input.depthIndent)
    }
  }

  class Task(
    val values: Map<Column, String>,
    val subtasks: List<Task> = emptyList()
  ) {
    companion object {
      val EMPTY = Task(emptyMap())
    }
  }

  interface InputApi {
    val textMetrics: TextMetrics
    val rowHeight: Int
    val depthIndent: Int
    val horizontalOffset: Int
  }
}
