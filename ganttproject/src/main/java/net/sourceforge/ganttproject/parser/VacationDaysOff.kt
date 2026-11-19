/*
Copyright 2026 BarD Software s.r.o, GanttProject Cloud OU

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
package net.sourceforge.ganttproject.parser

import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.time.GanttCalendar
import org.slf4j.LoggerFactory
import java.time.LocalDate

private val LOGGER = LoggerFactory.getLogger("VacationDaysOff")

/**
 * Turns the start and the end of a `<vacation>` element into days off.
 *
 * The end of a days off interval is exclusive, so an interval which ends on its start, or before it, covers no day
 * and would silently drop out of the plan. Such a vacation is read as the single day of its start, and a warning is
 * logged and handed to [onNoDays]. This is shared by the desktop and the cloud readers, so that they cannot disagree about the same file.
 */
fun daysOffFromFile(start: GanttCalendar, end: GanttCalendar, resourceId: Int,
                    onNoDays: (start: LocalDate, end: LocalDate) -> Unit = { _, _ -> }): GanttDaysOff {
  val startDay = start.toLocalDate()
  if (!end.toLocalDate().isAfter(startDay)) {
    LOGGER.warn("Vacation of resource {} starts on {} and ends on {}, which covers no day. Reading it as the single day {}",
      resourceId, startDay, end.toLocalDate(), startDay)
    onNoDays(startDay, end.toLocalDate())
    // GanttCalendar.fromLocalDate and not an instant, which would shift the date with the bent default time zone.
    return GanttDaysOff(start, GanttCalendar.fromLocalDate(startDay.plusDays(1)))
  }
  return GanttDaysOff(start, end)
}
