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
package biz.ganttproject.core.calendar.walker

import biz.ganttproject.core.calendar.CalendarEvent
import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.TimeTestHelper.initLocale
import biz.ganttproject.core.time.TimeTestHelper.newFriday
import biz.ganttproject.core.time.TimeTestHelper.newMonday
import biz.ganttproject.core.time.TimeTestHelper.newSaturday
import biz.ganttproject.core.time.TimeTestHelper.newThursday
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.util.*

class DayTypeScannerTest {
  @BeforeEach
  fun setUp() {
    initLocale()
  }
  @Test
  fun `find non-working day type`() {
    val calendar = WeekendCalendarImpl()
    calendar.setWeekDayType(Calendar.SATURDAY, GPCalendar.DayType.WEEKEND)
    calendar.setWeekDayType(Calendar.SUNDAY, GPCalendar.DayType.WEEKEND)

    val scanner = DayTypeScan(calendar, newThursday().time, GPCalendar.DayType.NON_WORKING, GPTimeUnitStack.DAY)
    assertEquals(newSaturday().time,  scanner.scan())
  }

  @Test
  fun `find working day type`() {
    val calendar = WeekendCalendarImpl()
    calendar.setWeekDayType(Calendar.SATURDAY, GPCalendar.DayType.WEEKEND)
    calendar.setWeekDayType(Calendar.SUNDAY, GPCalendar.DayType.WEEKEND)

    val scanner = DayTypeScan(calendar, newFriday().time, GPCalendar.DayType.WORKING, GPTimeUnitStack.DAY)
    assertEquals(newMonday().time,  scanner.scan())
  }

  @Test
  fun `find non-working day type with no weekends`() {
    val calendar = WeekendCalendarImpl()
    calendar.setWeekDayType(Calendar.SATURDAY, GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(Calendar.SUNDAY, GPCalendar.DayType.WORKING)

    val holidays2005 = createHolidays(2005)
    calendar.publicHolidays = holidays2005

    DayTypeScan(calendar, newThursday().time, GPCalendar.DayType.NON_WORKING, GPTimeUnitStack.DAY).let {
      assertEquals(holidays2005[0].myDate, it.scan())
    }


    val holidays2020 = createHolidays(2020)
    calendar.publicHolidays = holidays2020
    DayTypeScan(calendar, newThursday().time, GPCalendar.DayType.NON_WORKING, GPTimeUnitStack.DAY).let {
      assertEquals(holidays2020[0].myDate, it.scan())
    }
  }
}

fun createHolidays(year: Int) = listOf(
  CalendarEvent.newEvent(
    CalendarFactory.createGanttCalendar(year, 0, 1).time,
    false,
    CalendarEvent.Type.HOLIDAY,
    "Jan 1",
    null
  ),
  CalendarEvent.newEvent(
    CalendarFactory.createGanttCalendar(year, 1, 14).time,
    false,
    CalendarEvent.Type.NEUTRAL,
    "Feb 14",
    null
  ),
)