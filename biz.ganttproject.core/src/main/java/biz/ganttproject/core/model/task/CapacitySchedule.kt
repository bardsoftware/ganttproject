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

import java.math.BigDecimal
import java.time.LocalDate

/** From [from] on, including that day, [hoursPerDay] hours are available per working day. */
data class CapacityChange(val from: LocalDate, val hoursPerDay: BigDecimal)

/**
 * Hours per working day as they change over time: [base] until the first change, then the value of the latest change
 * which has started. Has no dependencies on the project model.
 */
class CapacitySchedule(val base: BigDecimal, changes: List<CapacityChange> = emptyList()) {
  /** Sorted by date. If a date occurs twice, the later entry of the input wins. */
  val changes: List<CapacityChange> = changes.associateBy { it.from }.values.sortedBy { it.from }

  val isConstant: Boolean get() = changes.isEmpty()

  fun hoursOn(day: LocalDate): BigDecimal = changes.lastOrNull { !it.from.isAfter(day) }?.hoursPerDay ?: base

  companion object {
    /** The upper limit for hours per day, for input validation. */
    val MAX_HOURS_PER_DAY: BigDecimal = BigDecimal(24)

    /**
     * Upper limit of [daysNeeded]. Without it, a period without hours which never ends would loop forever. It is about
     * 40 years of working days.
     */
    const val MAX_DAYS = 10_000
  }
}

/**
 * Working days needed to do [effortHours] when starting on [start]: every working day takes what the schedule
 * offers on that day, so a task which runs over a change is counted with the old hours before and the new ones after
 * it. Days without hours count as days of the task. Negative hours count as none.
 *
 * @return at least 1, or null if the effort is not done within [CapacitySchedule.MAX_DAYS] working days.
 */
fun daysNeeded(effortHours: BigDecimal, start: LocalDate, schedule: CapacitySchedule,
               isWorkingDay: (LocalDate) -> Boolean): Int? {
  if (effortHours.signum() <= 0) {
    return 1
  }
  var remaining = effortHours
  var day = start
  var days = 0
  // Each iteration is a calendar day; the factor leaves room for calendars where most days are off.
  var guard = 0
  while (guard++ < CapacitySchedule.MAX_DAYS * 4) {
    if (isWorkingDay(day)) {
      days++
      remaining = remaining.subtract(schedule.hoursOn(day).max(BigDecimal.ZERO))
      if (remaining.signum() <= 0) {
        return days
      }
      if (days >= CapacitySchedule.MAX_DAYS) {
        return null
      }
    }
    day = day.plusDays(1)
  }
  return null
}

/**
 * Reads changes of working hours as typed by a user: `2027-04-01: 6; 2027-10-01: 8`. Entries are separated by `;` or a
 * line break, date and hours by `:` or `=`, and the hours may have a comma. Dates are ISO dates, because a date such
 * as `03.04.05` means three different days in three countries. Hours are from 0 to 24.
 *
 * @return null if any entry is not valid, so that a typing error never leaves the schedule half read.
 */
fun parseCapacityChanges(text: String): List<CapacityChange>? =
  text.split(';', '\n').map { it.trim() }.filter { it.isNotEmpty() }.map { entry ->
    val separator = entry.indexOfFirst { it == ':' || it == '=' }
    if (separator < 0) {
      return null
    }
    val day = try {
      LocalDate.parse(entry.substring(0, separator).trim())
    } catch (e: java.time.format.DateTimeParseException) {
      return null
    }
    val hours = entry.substring(separator + 1).trim().replace(',', '.').toBigDecimalOrNull() ?: return null
    if (hours.signum() < 0 || hours > CapacitySchedule.MAX_HOURS_PER_DAY) {
      return null
    }
    CapacityChange(day, hours)
  }

/** The inverse of [parseCapacityChanges]. */
fun formatCapacityChanges(changes: List<CapacityChange>): String =
  changes.joinToString("; ") { "${it.from}: ${it.hoursPerDay.stripTrailingZeros().toPlainString()}" }
