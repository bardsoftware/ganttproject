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

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.TextMetrics
import java.awt.Color
import java.lang.Integer.max

class TableSceneBuilder(
  private val config: Config,
  private val table: Table,
  private val canvas: Canvas = Canvas()
) {
  private val colsWidth = calculateColsWidth()
  private val dimensions = calculateDimensions()

  fun build(): Canvas {
    var state = PaintState(config.headerHeight - HEADER_HEIGHT_DECREMENT)

    paintHeader(state)
    state = state.withRowHeight(config.rowHeight)
    table.rows.forEach {
      canvas.createLine(0, state.y + config.rowHeight, dimensions.width, state.y + config.rowHeight).apply {
        foregroundColor = Color.GRAY
      }
      paintRow(it, state.y + config.rowHeight/2)
      state.toNextRow()
    }

    return canvas
  }

  data class ColumnWidth(val actual: Int, val maxContent: Int) {
  }

  private fun calculateColsWidth(): Map<Table.Column, ColumnWidth> {
    val widths = mutableMapOf<Table.Column, ColumnWidth>()
    table.columns.forEach { col ->
      widths[col] = ColumnWidth(col.width ?: 0, run {
        val colNameWidth = config.textMetrics.getTextLength(col.name)
        val colContentWidth = table.rows.map {
          val indent = if (col.isTreeColumn) it.indent else 0
          indent + (it.values[col]?.let(config.textMetrics::getTextLength) ?: 0)
        }.maxOrNull() ?: 0
        max(colNameWidth, colContentWidth)
      })
    }
    return widths
  }

  private fun calculateDimensions(): Dimension {
    val height = config.rowHeight * table.rows.size
    val width = table.columns.sumOf { colsWidth[it]!!.actual } + 2 * config.horizontalOffset
    return Dimension(height, width)
  }

  private fun paintHeader(state: PaintState) {
    var x = config.horizontalOffset
    table.columns.forEach {
      val height = config.headerHeight - HEADER_HEIGHT_DECREMENT
      val width = colsWidth[it]!!

      val rectangle = canvas.createRectangle(
        x, state.y, width.actual, height
      ).apply {
        style = "timeline.area"
      }
      //timeunitHeaderBorder.setForegroundColor(myInputApi.getTimelineBorderColor());
      canvas.createLine(
        x, state.y + height, x + width.actual, state.y + height
      ).apply {
        style = "timeline.borderBottom"
      }
//      canvas.createLine(
//        x + width, state.y, x + width, state.y + height
//      ).apply {
//        style = "timeline.borderBottom"
//      }

      // TODO: add rectangle borders and color?
      paintString(it.name, x + TEXT_PADDING, rectangle.middleY, width.actual).also {
        it.setAlignment(Canvas.HAlignment.LEFT, Canvas.VAlignment.CENTER)
      }
      x += width.actual
    }
    state.toNextRow()
  }

  private fun paintRow(row: Table.Row, y: Int) {
    var x = config.horizontalOffset
    table.columns.forEach { col ->
      val width = colsWidth[col]!!
      row.values[col]?.also {
        val indent = TEXT_PADDING + if (col.isTreeColumn) row.indent else 0
        when (col.alignment) {
          Canvas.HAlignment.RIGHT -> {
            paintString(it, x + width.actual - TEXT_PADDING, y, width.actual).also {
              it.setAlignment(col.alignment, Canvas.VAlignment.CENTER)
            }
          }
          Canvas.HAlignment.LEFT -> {
            paintString(it, x + indent, y, width.actual).also {
              it.setAlignment(col.alignment, Canvas.VAlignment.CENTER)
            }
          }
          else -> {}
        }
        //canvas.createLine(x + width, y + config.rowHeight/4, x+width, y + config.rowHeight/2)
      }
      x += width.actual
    }
  }

  private fun paintString(string: String, x: Int, y: Int, widthLimit: Int): Canvas.Text {
    var fitString = string
    if (config.textMetrics.getTextLength(fitString) > widthLimit) {
      val letterWidth = config.textMetrics.getTextLength("m")
      val dots = "... "
      val lettersNumber = max(0, (widthLimit - config.textMetrics.getTextLength(dots)) / letterWidth)
      if (lettersNumber < fitString.length) {
        fitString = fitString.substring(0, lettersNumber)
        fitString += dots
      }
    }
    return canvas.createText(x, y, fitString)
  }

  data class Config(
    val headerHeight: Int,
    val rowHeight: Int,
    val horizontalOffset: Int,
    val textMetrics: TextMetrics
  )

  class Table(val columns: List<Column>, val rows: List<Row>) {
    class Column(val name: String,
                 val width: Int? = null,
                 val isTreeColumn: Boolean = false,
                 val alignment: Canvas.HAlignment = Canvas.HAlignment.LEFT)

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

    fun withRowHeight(rowHeight: Int) =
      PaintState(rowHeight).also {
        it.rowNumber = this.rowNumber
        it.y = this.y
      }
  }
}

// Because of hysterical raisins the height of the timeline area in the chart is 1 pixel less
// (see TimelineSceneBuilder). This might be due to the legacy technology of painting Swing tree component
// instead of building our own canvas model. We apply decrement here too.
val HEADER_HEIGHT_DECREMENT = 1
val TEXT_PADDING = 5
