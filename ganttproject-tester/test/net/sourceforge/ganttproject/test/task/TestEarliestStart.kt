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
package net.sourceforge.ganttproject.test.task

import junit.framework.TestCase
import net.sourceforge.ganttproject.TestSetupHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestEarliestStart: TaskTestCase() {
  @BeforeEach
  override fun setUp() {
    super.setUp()
  }
  @Test
  fun `test earliest start enable-disable`() {
    val monday = TestSetupHelper.newMonday()
    val tuesday = TestSetupHelper.newTuesday()
    val wednesday = TestSetupHelper.newWendesday()

    val task = createTask(monday)
    task.createMutator().let {
      it.setThird(wednesday, 1)
      it.commit()
    }
    assertEquals(wednesday, task.third)
    assertEquals(1, task.thirdDateConstraint)

    task.createMutator().let {
      it.setThird(tuesday, 0)
      it.commit()
    }
    assertNull(task.third)
    assertEquals(0, task.thirdDateConstraint)
  }

  @Test fun `test earliest start change triggers scheduler`() {
    val monday = TestSetupHelper.newMonday()
    val wednesday = TestSetupHelper.newWendesday()

    val task = createTask(monday)
    task.createMutator().let {
      it.setThird(wednesday, 1)
      it.commit()
    }
    TestCase.assertEquals(wednesday, task.start)
  }
}