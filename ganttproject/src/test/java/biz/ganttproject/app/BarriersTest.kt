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

class BarriersTest {

  @Test
  fun `two phase barrier exit registered before entrance`() {
    var exitCalled = false
    val barrier = TwoPhaseBarrierImpl<Boolean>(true)
    barrier.await {
      assertTrue(it)
      exitCalled = true
    }
    assertFalse(exitCalled)

    barrier.register("Entrance activity").let { entrance -> entrance() }
    assertTrue(exitCalled)
  }

  @Test
  fun `two phase barrier exit registered after entrance`() {
    var exitCalled = false
    val barrier = TwoPhaseBarrierImpl<Boolean>(true)
    barrier.register("Entrance activity").let { entrance -> entrance() }
    barrier.await {
      assertTrue(it)
      exitCalled = true
    }
    assertTrue(exitCalled)
  }

}