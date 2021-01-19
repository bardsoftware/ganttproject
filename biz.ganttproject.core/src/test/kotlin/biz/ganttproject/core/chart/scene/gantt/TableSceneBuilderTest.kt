package biz.ganttproject.core.chart.scene.gantt

import org.junit.Test
import org.mockito.Mockito.*

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.scene.gantt.TableSceneBuilder.*

class TableSceneBuilderTest {
  @Test
  fun `test width when max width comes from large indent row`() {
    val rowHeight = 10
    val horizontalOffset = 5
    val rows = listOf(
      Row(10, "", 0), Row(1, "", 10), Row(4, "", 5),
    )
    test(rowHeight, horizontalOffset, rows)
  }

  @Test
  fun `test width when max width comes from large text width row`() {
    val rowHeight = 10
    val horizontalOffset = 5
    val rows = listOf(
      Row(12, "", 0), Row(1, "", 10), Row(4, "", 5),
    )
    test(rowHeight, horizontalOffset, rows)
  }

  private fun test(rowHeight: Int, horizontalOffset: Int, rows: List<Row>) {
    val canvas = spy(Canvas())
    val sceneBuilder = TableSceneBuilder(Config(rowHeight, horizontalOffset))
    sceneBuilder.build(rows, canvas)
    val expectedWidth = rows.map { it.indent + it.width + 2 * horizontalOffset }.maxOrNull() ?: 0

    rows.forEachIndexed { i, row ->
      verify(canvas).createRectangle(0, i * rowHeight, expectedWidth, rowHeight)
    }
  }
}
