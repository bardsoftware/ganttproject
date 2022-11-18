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
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.importer.ImporterFromGanttFile
import org.junit.jupiter.api.Test
import java.awt.Color
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

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
    val mpxjCalendar = mpxjProject.calendars.addDefaultBaseCalendar()
    ProjectFileExporter.exportHolidays(calendar, mpxjCalendar)
    assertTrue(mpxjCalendar.isWorkingDate(TestSetupHelper.newSaturday().time))
    assertFalse(mpxjCalendar.isWorkingDate(TestSetupHelper.newMonday().time))
  }

  fun testImportCalendarEvents() {
    val project = GanttProjectImpl()
    val columns = ImporterFromGanttFile.VisibleFieldsImpl()
    val fileUrl = ProjectCalendarTest::class.java.getResource("/issue1520.xml")
    assertNotNull(fileUrl)
    val importer = ProjectFileImporter(project, columns, File(fileUrl.toURI()))
    importer.setPatchMspdi(false)
    importer.run()

    val parser = SimpleDateFormat("yyyy-MM-dd")
    val calendar = project.activeCalendar
    val publicHolidays = ArrayList(calendar.publicHolidays)
    assertEquals(2, publicHolidays.size)
    assertTrue(publicHolidays[0].type == CalendarEvent.Type.WORKING_DAY)
    assertEquals(parser.parse("2018-04-28"), publicHolidays[0].myDate)

    assertTrue(publicHolidays[1].type == CalendarEvent.Type.HOLIDAY)
    assertEquals(parser.parse("2018-04-30"), publicHolidays[1].myDate)
  }

  @Test
  fun `first week of year`() {
    // Year 2022
    assertEquals(1, Calendar.Builder().setLocale(Locale.US)
      .setWeekDefinition(Calendar.SUNDAY, 1)
      .setDate(2022, 0, 1)
      .build().get(Calendar.WEEK_OF_YEAR))
    assertEquals(2, Calendar.Builder().setLocale(Locale.US)
      .setWeekDefinition(Calendar.SUNDAY, 1)
      .setDate(2022, 0, 3)
      .build().get(Calendar.WEEK_OF_YEAR))
    assertEquals(53, Calendar.Builder().setLocale(Locale.UK)
      .setWeekDefinition(Calendar.MONDAY, 4)
      .setDate(2021, 0, 1)
      .build().get(Calendar.WEEK_OF_YEAR))
    assertEquals(1, Calendar.Builder().setLocale(Locale.UK)
      .setWeekDefinition(Calendar.MONDAY, 4)
      .setDate(2022, 0, 3)
      .build().get(Calendar.WEEK_OF_YEAR))

    // Year 2015 (January 1st was Thursday)
    assertEquals(1, Calendar.Builder().setLocale(Locale.US)
      .setWeekDefinition(Calendar.SUNDAY, 1)
      .setDate(2015, 0, 1)
      .build().get(Calendar.WEEK_OF_YEAR))
    assertEquals(1, Calendar.Builder().setLocale(Locale.UK)
      .setWeekDefinition(Calendar.MONDAY, 4)
      .setDate(2015, 0, 1)
      .build().get(Calendar.WEEK_OF_YEAR))

    // Year 2023 (January 1st is Sunday)
    assertEquals(1, Calendar.Builder().setLocale(Locale.US)
      .setWeekDefinition(Calendar.SUNDAY, 1)
      .setDate(2023, 0, 1)
      .build().get(Calendar.WEEK_OF_YEAR))
    assertEquals(52, Calendar.Builder().setLocale(Locale.UK)
      .setWeekDefinition(Calendar.MONDAY, 4)
      .setDate(2023, 0, 1)
      .build().get(Calendar.WEEK_OF_YEAR))
    assertEquals(1, Calendar.Builder().setLocale(Locale.UK)
      .setWeekDefinition(Calendar.MONDAY, 4)
      .setDate(2023, 0, 2)
      .build().get(Calendar.WEEK_OF_YEAR))
  }

}

