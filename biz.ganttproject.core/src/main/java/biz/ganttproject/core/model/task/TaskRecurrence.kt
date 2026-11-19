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

import java.time.LocalDate

enum class RecurrenceUnit(val xmlValue: String) {
  DAY("day"), WEEK("week"), MONTH("month"), YEAR("year");

  companion object {
    fun fromXml(value: String?): RecurrenceUnit? = values().firstOrNull { it.xmlValue == value }
  }
}

/**
 * A rule saying that a task repeats: every [interval] [unit]s, until the date [until] or for [count] tasks in total
 * (the first one included). Exactly one of [until] and [count] is set, so a rule always ends.
 */
data class RecurrenceRule(
  val unit: RecurrenceUnit,
  val interval: Int = 1,
  val until: LocalDate? = null,
  val count: Int? = null
) {
  init {
    require(interval >= 1) { "Recurrence interval must be positive: $interval" }
    require((until == null) != (count == null)) { "Exactly one of 'until' and 'count' must be set" }
    require(count == null || count >= 1) { "Recurrence count must be positive: $count" }
  }

  /** Why a rule cannot produce its tasks starting from [first], or null if it can. */
  fun problemFor(first: LocalDate, isWorkingDay: (LocalDate) -> Boolean): RecurrenceProblem? = when {
    until != null && until.isBefore(first) -> RecurrenceProblem.END_BEFORE_START
    generate(first, isWorkingDay, MAX_OCCURRENCES + 1).size > MAX_OCCURRENCES -> RecurrenceProblem.TOO_MANY
    else -> null
  }

  /**
   * Dates of the repeated tasks, the first one included.
   *
   * A date on a non-working day moves forward to the next working day: moving back could cross the previous
   * date of the same series or the start of the project, moving forward cannot. The end of the series is checked
   * against the date before the move, so it does not depend on whether the last date happens to be a holiday.
   * Dates which collapse onto the same working day are kept once. At most [MAX_OCCURRENCES] dates are returned.
   */
  fun occurrences(first: LocalDate, isWorkingDay: (LocalDate) -> Boolean): List<LocalDate> =
    generate(first, isWorkingDay, MAX_OCCURRENCES)

  private fun generate(first: LocalDate, isWorkingDay: (LocalDate) -> Boolean, limit: Int): List<LocalDate> {
    val result = linkedSetOf<LocalDate>()
    var index = 0L
    while (result.size < limit) {
      val raw = when (unit) {
        RecurrenceUnit.DAY -> first.plusDays(index * interval)
        RecurrenceUnit.WEEK -> first.plusWeeks(index * interval)
        RecurrenceUnit.MONTH -> first.plusMonths(index * interval)
        RecurrenceUnit.YEAR -> first.plusYears(index * interval)
      }
      if (until != null && raw.isAfter(until)) {
        break
      }
      result.add(recurrenceNextWorkingDay(raw, isWorkingDay))
      index++
      if (count != null && result.size >= count) {
        break
      }
    }
    return result.toList()
  }

  companion object {
    /** A series longer than this is most likely a typo in its end. */
    const val MAX_OCCURRENCES = 500

    /**
     * Builds a rule from the values read from a file. Returns null if they do not make a rule, so that a damaged
     * attribute does not make the whole file unreadable.
     */
    fun fromXml(unit: String?, interval: String?, until: String?, count: String?): RecurrenceRule? {
      val parsedUnit = RecurrenceUnit.fromXml(unit) ?: return null
      val parsedInterval = if (interval == null) 1 else interval.toIntOrNull() ?: return null
      val parsedCount = if (count == null) null else count.toIntOrNull() ?: return null
      val parsedUntil = until?.let { runCatching { LocalDate.parse(it) }.getOrNull() ?: return null }
      return runCatching { RecurrenceRule(parsedUnit, parsedInterval, parsedUntil, parsedCount) }.getOrNull()
    }
  }
}

enum class RecurrenceProblem { END_BEFORE_START, TOO_MANY }

/**
 * Tells that a task was created from a recurrence rule of the task [sourceTaskId].
 * A task with a [date] is one repeated task, a task without it is the group which holds the series.
 */
data class RecurrenceOrigin(val sourceTaskId: Int, val date: LocalDate? = null) {
  val isGroup: Boolean get() = date == null

  companion object {
    fun fromXml(sourceTaskId: String?, date: String?): RecurrenceOrigin? {
      val id = sourceTaskId?.toIntOrNull() ?: return null
      if (date == null) {
        return RecurrenceOrigin(id)
      }
      return runCatching { RecurrenceOrigin(id, LocalDate.parse(date)) }.getOrNull()
    }
  }
}

private fun recurrenceNextWorkingDay(from: LocalDate, isWorkingDay: (LocalDate) -> Boolean): LocalDate {
  var day = from
  var guard = 0
  while (!isWorkingDay(day) && guard++ < 400) {
    day = day.plusDays(1)
  }
  return day
}
