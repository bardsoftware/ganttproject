/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

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
package biz.ganttproject.core.model.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class TestRecurrenceRule {
  private val everyDay: (LocalDate) -> Boolean = { true }
  private val weekdaysOnly: (LocalDate) -> Boolean = { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }

  // 2026-11-09 is a Monday.
  private val monday = LocalDate.of(2026, 11, 9)

  @Test
  fun `monthly until a date includes the first date and the last one on the end date`() {
    val rule = RecurrenceRule(RecurrenceUnit.MONTH, until = LocalDate.of(2027, 1, 9))
    assertEquals(
      listOf(monday, LocalDate.of(2026, 12, 9), LocalDate.of(2027, 1, 9)),
      rule.occurrences(monday, everyDay))
  }

  @Test
  fun `count includes the first date`() {
    val rule = RecurrenceRule(RecurrenceUnit.WEEK, count = 3)
    assertEquals(
      listOf(monday, monday.plusWeeks(1), monday.plusWeeks(2)),
      rule.occurrences(monday, everyDay))
  }

  @Test
  fun `interval skips units`() {
    val rule = RecurrenceRule(RecurrenceUnit.WEEK, interval = 2, count = 3)
    assertEquals(listOf(monday, monday.plusWeeks(2), monday.plusWeeks(4)), rule.occurrences(monday, everyDay))
  }

  @Test
  fun `a date on a non working day moves forward to the next working day`() {
    // 2026-12-12 is a Saturday.
    val rule = RecurrenceRule(RecurrenceUnit.DAY, interval = 32, count = 2)
    val first = LocalDate.of(2026, 11, 10)
    assertEquals(LocalDate.of(2026, 12, 12), first.plusDays(32))
    assertEquals(listOf(first, LocalDate.of(2026, 12, 14)), rule.occurrences(first, weekdaysOnly))
  }

  @Test
  fun `the end is checked against the date before the move`() {
    // The raw date 2026-12-12 is a Saturday and is not after the end 2026-12-12, so it belongs to the series,
    // although it moves to Monday the 14th which is after the end.
    val rule = RecurrenceRule(RecurrenceUnit.DAY, interval = 32, until = LocalDate.of(2026, 12, 12))
    assertEquals(2, rule.occurrences(LocalDate.of(2026, 11, 10), weekdaysOnly).size)
  }

  @Test
  fun `dates which collapse onto the same working day are kept once and do not count twice`() {
    val rule = RecurrenceRule(RecurrenceUnit.DAY, count = 4)
    // Friday, Saturday and Sunday all give Friday, Monday; the Monday collapses with the Sunday date.
    val friday = LocalDate.of(2026, 11, 13)
    assertEquals(
      listOf(friday, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 18)),
      rule.occurrences(friday, weekdaysOnly))
  }

  @Test
  fun `monthly from the end of a month follows the shorter months`() {
    val rule = RecurrenceRule(RecurrenceUnit.MONTH, count = 3)
    assertEquals(
      listOf(LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31)),
      rule.occurrences(LocalDate.of(2027, 1, 31), everyDay))
  }

  @Test
  fun `an end before the start is a problem and gives no dates after the first`() {
    val rule = RecurrenceRule(RecurrenceUnit.WEEK, until = monday.minusDays(1))
    assertEquals(RecurrenceProblem.END_BEFORE_START, rule.problemFor(monday, everyDay))
    assertEquals(emptyList<LocalDate>(), rule.occurrences(monday, everyDay))
  }

  @Test
  fun `an end on the start date is fine and gives only the first date`() {
    val rule = RecurrenceRule(RecurrenceUnit.WEEK, until = monday)
    assertNull(rule.problemFor(monday, everyDay))
    assertEquals(listOf(monday), rule.occurrences(monday, everyDay))
  }

  @Test
  fun `the limit is reached exactly at the maximum and exceeded above it`() {
    val max = RecurrenceRule.MAX_OCCURRENCES
    assertNull(RecurrenceRule(RecurrenceUnit.DAY, count = max).problemFor(monday, everyDay))
    assertEquals(max, RecurrenceRule(RecurrenceUnit.DAY, count = max).occurrences(monday, everyDay).size)
    val above = RecurrenceRule(RecurrenceUnit.DAY, count = max + 1)
    assertEquals(RecurrenceProblem.TOO_MANY, above.problemFor(monday, everyDay))
    assertEquals(max, above.occurrences(monday, everyDay).size, "the list is cut at the maximum")
    val untilFarAway = RecurrenceRule(RecurrenceUnit.DAY, until = LocalDate.of(2099, 1, 1))
    assertEquals(RecurrenceProblem.TOO_MANY, untilFarAway.problemFor(monday, weekdaysOnly))
  }

  @Test
  fun `a rule needs exactly one end and a positive interval and count`() {
    assertThrows(IllegalArgumentException::class.java) { RecurrenceRule(RecurrenceUnit.DAY) }
    assertThrows(IllegalArgumentException::class.java) {
      RecurrenceRule(RecurrenceUnit.DAY, until = monday, count = 2)
    }
    assertThrows(IllegalArgumentException::class.java) { RecurrenceRule(RecurrenceUnit.DAY, interval = 0, count = 2) }
    assertThrows(IllegalArgumentException::class.java) { RecurrenceRule(RecurrenceUnit.DAY, count = 0) }
  }

  @Test
  fun `values read from a file make a rule or nothing`() {
    assertEquals(RecurrenceRule(RecurrenceUnit.WEEK, 2, null, 5), RecurrenceRule.fromXml("week", "2", null, "5"))
    assertEquals(RecurrenceRule(RecurrenceUnit.MONTH, 1, monday, null), RecurrenceRule.fromXml("month", null, "2026-11-09", null))
    assertNull(RecurrenceRule.fromXml(null, "1", null, "5"), "no unit")
    assertNull(RecurrenceRule.fromXml("fortnight", "1", null, "5"), "unknown unit")
    assertNull(RecurrenceRule.fromXml("week", "1", null, null), "no end")
    assertNull(RecurrenceRule.fromXml("week", "1", "2026-11-09", "5"), "two ends")
    assertNull(RecurrenceRule.fromXml("week", "1", "9.11.2026", null), "date is not ISO")
    assertNull(RecurrenceRule.fromXml("week", "0", null, "5"), "interval is zero")
    assertNull(RecurrenceRule.fromXml("week", "often", null, "5"), "interval is not a number")
  }

  @Test
  fun `an origin without a date is the group`() {
    assertNotNull(RecurrenceOrigin.fromXml("3", null)?.takeIf { it.isGroup })
    assertEquals(RecurrenceOrigin(3, monday), RecurrenceOrigin.fromXml("3", "2026-11-09"))
    assertNull(RecurrenceOrigin.fromXml(null, "2026-11-09"))
    assertNull(RecurrenceOrigin.fromXml("x", null))
    assertNull(RecurrenceOrigin.fromXml("3", "yesterday"))
  }
}
