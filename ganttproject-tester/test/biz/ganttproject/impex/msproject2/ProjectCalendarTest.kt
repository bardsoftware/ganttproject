/*
Copyright 2018 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.impex.msproject2

import biz.ganttproject.core.calendar.CalendarEvent
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.time.CalendarFactory
import junit.framework.TestCase
import net.sf.mpxj.ProjectFile
import net.sourceforge.ganttproject.TestSetupHelper
import java.awt.Color
import java.text.DateFormat
import java.util.*

fun initLocale() {
  object : CalendarFactory() {
    init {
      CalendarFactory.setLocaleApi(object : CalendarFactory.LocaleApi {
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

/**
 * Tests project calendar export and import.
 */
class ProjectCalendarTest: TestCase() {
  override fun setUp() {
    super.setUp()
    initLocale()
  }

  fun testExportCalendarEvents() {
    val calendar = WeekendCalendarImpl()
    calendar.publicHolidays = listOf<CalendarEvent>(
        CalendarEvent.newEvent(TestSetupHelper.newMonday().time, false, CalendarEvent.Type.HOLIDAY, "", Color.RED),
        CalendarEvent.newEvent(TestSetupHelper.newSaturday().time, false, CalendarEvent.Type.WORKING_DAY, "", Color.BLACK)
    )
    val mpxjProject = ProjectFile()
    val mpxjCalendar = mpxjProject.defaultCalendar
    ProjectFileExporter.exportHolidays(calendar, mpxjCalendar)
    assertTrue(mpxjCalendar.isWorkingDate(TestSetupHelper.newSaturday().time))
    assertFalse(mpxjCalendar.isWorkingDate(TestSetupHelper.newMonday().time))
  }
}

