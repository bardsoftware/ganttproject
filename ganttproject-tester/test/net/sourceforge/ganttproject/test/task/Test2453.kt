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


class Test2453: TaskTestCase() {
  @BeforeEach
  override fun setUp() {
    super.setUp()
  }

  fun `test for issue 2453`() {
    val monday = TestSetupHelper.newMonday()

    val milestone = createTask(monday)
    milestone.isMilestone = true
    assertEquals(1, milestone.activities.first().duration.length)

    milestone.createMutator().let {
      it.setMilestone(false)
      it.setDuration(taskManager.createLength(3))
      it.commit()
    }
    assertEquals(3, milestone.duration.length)
    assertFalse(milestone.isMilestone)
    assertEquals(3, milestone.activities.first().duration.length)
  }
}