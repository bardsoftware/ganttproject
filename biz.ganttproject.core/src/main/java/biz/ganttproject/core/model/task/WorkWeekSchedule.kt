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

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * From [from] on, including that day, the resource works on [days]. Without [from] the days apply from the beginning
 * of the plan. Empty [days] mean that the resource works on no day.
 */
data class WorkWeekChange(val from: LocalDate?, val days: Set<DayOfWeek>)

/**
 * The working week of a resource as it changes over time: the days of the latest change which has started. It tells
 * whether a day is a working day of the resource, while [CapacitySchedule] tells how many hours it works on such a
 * day. Has no dependencies on the project model.
 *
 * A resource without any change has no working week of its own, and the project calendar applies to it.
 */
class WorkWeekSchedule(changes: List<WorkWeekChange> = emptyList()) {
  /** Sorted by date, the undated change first. If a date occurs twice, the later entry of the input wins. */
  val changes: List<WorkWeekChange> = changes.associateBy { it.from }.values.sortedBy { it.from ?: LocalDate.MIN }

  val isEmpty: Boolean get() = changes.isEmpty()

  /** The change in force on [day], or null if none has started. */
  fun sectionOn(day: LocalDate): WorkWeekChange? = changes.lastOrNull { it.from == null || !it.from.isAfter(day) }

  /**
   * Whether the resource works on [day], or null if no change has started on that day. Null is not false: the
   * project calendar decides on such a day.
   */
  fun worksOn(day: LocalDate): Boolean? = sectionOn(day)?.days?.contains(day.dayOfWeek)
}

/**
 * Whether a task works on [day], given the working weeks of the resources [involved] in it. A task only works on
 * working days of the project: a day which the project calendar leaves free, be it a weekend day or a holiday, is
 * never a working day of the task, even if every involved resource names its weekday. On a working day of the project
 * every involved resource has to be available, where a resource without a statement for the day is available. Hence a
 * resource can take a working day of the project away from the task, but never add one. A task without involved
 * resources follows the project calendar.
 *
 * @param projectWorking whether the project calendar says that [day] is a working day.
 */
fun taskWorksOn(day: LocalDate, projectWorking: Boolean, involved: List<WorkWeekSchedule>): Boolean =
  projectWorking && involved.all { it.worksOn(day) ?: true }

/**
 * Reads a working week as typed by a user: `1,2,5,6; 2027-03-01: 1,2,3`. Days are ISO numbers from 1 for Monday to
 * 7 for Sunday, so that the text means the same in every language. Entries are separated by `;` or a line break,
 * an optional ISO date and the days by `:` or `=`. An entry without a date applies from the beginning of the plan, and
 * an entry without days, such as `2027-05-01:`, means no working day from that date on.
 *
 * @return null if any entry is not valid, so that a typing error never leaves the week half read.
 */
fun parseWorkWeek(text: String): List<WorkWeekChange>? =
  text.split(';', '\n').map { it.trim() }.filter { it.isNotEmpty() }.map { entry ->
    val separator = entry.indexOfFirst { it == ':' || it == '=' }
    val dateText = if (separator < 0) "" else entry.substring(0, separator).trim()
    val daysText = if (separator < 0) entry else entry.substring(separator + 1)
    val from = if (dateText.isEmpty()) null else try {
      LocalDate.parse(dateText)
    } catch (e: DateTimeParseException) {
      return null
    }
    WorkWeekChange(from, parseWeekDays(daysText) ?: return null)
  }

/** Reads ISO weekday numbers separated by commas, such as `1,2,5`. Returns null if any of them is not 1 to 7. */
fun parseWeekDays(text: String): Set<DayOfWeek>? =
  text.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { token ->
    val number = token.toIntOrNull()
    if (number == null || number < 1 || number > 7) {
      return null
    }
    DayOfWeek.of(number)
  }.toSet()

/** ISO weekday numbers separated by commas, the inverse of [parseWeekDays]. */
fun formatWeekDays(days: Set<DayOfWeek>): String = days.sortedBy { it.value }.joinToString(",") { it.value.toString() }

/**
 * The inverse of [parseWorkWeek]. A change without days keeps its separator, and an undated one is a bare `:`, so
 * that "works on no day" is read back as such and does not vanish.
 */
fun formatWorkWeek(changes: List<WorkWeekChange>): String = changes.joinToString("; ") { change ->
  val days = formatWeekDays(change.days)
  when {
    change.from == null && days.isEmpty() -> ":"
    change.from == null -> days
    else -> "${change.from}: $days".trimEnd()
  }
}
