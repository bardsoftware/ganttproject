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

import biz.ganttproject.core.model.task.CapacityChange
import biz.ganttproject.core.model.task.CapacitySchedule
import biz.ganttproject.core.model.task.daysNeeded
import biz.ganttproject.core.model.task.formatCapacityChanges
import biz.ganttproject.core.model.task.parseCapacityChanges
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

class CapacityScheduleTest {
  private val monday = LocalDate.of(2026, 11, 2)
  private fun hours(value: String) = BigDecimal(value)
  private fun change(date: String, hours: String) = CapacityChange(LocalDate.parse(date), BigDecimal(hours))
  private val weekdays: (LocalDate) -> Boolean = { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }

  @Test
  fun `before the first change the base counts, from a change on its hours`() {
    val schedule = CapacitySchedule(hours("8"), listOf(change("2026-11-04", "6"), change("2026-12-01", "4")))
    assertEquals(hours("8"), schedule.hoursOn(LocalDate.parse("2026-11-03")))
    assertEquals(hours("6"), schedule.hoursOn(LocalDate.parse("2026-11-04")))
    assertEquals(hours("6"), schedule.hoursOn(LocalDate.parse("2026-11-30")))
    assertEquals(hours("4"), schedule.hoursOn(LocalDate.parse("2026-12-01")))
  }

  @Test
  fun `changes are sorted and the later of two entries for one date wins`() {
    val schedule = CapacitySchedule(hours("8"), listOf(change("2026-12-01", "4"), change("2026-11-04", "6"), change("2026-11-04", "5")))
    assertEquals(listOf(change("2026-11-04", "5"), change("2026-12-01", "4")), schedule.changes)
  }

  @Test
  fun `a schedule without changes is constant`() {
    assertTrue(CapacitySchedule(hours("8")).isConstant)
  }

  @Test
  fun `a task over a change is counted with the hours of each day`() {
    val schedule = CapacitySchedule(hours("4"), listOf(change("2026-11-04", "8")))
    // 4 + 4 + 8 + 8 = 24 >= 20, after three days there are only 16
    assertEquals(4, daysNeeded(hours("20"), monday, schedule, weekdays))
    assertEquals(3, daysNeeded(hours("16"), monday, schedule, weekdays))
  }

  @Test
  fun `days off are skipped`() {
    val friday = LocalDate.of(2026, 11, 6)
    assertEquals(2, daysNeeded(hours("16"), friday, CapacitySchedule(hours("8")), weekdays))
  }

  @Test
  fun `a period without hours counts as days of the task`() {
    val schedule = CapacitySchedule(hours("8"), listOf(change("2026-11-02", "0"), change("2026-11-09", "8")))
    assertEquals(6, daysNeeded(hours("8"), monday, schedule, weekdays))
  }

  @Test
  fun `hours which never come end in null instead of a loop`() {
    val schedule = CapacitySchedule(hours("8"), listOf(change("2026-11-02", "0")))
    assertNull(daysNeeded(hours("8"), monday, schedule, weekdays))
    assertNull(daysNeeded(hours("8"), monday, schedule) { false })
  }

  @Test
  fun `negative hours do not add effort`() {
    val schedule = CapacitySchedule(hours("8"), listOf(change("2026-11-02", "-4"), change("2026-11-04", "8")))
    assertEquals(3, daysNeeded(hours("8"), monday, schedule, weekdays))
  }

  @Test
  fun `no effort takes one day`() {
    assertEquals(1, daysNeeded(BigDecimal.ZERO, monday, CapacitySchedule(hours("8")), weekdays))
  }

  @Test
  fun `a fraction of a day is a whole day`() {
    assertEquals(2, daysNeeded(hours("8.5"), monday, CapacitySchedule(hours("8")), weekdays))
  }

  @Test
  fun `typed changes are read with either separator, a decimal comma and line breaks`() {
    assertEquals(listOf(change("2027-04-01", "6"), change("2027-10-01", "7.5"), change("2027-12-01", "0")),
      parseCapacityChanges("2027-04-01: 6; 2027-10-01 = 7,5\n2027-12-01:0"))
    assertEquals(emptyList<CapacityChange>(), parseCapacityChanges("  "))
  }

  @Test
  fun `a typed text with one invalid entry is not read at all`() {
    listOf("2027-04-01", "04/01/2027: 6", "2027-04-01: six", "2027-04-01: -1", "2027-04-01: 24.5", "2027-04-01: 6; oops").forEach {
      assertNull(parseCapacityChanges(it), it)
    }
    assertEquals(listOf(change("2027-04-01", "24")), parseCapacityChanges("2027-04-01: 24"))
  }

  @Test
  fun `formatted changes are read back`() {
    val changes = listOf(change("2027-04-01", "6.50"), change("2027-10-01", "8"))
    assertEquals("2027-04-01: 6.5; 2027-10-01: 8", formatCapacityChanges(changes))
    assertEquals(listOf(change("2027-04-01", "6.5"), change("2027-10-01", "8")), parseCapacityChanges(formatCapacityChanges(changes)))
  }
}
