/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

This file is part of GanttProject, an opensource project management tool.

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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.task.algorithm.LevellingRunNotifier
import net.sourceforge.ganttproject.task.algorithm.LevellingStaleness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The status bar message which says that the levelling is due. */
class LevellingDueBarTest {
  private val notifier = LevellingRunNotifier()

  private fun withBar(runLevelling: () -> Unit = {}, body: (LevellingStaleness, LevellingDueBar) -> Unit) =
    runBlocking {
      withContext(Dispatchers.JavaFx) {
        val staleness = LevellingStaleness(notifier)
        val bar = LevellingDueBar(staleness, runLevelling)
        try {
          body(staleness, bar)
        } finally {
          bar.detach()
          staleness.detach()
        }
      }
    }

  @Test
  fun `with a clear mark the message is invisible and takes no space`() = withBar { _, bar ->
    assertFalse(bar.node.isVisible)
    assertFalse(bar.node.isManaged)
  }

  @Test
  fun `a change of the plan shows the message`() = withBar { staleness, bar ->
    staleness.markStale()
    assertTrue(bar.node.isVisible)
    assertTrue(bar.node.isManaged)
  }

  @Test
  fun `the message goes away when the mark is cleared`() = withBar { staleness, bar ->
    staleness.markStale()
    staleness.clear()
    assertFalse(bar.node.isVisible)
    assertFalse(bar.node.isManaged)
  }

  @Test
  fun `the button runs the levelling`() {
    var runs = 0
    withBar({ runs++ }) { staleness, bar ->
      staleness.markStale()
      bar.button.fire()
    }
    assertEquals(1, runs)
  }

  @Test
  fun `a levelling run hides the message`() = withBar { staleness, bar ->
    staleness.markStale()
    assertTrue(bar.node.isVisible)
    notifier.runAndReport { 0 }
    assertFalse(bar.node.isVisible)
    assertFalse(bar.node.isManaged)
  }

  @Test
  fun `the message and the button have a tooltip of their own`() = withBar { _, bar ->
    val labelTip = bar.label.tooltip?.text
    val buttonTip = bar.button.tooltip?.text
    assertTrue(!labelTip.isNullOrBlank())
    assertTrue(!buttonTip.isNullOrBlank())
    assertTrue(labelTip != buttonTip)
  }

  @Test
  fun `a key without a text is shown as the key itself`() {
    assertEquals("levelling.due.no-such-key", RootLocalizer.formatText("levelling.due.no-such-key"))
  }
}
