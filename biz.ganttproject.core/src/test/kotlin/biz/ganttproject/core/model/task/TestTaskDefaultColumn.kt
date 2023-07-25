/*
Copyright 2023 BarD Software s.r.o

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
package biz.ganttproject.core.model.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestTaskDefaultColumn {
  @Test
  fun `outline number comparator`() {
    assertEquals(-1, TaskDefaultColumn.Functions.OUTLINE_NUMBER_COMPARATOR.compare("1", "10"))
    assertEquals(-1, TaskDefaultColumn.Functions.OUTLINE_NUMBER_COMPARATOR.compare("1", "1.1"))
    assertEquals(-1, TaskDefaultColumn.Functions.OUTLINE_NUMBER_COMPARATOR.compare("2", "10"))
    assertEquals(-1, TaskDefaultColumn.Functions.OUTLINE_NUMBER_COMPARATOR.compare("2.1", "10"))
    assertEquals(1, TaskDefaultColumn.Functions.OUTLINE_NUMBER_COMPARATOR.compare("2.2", "2.1.1"))
  }
}