/*
Copyright 2022 BarD Software s.r.o, GanttProject Cloud OU, Dmitry Barashev

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
package net.sourceforge.ganttproject.parser

import biz.ganttproject.core.calendar.CalendarEvent
import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.GPCalendarCalc
import biz.ganttproject.core.io.XmlProject
import biz.ganttproject.core.time.CalendarFactory
import net.sourceforge.ganttproject.util.ColorConvertion

class CalendarSerializer(private val calendar: GPCalendarCalc) {
  fun loadCalendar(xmlProject: XmlProject) {
    val xmlCalendars = xmlProject.calendars
    calendar.baseCalendarID = xmlCalendars.baseId

    val xmlDayTypes = xmlCalendars.dayTypes
    for (i in 1..7) {
      calendar.setWeekDayType(i, GPCalendar.DayType.WORKING)
    }
    calendar.setWeekDayType(1, if (xmlDayTypes.defaultWeek.sun == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(2, if (xmlDayTypes.defaultWeek.mon == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(3, if (xmlDayTypes.defaultWeek.tue == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(4, if (xmlDayTypes.defaultWeek.wed == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(5, if (xmlDayTypes.defaultWeek.thu == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(6, if (xmlDayTypes.defaultWeek.fri == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)
    calendar.setWeekDayType(7, if (xmlDayTypes.defaultWeek.sat == 1) GPCalendar.DayType.WEEKEND else GPCalendar.DayType.WORKING)

    calendar.onlyShowWeekends = xmlDayTypes.onlyShowWeekends.value
    xmlCalendars.events?.map { xmlEvent ->
      val month = xmlEvent.month
      val day = xmlEvent.date
      val type = if (xmlEvent.type.isEmpty()) CalendarEvent.Type.HOLIDAY else CalendarEvent.Type.valueOf(xmlEvent.type)
      val color = xmlEvent.color?.let { ColorConvertion.determineColor(it) }
      val description = xmlEvent.value?.replace("^\\p{Space}+".toRegex(), "")?.replace("\\p{Space}+$".toRegex(), "") ?: ""
      val event = if (xmlEvent.year.isBlank()) {
        val date = CalendarFactory.createGanttCalendar(1, month - 1, day).time
        CalendarEvent.newEvent(date, true, type, description, color)
      } else {
        val date = CalendarFactory.createGanttCalendar(xmlEvent.year.toInt(), month - 1, day).time
        CalendarEvent.newEvent(date, false, type, description, color)
      }
      event
    }?.toList()?.let { calendar.publicHolidays = it }

  }
}