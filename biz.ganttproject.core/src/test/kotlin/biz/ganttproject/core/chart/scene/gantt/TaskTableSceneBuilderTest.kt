package biz.ganttproject.core.chart.scene.gantt

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.TextMetrics
import biz.ganttproject.core.chart.scene.gantt.TaskTableSceneBuilder.*

import org.junit.Test
import org.mockito.Mockito.*
import java.awt.Font

class TaskTableSceneBuilderTest {
  @Test
  fun `test indents`() {
    val input = object : InputApi {
      override val textMetrics = TextMetricsStub
      override val rowHeight = 10
      override val depthIndent = 15
      override val horizontalOffset = 2
    }
    val sceneBuilder = TaskTableSceneBuilder(input)
    val tasks = listOf(
      Task("1"),
      Task("2", listOf(
        Task("3"), Task("4", listOf(Task("5")))
      ))
    )
    val canvas = spy(Canvas())
    sceneBuilder.build(tasks, canvas)

    verify(canvas).createText(input.horizontalOffset, 5, "1")
    verify(canvas).createText(input.horizontalOffset, 15, "2")
    verify(canvas).createText(input.horizontalOffset + input.depthIndent, 25, "3")
    verify(canvas).createText(input.horizontalOffset + input.depthIndent, 35, "4")
    verify(canvas).createText(input.horizontalOffset + input.depthIndent * 2, 45, "5")
  }
}

private object TextMetricsStub : TextMetrics {
  override fun getTextLength(text: String) = text.length * 7

  override fun getTextHeight(text: String) = 10

  override fun getTextHeight(font: Font, text: String) = font.size

  override fun getState() = Object()
}
