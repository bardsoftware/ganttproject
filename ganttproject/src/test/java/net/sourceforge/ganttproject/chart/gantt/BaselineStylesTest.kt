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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

/**
 * The rule which decides whether a baseline bar is drawn at all and which colour it gets.
 * The scenarios are written as (baseline start, baseline duration) against (current start,
 * current duration); the end dates are derived from them.
 */
class BaselineStylesTest {
  @Test
  fun `unchanged task gets no baseline bar`() {
    assertNull(styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_10, currentDuration = 5))
  }

  @Test
  fun `moved task with unchanged duration is not coloured as a deviation`() {
    // Starts three days later, lasts exactly as long. The bar shows where the task used to be.
    val styles = styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_13, currentDuration = 5)
    assertNotNull(styles)
    assertFalse(styles!!.contains("later"), "a task which only moved must not be coloured as taking longer")
    assertFalse(styles.contains("earlier"), "a task which only moved must not be coloured as taking less time")
  }

  @Test
  fun `task which starts earlier and ends on the same day gets a bar and the longer colour`() {
    // The blind spot: the end date does not move, but the task lasts twice as long.
    val styles = styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_3, currentDuration = 10)
    assertNotNull(styles, "a task whose duration grew must get a baseline bar even if its end date did not move")
    assertTrue(styles!!.contains("later"))
  }

  @Test
  fun `task which really takes longer keeps the longer colour`() {
    // Same start, later end: the case which is coloured correctly today and must stay so.
    val styles = styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_10, currentDuration = 10)
    assertEquals(listOf("later"), styles)
  }

  @Test
  fun `task which takes less time keeps the shorter colour`() {
    val styles = styles(baselineStart = AUG_10, baselineDuration = 10, currentStart = AUG_10, currentDuration = 5)
    assertEquals(listOf("earlier"), styles)
  }

  @Test
  fun `one day longer with an unchanged end date is a deviation`() {
    val styles = styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_9, currentDuration = 6)
    assertEquals(listOf("later"), styles)
  }

  @Test
  fun `one day earlier with an unchanged duration is a bar without a colour`() {
    val styles = styles(baselineStart = AUG_10, baselineDuration = 5, currentStart = AUG_9, currentDuration = 5)
    assertEquals(emptyList<String>(), styles)
  }

  @Test
  fun `a milestone which moved is marked as a milestone`() {
    val styles = styles(
      baselineStart = AUG_10, baselineDuration = 1, currentStart = AUG_13, currentDuration = 1, isMilestone = true
    )
    assertEquals(listOf("milestone"), styles)
  }

  @Test
  fun `a milestone which did not move gets no baseline bar`() {
    assertNull(
      styles(
        baselineStart = AUG_10, baselineDuration = 1, currentStart = AUG_10, currentDuration = 1, isMilestone = true
      )
    )
  }
}

private val AUG_3 = date(2026, Calendar.AUGUST, 3)
private val AUG_9 = date(2026, Calendar.AUGUST, 9)
private val AUG_10 = date(2026, Calendar.AUGUST, 10)
private val AUG_13 = date(2026, Calendar.AUGUST, 13)

private fun date(year: Int, month: Int, day: Int): Date = GregorianCalendar(year, month, day).time

/** Shifts by calendar days, which is what the test scenarios are written in. */
private fun shift(start: Date, days: Int): Date = GregorianCalendar().let {
  it.time = start
  it.add(Calendar.DAY_OF_YEAR, days)
  it.time
}

private fun styles(
  baselineStart: Date, baselineDuration: Int, currentStart: Date, currentDuration: Int, isMilestone: Boolean = false
) = GanttChartSceneBuilder.getBaselineStyles(
  isMilestone,
  baselineDuration.toFloat(),
  currentDuration.toFloat(),
  shift(baselineStart, baselineDuration),
  shift(currentStart, currentDuration)
)
