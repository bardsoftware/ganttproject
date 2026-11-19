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
package biz.ganttproject.core.model.task

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * A stretch of days which a resource works at home. [start] is the first day and [endExclusive] the day after the last
 * one, which is how the interval of a day off is stored.
 */
data class HomeOfficePeriod(val start: LocalDate, val endExclusive: LocalDate) {
  fun covers(day: LocalDate): Boolean = !day.isBefore(start) && day.isBefore(endExclusive)
}

/**
 * The days on which a resource works at home: the days of the week, as they change over time, and periods. A day at
 * home is a working day with all its hours. It only keeps the resource from the tasks which need somebody on site, so
 * it must never be mixed with a day off or with a non-working day of the working week.
 *
 * Unlike a working week, nothing entered is not "the project calendar applies" but "never at home". Has no dependencies
 * on the project model.
 */
class HomeOffice(week: List<WorkWeekChange> = emptyList(), periods: List<HomeOfficePeriod> = emptyList()) {
  /** The days of the week at home. A change without days ends the arrangement from its date on. */
  val week: WorkWeekSchedule = WorkWeekSchedule(week)

  /** Sorted by start, without duplicates. */
  val periods: List<HomeOfficePeriod> = periods.distinct().sortedWith(compareBy({ it.start }, { it.endExclusive }))

  val isEmpty: Boolean get() = week.isEmpty && periods.isEmpty()

  /** Whether the resource works at home on [day]: the weekly pattern names it or a period covers it. */
  fun worksFromHome(day: LocalDate): Boolean = week.worksOn(day) == true || periods.any { it.covers(day) }
}

/**
 * Reads the periods as typed by a user: `2027-06-01..2027-06-03; 2027-07-05`. Both dates of a period are days at home,
 * and a single date is one day. Periods are separated by `;` or a line break.
 *
 * @return null if any period is not valid, that is, has an unreadable date or ends before it starts.
 */
fun parseHomeOfficePeriods(text: String): List<HomeOfficePeriod>? =
  text.split(';', '\n').map { it.trim() }.filter { it.isNotEmpty() }.map { entry ->
    try {
      val separator = entry.indexOf("..")
      val first = LocalDate.parse((if (separator < 0) entry else entry.substring(0, separator)).trim())
      val last = if (separator < 0) first else LocalDate.parse(entry.substring(separator + 2).trim())
      if (last.isBefore(first)) {
        return null
      }
      HomeOfficePeriod(first, last.plusDays(1))
    } catch (e: DateTimeParseException) {
      return null
    }
  }

/** The inverse of [parseHomeOfficePeriods]. */
fun formatHomeOfficePeriods(periods: List<HomeOfficePeriod>): String = periods.joinToString("; ") { period ->
  val last = period.endExclusive.minusDays(1)
  if (last == period.start) "${period.start}" else "${period.start}..$last"
}
