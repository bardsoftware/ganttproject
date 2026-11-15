/*
Copyright 2026 BarD Software s.r.o

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
package biz.ganttproject.app

import javafx.embed.swing.SwingNode
import javafx.scene.Scene
import javafx.scene.control.SplitPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.javafx.JavaFx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.SwingUtilities

class ChartNodeFitTest {
  /** The room the chart had in the measured window of 1900x1040 with the table divider at 500. */
  private val chartWidth = 1396.0
  private val chartHeight = 851.0

  @Test
  fun `fitting the chart node from the Swing thread leaves it as large as its pane`() {
    val chartNode = SwingNode()
    val components = runBlocking {
      withContext(Dispatchers.JavaFx) {
        val pane = VBox(chartNode)
        VBox.setVgrow(chartNode, Priority.ALWAYS)
        Scene(pane, chartWidth, chartHeight)
        pane.resize(chartWidth, chartHeight)
        pane.layout()
        ViewComponents(image = Pane(), splitPane = SplitPane(), table = Pane(), chartNode = chartNode)
      }
    }
    // The chart prefers a much smaller size than it gets, like the Gantt chart does.
    SwingUtilities.invokeAndWait {
      chartNode.content = JPanel().also { it.preferredSize = Dimension(465, 600) }
    }
    // The chart refresh calls it from the Swing thread.
    SwingUtilities.invokeAndWait {
      components.fitChartNode()
    }
    val size = runBlocking {
      withContext(Dispatchers.JavaFx) {}
      withContext(Dispatchers.JavaFx) {
        chartNode.layoutBounds.width to chartNode.layoutBounds.height
      }
    }
    assertEquals(chartWidth to chartHeight, size, "the chart node must fill its pane after it was fitted")
  }
}
