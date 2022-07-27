/*
Copyright 2022 BarD Software s.r.o, Alexander Popov

This file is part of GanttProject, an open-source project management tool.

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

import biz.ganttproject.core.time.CalendarFactory
import com.google.common.base.Function
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.*

/**
 * @author apopov77@gmail.com
 */

val usWeekNumbering = Function { date: Date ->
  val weekNumbering = WeekFields.of(Locale.US)
  val localDate = date.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
  localDate[weekNumbering.weekOfWeekBasedYear()]
}

val europeanWeekNumbering = Function { date: Date ->
  val weekNumbering = WeekFields.of(Locale.UK)
  val localDate = date.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
  localDate[weekNumbering.weekOfWeekBasedYear()]
}

val defaultWeekNumbering = Function { date: Date ->
  val calendar = CalendarFactory.newCalendar()
  calendar.time = date
  calendar[Calendar.WEEK_OF_YEAR]
}

class RelativeWeekNumbering(private val startProjectDate: Date) : Function<Date, Int> {
  override fun apply(date: Date): Int {
    val calendar = CalendarFactory.newCalendar()
    calendar.time = date
    var weekNum = calendar[Calendar.WEEK_OF_YEAR]
    calendar.time = startProjectDate
    val startWeekNum = calendar[Calendar.WEEK_OF_YEAR]
    weekNum -= startWeekNum
    if (weekNum >= 0) {
      weekNum++
    }
    return weekNum
  }
}
