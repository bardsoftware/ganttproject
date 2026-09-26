/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.chart.gantt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

/**
 * The rule which decides whether a baseline bar is drawn at all and which colour it gets. The
 * scenarios are written as (baseline start, baseline duration) against (current start, current
 * duration); the end dates are derived from them by shifting over calendar days.
 *
 * The colours follow the end date, because that is what a schedule is judged by. The only task
 * which gets a bar without a colour is the one which still ends on the planned day.
 */
class BaselineStylesTest {
  @Test
  fun `a task which did not change at all gets no baseline bar`() {
    assertNull(styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_10, currentDuration = 5))
  }

  @Test
  fun `a milestone which did not move gets no baseline bar`() {
    assertNull(
      styles(
        baselineStart = AUG_10, baselineDuration = 1, currentStart = AUG_10, currentDuration = 1, isMilestone = true
      )
    )
  }

  @Test
  fun `a task which ends later than planned keeps the later colour`() {
    assertEquals(
      listOf("later"),
      styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_10, currentDuration = 10)
    )
  }

  @Test
  fun `a task which ends sooner than planned keeps the earlier colour`() {
    assertEquals(
      listOf("earlier"),
      styles(baselineStart = AUG_10, baselineDuration = 10, currentStart = AUG_10, currentDuration = 5)
    )
  }

  @Test
  fun `a moved task is coloured by its new end date`() {
    // Three days later, same duration: the deadline is missed by three days, which is what the
    // colour says. This is the behaviour of the chart today and must not change.
    assertEquals(
      listOf("later"),
      styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_13, currentDuration = 5)
    )
  }

  @Test
  fun `a moved milestone is coloured by its new end date`() {
    assertEquals(
      listOf("milestone", "later"),
      styles(
        baselineStart = AUG_10, baselineDuration = 1, currentStart = AUG_13, currentDuration = 1, isMilestone = true
      )
    )
  }

  @Test
  fun `a task which grew but still ends on the planned day gets a bar without a colour`() {
    // The blind spot: the deadline is met, so no colour, but the task now takes twice as long and
    // the bar is the only place where that is visible.
    assertEquals(
      emptyList<String>(),
      styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_5, currentDuration = 10)
    )
  }

  @Test
  fun `a task which shrank but still ends on the planned day gets a bar without a colour`() {
    assertEquals(
      emptyList<String>(),
      styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_13, currentDuration = 2)
    )
  }
}

private val AUG_5 = date(2026, Calendar.AUGUST, 5)
private val AUG_10 = date(2026, Calendar.AUGUST, 10)
private val AUG_13 = date(2026, Calendar.AUGUST, 13)

private fun date(year: Int, month: Int, day: Int): Date = GregorianCalendar(year, month, day).time

/** Shifts by calendar days, which is what the scenarios above are written in. */
private fun shift(start: Date, days: Int): Date = GregorianCalendar().let {
  it.time = start
  it.add(Calendar.DAY_OF_YEAR, days)
  it.time
}

private fun styles(
  baselineStart: Date, baselineDuration: Int, currentStart: Date, currentDuration: Int, isMilestone: Boolean = false
) = GanttChartSceneBuilder.getBaselineStyles(
  isMilestone,
  baselineDuration,
  currentDuration,
  shift(baselineStart, baselineDuration),
  shift(currentStart, currentDuration)
)
