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
package net.sourceforge.ganttproject.chart

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class ChartComparisonTest {
  private fun day(iso: String): Date = LocalDate.parse(iso).asDate()

  private fun LocalDate.asDate(): Date = Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant())

  private fun hours(value: String) = BigDecimal(value)

  @Test
  fun `the dates view colors by the end date`() {
    assertEquals(ComparisonResult.NO_BAND, compareDates(day("2026-08-14"), day("2026-08-14"), 5, 5))
    assertEquals(ComparisonResult.MORE, compareDates(day("2026-08-14"), day("2026-08-17"), 5, 5))
    assertEquals(ComparisonResult.LESS, compareDates(day("2026-08-14"), day("2026-08-12"), 5, 5))
    assertEquals(ComparisonResult.NEUTRAL, compareDates(day("2026-08-14"), day("2026-08-14"), 5, 10),
      "same end and another duration gets a neutral band, as the baseline always did")
  }

  @Test
  fun `the durations view leaves a mere shift out`() {
    assertEquals(ComparisonResult.NO_BAND, compareDurations(5, 5))
    assertEquals(ComparisonResult.MORE, compareDurations(5, 7))
    assertEquals(ComparisonResult.LESS, compareDurations(5, 3))
  }

  @Test
  fun `without a baseline the durations view is neutral, not empty`() {
    assertEquals(ComparisonResult.NEUTRAL, compareDurations(null, 5))
  }

  @Test
  fun `a moved task is late in the dates view and unchanged in the durations view`() {
    assertEquals(ComparisonResult.MORE, compareDates(day("2026-08-07"), day("2026-08-14"), 5, 5))
    assertEquals(ComparisonResult.NO_BAND, compareDurations(5, 5))
  }

  @Test
  fun `the dates and durations view tells a shift from a growth`() {
    val moved = compareDatesAndDurations(day("2026-09-04"), day("2026-09-11"), 5, 5)
    val grown = compareDatesAndDurations(day("2026-09-04"), day("2026-09-11"), 5, 10)
    assertEquals(ComparisonResult.MORE, moved.dates)
    assertEquals(ComparisonResult.MORE, grown.dates)
    assertEquals(ComparisonResult.NO_BAND, moved.durations)
    assertEquals(ComparisonResult.MORE, grown.durations)
    assertNotEquals(moved, grown)
  }

  @Test
  fun `without a baseline only the durations half is drawn, and it is neutral`() {
    val both = compareDatesAndDurations(null, day("2026-09-11"), null, 5)
    assertEquals(DatesAndDurations(ComparisonResult.NO_BAND, ComparisonResult.NEUTRAL), both,
      "no dates to compare, so no dates half, as in the dates view")
  }

  /**
   * In the dates and durations view the durations half starts with the task bar. A dates half drawn alone means equal
   * durations and another end, hence another start: it can never sit flush with the bar, so a lone strip tells by its
   * position which half it is.
   */
  @Test
  fun `a lone dates half never starts with the bar`() {
    val plannedStart = LocalDate.of(2026, 8, 31)
    var loneDatesHalves = 0
    for (plannedDays in 1..8) {
      for (actualDays in 1..8) {
        for (shift in -4..4) {
          val actualStart = plannedStart.plusDays(shift.toLong())
          val both = compareDatesAndDurations(
            plannedStart.plusDays(plannedDays.toLong()).asDate(), actualStart.plusDays(actualDays.toLong()).asDate(),
            plannedDays, actualDays
          )
          if (both.dates != ComparisonResult.NO_BAND && both.durations == ComparisonResult.NO_BAND) {
            loneDatesHalves++
            assertNotEquals(plannedStart, actualStart, "planned $plannedDays days, actually $actualDays from $actualStart")
          }
        }
      }
    }
    assertTrue(loneDatesHalves > 0, "the grid has to contain lone dates halves")
  }

  @Test
  fun `the effort view compares the actual effort with the original effort`() {
    assertEquals(ComparisonResult.MORE, compareEffort(hours("8"), hours("12.5")))
    assertEquals(ComparisonResult.LESS, compareEffort(hours("8"), hours("6")))
    assertEquals(ComparisonResult.NO_BAND, compareEffort(hours("8"), hours("8.0")))
  }

  @Test
  fun `without an original effort the effort view draws nothing`() {
    assertEquals(ComparisonResult.NO_BAND, compareEffort(null, hours("5")))
    assertEquals(ComparisonResult.NO_BAND, compareEffort(BigDecimal.ZERO, hours("5")))
  }

  @Test
  fun `an original effort without actual effort is neutral`() {
    assertEquals(ComparisonResult.NEUTRAL, compareEffort(hours("8"), null))
    assertEquals(ComparisonResult.NEUTRAL, compareEffort(hours("8"), BigDecimal.ZERO))
  }

  @Test
  fun `less than a minute is no difference, two minutes are`() {
    assertEquals(ComparisonResult.NO_BAND, compareEffort(hours("8"), hours("8.008")))
    assertEquals(ComparisonResult.MORE, compareEffort(hours("8"), hours("8.034")))
  }

  @Test
  fun `the views that draw without a baseline need room in the row`() {
    assertFalse(ChartComparison.DATES.needsBandRoomWithoutBaseline)
    assertTrue(ChartComparison.EFFORT.needsBandRoomWithoutBaseline)
    assertTrue(ChartComparison.DURATIONS.needsBandRoomWithoutBaseline)
    assertTrue(ChartComparison.DATES_AND_DURATIONS.needsBandRoomWithoutBaseline)
  }

  @Test
  fun `the style names are the baseline ones`() {
    assertEquals("later", ComparisonResult.MORE.styleName)
    assertEquals("earlier", ComparisonResult.LESS.styleName)
    assertNull(ComparisonResult.NEUTRAL.styleName)
    assertNull(ComparisonResult.NO_BAND.styleName)
  }
}
