/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.importer

import biz.ganttproject.core.calendar.CalendarEvent
import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.ImportCalendarOption
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.time.CalendarFactory
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.resource.HumanResourceMerger
import org.easymock.EasyMock.createNiceMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Tests importing the calendar of a project file into the current project, see issues #2861 and #2862.
 */
class ImportCalendarTest {
  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
  }

  private fun holiday(title: String, day: Int) = CalendarEvent.newEvent(
    GregorianCalendar(2026, Calendar.DECEMBER, day).time, false, CalendarEvent.Type.HOLIDAY, title, null
  )

  // Mirrors GanttProjectBase: the task manager of the project and the project itself share one calendar.
  private fun newTargetProject(): Pair<GanttProjectImpl, WeekendCalendarImpl> {
    val calendar = WeekendCalendarImpl()
    calendar.publicHolidays = listOf(holiday("Target holiday", 24))
    calendar.setWeekDayType(Calendar.FRIDAY, GPCalendar.DayType.WEEKEND)
    val project = GanttProjectImpl(calendar = calendar)
    assertSame(calendar, project.activeCalendar)
    assertSame(calendar, project.taskManager.calendar)
    return project to calendar
  }

  private fun newBufferProject(target: GanttProjectImpl): BufferProject {
    val buffer = BufferProject(target, createNiceMock(UIFacade::class.java))
    // This is what reading the imported file does.
    buffer.activeCalendar.publicHolidays = listOf(holiday("Imported holiday", 31))
    return buffer
  }

  private fun importProject(target: GanttProjectImpl, buffer: BufferProject, value: ImportCalendarOption.Values) {
    target.importProject(buffer, HumanResourceMerger.MergeResourcesOption(), ImportCalendarOption(value), false)
  }

  private fun GPCalendar.holidayTitles() = publicHolidays.map { it.title }.toSet()

  @Test
  fun `creating a buffer project keeps the calendar of the target project`() {
    val (target, calendar) = newTargetProject()
    newBufferProject(target)
    assertEquals(setOf("Target holiday"), calendar.holidayTitles())
    assertEquals(GPCalendar.DayType.WEEKEND, calendar.getWeekDayType(Calendar.FRIDAY))
  }

  @Test
  fun `option NO keeps the calendar of the target project`() {
    val (target, calendar) = newTargetProject()
    importProject(target, newBufferProject(target), ImportCalendarOption.Values.NO)
    assertEquals(setOf("Target holiday"), calendar.holidayTitles())
    assertEquals(GPCalendar.DayType.WEEKEND, calendar.getWeekDayType(Calendar.FRIDAY))
  }

  @Test
  fun `option REPLACE imports the holidays`() {
    val (target, calendar) = newTargetProject()
    importProject(target, newBufferProject(target), ImportCalendarOption.Values.REPLACE)
    assertEquals(setOf("Imported holiday"), calendar.holidayTitles())
  }

  @Test
  fun `option MERGE adds the imported holidays`() {
    val (target, calendar) = newTargetProject()
    importProject(target, newBufferProject(target), ImportCalendarOption.Values.MERGE)
    assertEquals(setOf("Target holiday", "Imported holiday"), calendar.holidayTitles())
    assertEquals(GPCalendar.DayType.WEEKEND, calendar.getWeekDayType(Calendar.FRIDAY))
  }
}
