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
package biz.ganttproject.task.recurrence

import java.time.LocalDate
import java.util.Date
import java.util.GregorianCalendar

/*
 * Conversion between LocalDate and the Date values of the project model.
 *
 * ZoneId.systemDefault() must not be used here: GanttLanguage.setLocale replaces the default time zone with a
 * "UTC" zone whose raw offset is the local one. TimeZone.getDefault() sees the offset, java.time resolves the id
 * and sees zero, so a date converted through java.time can be a day off. GregorianCalendar takes the same way as
 * CalendarFactory does and is right in both cases.
 */

/** Midnight of this day, in the time of the project model. */
fun LocalDate.toModelDate(): Date =
  GregorianCalendar(this.year, this.monthValue - 1, this.dayOfMonth).time

/** The calendar day which this moment falls on in the project model. */
fun Date.toModelLocalDate(): LocalDate {
  val calendar = GregorianCalendar()
  calendar.time = this
  return LocalDate.of(
    calendar.get(GregorianCalendar.YEAR),
    calendar.get(GregorianCalendar.MONTH) + 1,
    calendar.get(GregorianCalendar.DAY_OF_MONTH))
}
