/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.app

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BarriersTest {

  @Test
  fun `two phase barrier exit registered before entrance`() {
    var exitCalled = false
    val barrier = TwoPhaseBarrierImpl<Boolean>("Bar1")
    barrier.await {
      assertTrue(it)
      exitCalled = true
    }
    assertFalse(exitCalled)

    val entrance = barrier.register("Entrance activity")
    barrier.activate(true)
    entrance()
    assertTrue(exitCalled)
  }

  @Test
  fun `two phase barrier exit registered after entrance`() {
    var exitCalled = false
    val barrier = TwoPhaseBarrierImpl<Boolean>("Bar1")
    val entrance = barrier.register("Entrance activity")
    barrier.activate(true)
    entrance()
    barrier.await {
      assertTrue(it)
      exitCalled = true
    }
    assertTrue(exitCalled)
  }

  @Test
  fun `two phase barrier can't be activated twice`() {
    val barrier = TwoPhaseBarrierImpl<Boolean>("Bar1")
    barrier.activate(true)
    assertThrows<IllegalStateException> {
      barrier.activate(false)
    }
  }

  @Test
  fun `exits are called immediately upon activation`() {
    var exitCalled = false
    val barrier = TwoPhaseBarrierImpl<Boolean>("Bar1")
    barrier.await {
      assertTrue(it)
      exitCalled = true
    }
    barrier.activate(true)
    assertTrue(exitCalled)
  }
}