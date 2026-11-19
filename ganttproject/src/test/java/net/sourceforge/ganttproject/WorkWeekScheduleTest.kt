/*
Copyright 2026 BarD Software s.r.o., GanttProject Team

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
package net.sourceforge.ganttproject

import biz.ganttproject.core.model.task.WorkWeekChange
import biz.ganttproject.core.model.task.WorkWeekSchedule
import biz.ganttproject.core.model.task.parseWorkWeek
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate

/** Reading a typed working week and finding the change in force on a day. */
class WorkWeekScheduleTest {
  private val march = LocalDate.of(2027, 3, 1)
  private val may = LocalDate.of(2027, 5, 3)

  @Test
  fun `weekday numbers follow ISO, from 1 for Monday to 7 for Sunday, and spaces do not matter`() {
    assertEquals(listOf(WorkWeekChange(null, setOf(MONDAY, SUNDAY))), parseWorkWeek(" 1 , 7 "))
  }

  @Test
  fun `changes typed out of order are sorted by date, the undated one first`() {
    val schedule = WorkWeekSchedule(parseWorkWeek("2027-05-03: 5; 1,2; 2027-03-01: 3")!!)
    assertEquals(listOf(null, march, may), schedule.changes.map { it.from })
  }

  @Test
  fun `of two changes with the same date the later entry wins`() {
    val schedule = WorkWeekSchedule(parseWorkWeek("2027-03-01: 1; 2027-03-01: 2")!!)
    assertEquals(listOf(WorkWeekChange(march, setOf(TUESDAY))), schedule.changes)
  }

  @Test
  fun `between two dated changes the second applies on its date, not a day earlier`() {
    val schedule = WorkWeekSchedule(parseWorkWeek("2027-03-01: 3; 2027-05-03: 5")!!)
    assertEquals(setOf(WEDNESDAY), schedule.sectionOn(may.minusDays(1))?.days)
    assertEquals(setOf(FRIDAY), schedule.sectionOn(may)?.days)
    assertEquals(null, schedule.sectionOn(march.minusDays(1)))
  }
}
