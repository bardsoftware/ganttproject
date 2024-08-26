/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.core.time

import java.text.DateFormat
import java.util.*

/**
 * Utility functions required for testing time- and calendar- related code.
 */
object TimeTestHelper {
  /**
   * Initializes a locale factory for the datetime formatting purposes.
   */
  fun initLocale() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale {
            return Locale.US
          }

          override fun getShortDateFormat(): DateFormat {
            return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
          }
        })
      }
    }
  }

  // These functions create new dates for the test purposes, in the interval [thursday... wednesday] so that the weekend
  // was covered.
  fun newThursday() = CalendarFactory.createGanttCalendar(2004, 9, 14)
  fun newFriday() = CalendarFactory.createGanttCalendar(2004, 9, 15)
  fun newSaturday() = CalendarFactory.createGanttCalendar(2004, 9, 16)
  fun newSunday() = CalendarFactory.createGanttCalendar(2004, 9, 17)
  fun newMonday() = CalendarFactory.createGanttCalendar(2004, 9, 18)
  fun newTuesday() = CalendarFactory.createGanttCalendar(2004, 9, 19)
  fun newWednesday() = CalendarFactory.createGanttCalendar(2004, 9, 20)


}