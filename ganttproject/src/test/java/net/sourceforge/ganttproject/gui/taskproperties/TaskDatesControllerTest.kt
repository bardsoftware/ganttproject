/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui.taskproperties

import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.option.ObservableDate
import net.sourceforge.ganttproject.TestSetupHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.w3c.util.DateParser
import java.time.LocalDate

class TaskDatesControllerTest {
  @Test
  fun `basic dates controller test`() {
    val task = TestSetupHelper.newTaskManagerBuilder().build().newTaskBuilder().withName("task1").build()
    val milestoneOption = ObservableBoolean("foo", false)
    TaskDatesController(task, milestoneOption).let {
      it.startDateOption.set(LocalDate.parse("2025-09-22"), "picker")
      assertEquals("2025-09-22", it.startDateOption.toIsoDate())
      assertEquals("2025-09-22", it.displayEndDateOption.toIsoDate())
      assertEquals("2025-09-23", it.endDateOption.toIsoDate())
    }

  }
}

private fun ObservableDate.toIsoDate() = this.value?.let(DateParser::toJavaDate)?.let(DateParser::getIsoDateNoHours)