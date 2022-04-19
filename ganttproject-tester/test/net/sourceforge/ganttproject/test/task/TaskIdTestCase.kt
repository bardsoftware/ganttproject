/*
Copyright 2022 BarD Software s.r.o.

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
package net.sourceforge.ganttproject.test.task

import net.sourceforge.ganttproject.TestSetupHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TaskIdTestCase {
  @Test
  fun `UID is different when ID is the same`() {
    val taskManager1 = TestSetupHelper.newTaskManagerBuilder().build()
    val taskManager2 = TestSetupHelper.newTaskManagerBuilder().build()
    val task1 = taskManager1.newTaskBuilder().build()
    val task2 = taskManager2.newTaskBuilder().build()
    assertEquals(task1.taskID, task2.taskID)
    assertNotEquals(task1.uid, task2.uid)
  }

}