/*
Copyright 2026

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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.BaselineGap
import net.sourceforge.ganttproject.GanttPreviousState
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.algorithm.LevellingRunNotifier
import net.sourceforge.ganttproject.task.algorithm.LevellingStaleness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The part of the status bar message which says that tasks are missing in the baseline. */
class LevellingDueBarBaselineTest {
  private val notifier = LevellingRunNotifier()
  private val baseline = GanttPreviousState("Release 1", emptyList())

  private fun withBar(
    gapOf: () -> BaselineGap, runCatchUp: (() -> Unit) -> Unit = { it() },
    body: (LevellingStaleness, LevellingDueBar) -> Unit
  ) = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val staleness = LevellingStaleness(notifier)
      val bar = LevellingDueBar(staleness, {}, gapOf, runCatchUp)
      try {
        body(staleness, bar)
      } finally {
        bar.detach()
        staleness.detach()
      }
    }
  }

  private fun missing(count: Int) = BaselineGap.Missing(baseline, List(count) { mockTask() })

  private fun mockTask() = TestSetupHelper.newTaskManagerBuilder().build()
    .newTaskBuilder().withName("t").build()

  @Test
  fun `nothing is computed while the message is hidden`() {
    var asked = 0
    withBar({ asked++; missing(1) }) { _, bar ->
      assertFalse(bar.baselineLabel.isManaged)
      assertFalse(bar.baselineButton.isManaged)
    }
    assertEquals(0, asked)
  }

  @Test
  fun `missing tasks are shown with a button when the message appears`() {
    val gap = missing(2)
    withBar({ gap }) { staleness, bar ->
      staleness.markStale()
      assertTrue(bar.baselineLabel.isVisible && bar.baselineLabel.isManaged)
      assertTrue(bar.baselineButton.isVisible && bar.baselineButton.isManaged)
      assertEquals(RootLocalizer.formatText("baseline.missing", 2, "Release 1"), bar.baselineLabel.text)
    }
  }

  @Test
  fun `without a baseline there is a statement and no button`() = withBar({ BaselineGap.NoBaseline }) { staleness, bar ->
    staleness.markStale()
    assertTrue(bar.baselineLabel.isManaged)
    assertEquals(RootLocalizer.formatText("baseline.missing.none"), bar.baselineLabel.text)
    assertFalse(bar.baselineButton.isManaged)
  }

  @Test
  fun `a covered baseline shows nothing, and the levelling part stays`() = withBar({ BaselineGap.Covered }) { staleness, bar ->
    staleness.markStale()
    assertFalse(bar.baselineLabel.isManaged)
    assertFalse(bar.baselineButton.isManaged)
    assertTrue(bar.label.isVisible)
    assertTrue(bar.button.isVisible)
  }

  @Test
  fun `the button catches up and the hint goes away`() {
    var gap: BaselineGap = missing(1)
    var runs = 0
    withBar({ gap }, { onDone -> runs++; gap = BaselineGap.Covered; onDone() }) { staleness, bar ->
      staleness.markStale()
      bar.baselineButton.fire()
      assertFalse(bar.baselineLabel.isManaged)
      assertFalse(bar.baselineButton.isManaged)
    }
    assertEquals(1, runs)
  }
}
