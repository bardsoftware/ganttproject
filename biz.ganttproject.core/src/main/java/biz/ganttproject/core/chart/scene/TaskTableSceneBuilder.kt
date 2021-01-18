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
package biz.ganttproject.core.chart.scene

import biz.ganttproject.core.chart.scene.TableSceneBuilder.*
import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.TextMetrics

class TaskTableSceneBuilder(
  private val input: InputApi
) {
  private val tableSceneBuilder = TableSceneBuilder(object : TableSceneBuilder.InputApi {
    override val rowHeight = input.rowHeight
    override val horizontalOffset = input.horizontalOffset
  })

  fun build(tasks: List<Task>): Canvas {
    return tableSceneBuilder.build(toRows(tasks))
  }

  private fun toRows(tasks: List<Task>, indent: Int = 0): List<Row> {
    return tasks.flatMap {
      listOf(Row(
        input.textMetrics.getTextLength(it.name),
        it.name,
        indent
      )) + toRows(it.subtasks, indent + input.depthIndent)
    }
  }

  class Task(val name: String, val subtasks: List<Task> = emptyList()) {
    companion object {
      val EMPTY = Task("")
    }
  }

  interface InputApi {
    val textMetrics: TextMetrics
    val rowHeight: Int
    val depthIndent: Int
    val horizontalOffset: Int
  }
}
