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
  private val tableSceneBuilder = TableSceneBuilder(Config(input.rowHeight, input.horizontalOffset, input.textMetrics))
  private val cols = listOf(Column("Name"), Column("Begin date"), Column("End date"), Column("Cost"))

  fun build(tasks: List<Task>, canvas: Canvas = Canvas()): Canvas {
    return tableSceneBuilder.build(toTable(tasks), canvas)
  }

  private fun toTable(tasks: List<Task>): Table {
    val rows = toRow(tasks)
    return Table(cols, rows)
  }

  private fun toRow(tasks: List<Task>, indent: Int = 0): List<Row> {
    return tasks.flatMap {
      listOf(Row(
        mapOf(
          cols[0] to it.name, cols[1] to it.beginDate, cols[2] to it.endDate, cols[3] to it.cost
        ),
        indent
      )) + toRow(it.subtasks, indent + input.depthIndent)
    }
  }

  class Task(
    val name: String,
    val beginDate: String,
    val endDate: String,
    val cost: String,
    val subtasks: List<Task> = emptyList()
  ) {
    companion object {
      val EMPTY = Task("", "", "", "")
    }
  }

  interface InputApi {
    val textMetrics: TextMetrics
    val rowHeight: Int
    val depthIndent: Int
    val horizontalOffset: Int
  }
}
