package biz.ganttproject.core.chart.scene

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.TextMetrics
import java.awt.Color
import java.awt.Font
import java.lang.Integer.max

class TaskTableSceneBuilder(
  private val rows: List<RowContent>,
  private val input: InputApi,
  val canvas: Canvas = Canvas()
) {
  private val dimensions = calculateDimensions()

  fun build() {
    canvas.clear()
    val rectangle = canvas.createRectangle(0, 0, dimensions.width, dimensions.height)
    rectangle.backgroundColor = Color.WHITE
    paintTasks(rows, PaintState(input.horizontalOffset, 0, 0))
  }

  private fun calculateDimensions(rows: List<RowContent>? = null): Dimension {
    var height = 0
    var width = 0

    (rows ?: this.rows).forEach {
      height += input.rowHeight
      width = max(width, it.getWidth(input.textMetrics))
      if (it is Task) {
        val subtasksDimensions = calculateDimensions(it.subrows)
        height += subtasksDimensions.height
        width = max(width, subtasksDimensions.width + input.depthIndent)
      }
    }

    if (rows == null) {
      width += 2 * input.horizontalOffset
    }
    return Dimension(height, width)
  }

  private fun paintTasks(rows: List<RowContent>, initState: PaintState): PaintState {
    var state = initState
    rows.forEach {
      val rectangle = canvas.createRectangle(
        0, state.y, dimensions.width, input.rowHeight
      )
      if (state.rowNumber % 2 == 1) {
        rectangle.style = "odd-row"
      }

      if (it is Task) {
        paintTask(it, state.x, rectangle.middleY)
        val subtasksState = paintTasks(it.subrows, state.toNextRow(input.depthIndent))
        subtasksState.x = state.x
        state = subtasksState
      } else {
        state = state.toNextRow(0)
      }
    }
    return state
  }

  private fun paintTask(task: Task, x: Int, y: Int) {
    val text = canvas.createText(x, y, task.name)
    text.setAlignment(Canvas.HAlignment.LEFT, Canvas.VAlignment.CENTER)
  }

  interface RowContent {
    fun getWidth(metrics: TextMetrics): Int
  }
  class BlankRow : RowContent {
    override fun getWidth(metrics: TextMetrics) = 0
  }
  class Task(val name: String, val subrows: List<RowContent> = emptyList()) : RowContent {
    override fun getWidth(metrics: TextMetrics) = metrics.getTextLength(name)
  }

  interface InputApi {
    val textMetrics: TextMetrics
    val rowHeight: Int
    val depthIndent: Int
    val horizontalOffset: Int
  }

  private data class Dimension(val height: Int, val width: Int)

  private inner class PaintState(var x: Int = 0, val y: Int = 0, val rowNumber: Int) {
    fun toNextRow(dx: Int = 0): PaintState = PaintState(x + dx, y + input.rowHeight, rowNumber + 1)
  }
}

object TextMetricsStub : TextMetrics {
  override fun getTextLength(text: String) = text.length * 7

  override fun getTextHeight(text: String) = 10

  override fun getTextHeight(font: Font, text: String) = font.size

  override fun getState() = Object()
}
