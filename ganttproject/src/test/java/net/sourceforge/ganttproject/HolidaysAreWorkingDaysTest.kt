/*
Copyright 2026 BarD Software s.r.o., GanttProject Team

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
package net.sourceforge.ganttproject

import biz.ganttproject.core.calendar.CalendarEvent
import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.io.XmlProjectImporter
import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.calendar.ImportCalendarOption
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import biz.ganttproject.task.recurrence.toModelDate
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.WorkWeekWorkingDays
import net.sourceforge.ganttproject.task.algorithm.effortDrivenProjectDays
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * The project calendar setting which makes public holidays working days: the same days for the scheduler and for
 * the effort calculation, weekends and days off of people stay free, and the setting survives copying and the file.
 * The week starts on Monday 2026-11-09; the holiday is Wednesday 2026-11-11.
 */
class HolidaysAreWorkingDaysTest {
  private lateinit var project: GanttProjectImpl
  private val monday = LocalDate.of(2026, 11, 9)
  private val wednesday = monday.plusDays(2)
  private val saturday = monday.plusDays(5)

  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
    project = GanttProjectImpl()
  }

  private fun holidays(vararg days: LocalDate) {
    project.taskManager.calendar.publicHolidays = days.map {
      CalendarEvent.newEvent(it.toModelDate(), false, CalendarEvent.Type.HOLIDAY, null, null)
    }
  }

  private fun fiveDayTask(): Task =
    project.taskManager.newTaskBuilder().withName("t").withStartDate(monday.toModelDate())
      .withDuration(project.taskManager.createLength(5)).build()

  private fun Task.endDay(): LocalDate = LocalDate.of(end.year, end.month + 1, end.date)

  private fun save(p: IGanttProject): String = ByteArrayOutputStream().also { GanttXMLSaver(p).save(it) }.toString(Charsets.UTF_8)

  private val projectXml = """<project name="p"><tasks><task id="1" uid="u1" name="T" start="2026-11-09" duration="1"/></tasks></project>"""

  @Test
  fun `by default a holiday is not worked on and the task is a day longer`() {
    holidays(wednesday)
    assertFalse(project.activeCalendar.holidaysAreWorkingDays)
    assertEquals(0, project.taskManager.calendar.getDayMask(wednesday.toModelDate()) and GPCalendar.DayMask.WORKING)
    // Monday, Tuesday, Thursday, Friday and the next Monday
    assertEquals(monday.plusDays(8), fiveDayTask().endDay())
  }

  @Test
  fun `with the setting on a task over a holiday is a day shorter`() {
    holidays(wednesday)
    project.activeCalendar.holidaysAreWorkingDays = true
    assertTrue(project.taskManager.calendar.getDayMask(wednesday.toModelDate()) and GPCalendar.DayMask.HOLIDAY != 0)
    // Monday to Friday; the end is the day after the last working day
    assertEquals(monday.plusDays(5), fiveDayTask().endDay())
  }

  @Test
  fun `the bar and the effort calculation cover the same working days`() {
    holidays(wednesday)
    val resource = project.humanResourceManager.create("r", 1).also { it.hoursPerDay = BigDecimal(8) }
    val task = fiveDayTask()
    task.assignmentCollection.addAssignment(resource).also { it.load = 100f }
    val calendar = project.taskManager.calendar
    for (on in listOf(false, true)) {
      project.activeCalendar.holidaysAreWorkingDays = on
      val days = WorkWeekWorkingDays(calendar)
      val effortDays = effortDrivenProjectDays(BigDecimal(40), task.assignments, monday, calendar, days)!!
      assertEquals(5L, effortDays, "setting on = $on")
      val bar = project.taskManager.newTaskBuilder().withName("bar").withStartDate(monday.toModelDate())
        .withDuration(project.taskManager.createLength(effortDays)).build()
      assertEquals(effortDays, days.taskDays(monday, bar.endDay(), days.projectOnly).toLong(), "bar, setting on = $on")
      assertEquals(effortDays, days.taskDays(monday, days.endAfter(monday, 5, days.projectOnly), days.projectOnly).toLong(),
        "calculation, setting on = $on")
    }
  }

  @Test
  fun `a weekend stays free, also when it is a holiday`() {
    holidays(saturday)
    project.activeCalendar.holidaysAreWorkingDays = true
    assertFalse(WorkWeekWorkingDays(project.taskManager.calendar).projectOnly(saturday))
    assertFalse(WorkWeekWorkingDays(project.taskManager.calendar).projectOnly(saturday.plusDays(1)))
  }

  @Test
  fun `a day off of a person on a holiday still takes the day`() {
    holidays(wednesday)
    project.activeCalendar.holidaysAreWorkingDays = true
    val resource = project.humanResourceManager.create("r", 1).also { it.hoursPerDay = BigDecimal(8) }
    resource.addDaysOff(GanttDaysOff(
      CalendarFactory.createGanttCalendar(2026, 10, 11), CalendarFactory.createGanttCalendar(2026, 10, 12)))
    val task = fiveDayTask()
    task.assignmentCollection.addAssignment(resource).also { it.load = 100f }
    // the holiday is a working day, but nobody works on it: six days for five days of effort
    assertEquals(6L, effortDrivenProjectDays(BigDecimal(40), task.assignments, monday, project.taskManager.calendar))
  }

  @Test
  fun `copy and import of a calendar take the setting along`() {
    val calendar = WeekendCalendarImpl()
    calendar.holidaysAreWorkingDays = true
    assertTrue(calendar.copy().holidaysAreWorkingDays)

    val target = WeekendCalendarImpl()
    target.importCalendar(calendar, ImportCalendarOption(ImportCalendarOption.Values.REPLACE))
    assertTrue(target.holidaysAreWorkingDays)
    val merged = WeekendCalendarImpl()
    merged.importCalendar(calendar, ImportCalendarOption(ImportCalendarOption.Values.MERGE))
    assertTrue(merged.holidaysAreWorkingDays)
  }

  @Test
  fun `the setting survives a save and load`() {
    val p = XmlProjectImporter().import(projectXml)
    p.activeCalendar.holidaysAreWorkingDays = true
    val saved = save(p)
    assertTrue(saved.contains("""<holidays-are-working-days value="true"/>"""), saved)
    val loaded = XmlProjectImporter().import(saved)
    assertTrue(loaded.activeCalendar.holidaysAreWorkingDays, saved)
    assertEquals(saved, save(loaded), "second save differs from the first")
  }

  @Test
  fun `a file without the setting loads with it off and is saved without it`() {
    val p = XmlProjectImporter().import(projectXml)
    assertFalse(p.activeCalendar.holidaysAreWorkingDays)
    val saved = save(p)
    assertFalse(saved.contains("holidays-are-working-days"), saved)
    assertEquals(saved, save(XmlProjectImporter().import(saved)))
  }
}
