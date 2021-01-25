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
import java.lang.Integer.max

class TableSceneBuilder(
  private val config: Config,
  private val table: Table,
  private val canvas: Canvas = Canvas()
) {
  private val colsWidth = calculateColsWidth()
  private val dimensions = calculateDimensions()

  fun build(): Canvas {
    val state = PaintState(config.rowHeight)

    paintHeader(state)
    table.rows.forEach {
      val rectangle = canvas.createRectangle(
        0, state.y, dimensions.width, config.rowHeight
      )
      if (state.rowNumber % 2 == 1) {
        rectangle.style = "odd-row"
      }
      paintRow(it, rectangle.middleY)
      state.toNextRow()
    }

    return canvas
  }

  private fun calculateColsWidth(): Map<Table.Column, Int> {
    val widths = mutableMapOf<Table.Column, Int>()
    table.columns.forEach { col ->
      val isFirst = col == table.columns.first()
      widths[col] = if (col.width != null) {
        col.width
      } else {
        val colNameWidth = config.textMetrics.getTextLength(col.name)
        val colContentWidth = table.rows.map {
          val indent = if (isFirst) it.indent else 0
          indent + (it.values[col]?.let(config.textMetrics::getTextLength) ?: 0)
        }.maxOrNull() ?: 0
        max(colNameWidth, colContentWidth)
      }
    }
    return widths
  }

  private fun calculateDimensions(): Dimension {
    val height = config.rowHeight * table.rows.size
    val width = table.columns.sumBy { colsWidth[it]!! } + 2 * config.horizontalOffset
    return Dimension(height, width)
  }

  private fun paintHeader(state: PaintState) {
    var x = config.horizontalOffset
    table.columns.forEach {
      val rectangle = canvas.createRectangle(
        x, state.y, colsWidth[it]!!, config.rowHeight
      )
      // TODO: add rectangle borders and color?
      paintString(it.name, x, rectangle.middleY, colsWidth[it]!!)
      x += colsWidth[it]!!
    }
    state.toNextRow()
  }

  private fun paintRow(row: Table.Row, y: Int) {
    var x = config.horizontalOffset + row.indent
    table.columns.forEach { col ->
      row.values[col]?.also {
        paintString(it, x, y, colsWidth[col]!!)
      }
      x += colsWidth[col]!!
    }
  }

  private fun paintString(string: String, x: Int, y: Int, widthLimit: Int) {
    var fitString = string
    if (config.textMetrics.getTextLength(fitString) > widthLimit) {
      val letterWidth = config.textMetrics.getTextLength("m")
      val dots = "... "
      val lettersNumber = max(0, (widthLimit - config.textMetrics.getTextLength(dots)) / letterWidth)
      fitString = fitString.substring(0, lettersNumber)
      fitString += dots
    }
    val text = canvas.createText(x, y, fitString)
    text.setAlignment(Canvas.HAlignment.LEFT, Canvas.VAlignment.CENTER)
  }

  data class Config(val rowHeight: Int, val horizontalOffset: Int, val textMetrics: TextMetrics)

  class Table(val columns: List<Column>, val rows: List<Row>) {
    class Column(val name: String, val width: Int? = null)

    class Row(val values: Map<Column, String>, val indent: Int)
  }

  private data class Dimension(val height: Int, val width: Int)

  private class PaintState(private val rowHeight: Int) {
    var y = 0
      private set
    var rowNumber = 0
      private set

    fun toNextRow() {
      y += rowHeight
      rowNumber++
    }
  }
}
