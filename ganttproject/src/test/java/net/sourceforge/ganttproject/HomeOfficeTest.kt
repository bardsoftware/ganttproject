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

import biz.ganttproject.core.io.XmlProjectImporter
import biz.ganttproject.core.model.task.HomeOffice
import biz.ganttproject.core.model.task.HomeOfficePeriod
import biz.ganttproject.core.model.task.LevelConflict
import biz.ganttproject.core.model.task.LevelResult
import biz.ganttproject.core.model.task.WorkWeekChange
import biz.ganttproject.core.model.task.formatHomeOfficePeriods
import biz.ganttproject.core.model.task.levelTasks
import biz.ganttproject.core.model.task.parseHomeOfficePeriods
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import biz.ganttproject.task.recurrence.toModelDate
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.WorkWeekWorkingDays
import net.sourceforge.ganttproject.task.algorithm.collectLevelTasks
import net.sourceforge.ganttproject.task.algorithm.durationAtStart
import net.sourceforge.ganttproject.task.algorithm.presenceTest
import net.sourceforge.ganttproject.task.algorithm.resourcePresence
import net.sourceforge.ganttproject.task.algorithm.startAllowed
import net.sourceforge.ganttproject.task.algorithm.workingDaysOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.ThrowingSupplier
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DateFormat
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

/**
 * Days at home of a resource: the union of the weekly pattern and the periods, the file format, and the one effect
 * they have, on a task which needs its blocking resources on site. The week starts on Monday 2026-11-02; resources
 * work 8 hours a day.
 */
class HomeOfficeTest {
  private lateinit var project: GanttProjectImpl
  private val monday = LocalDate.of(2026, 11, 2)

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

  private fun resource(name: String = "anna"): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal(8)
    }

  private fun task(blocking: HumanResource, onSite: Boolean, effort: Int? = null, days: Int = 1): Task =
    project.taskManager.newTaskBuilder().withName("t").withStartDate(monday.toModelDate())
      .withDuration(project.taskManager.createLength(days.toLong())).build().also { task ->
        task.assignmentCollection.addAssignment(blocking).also { it.load = 100f; it.isBlocking = true }
        task.createMutator().also {
          it.setOnSiteOnly(onSite)
          if (effort != null) {
            it.setEffort(BigDecimal(effort))
            it.setEffortDriven(true)
          }
          it.commit()
        }
      }

  private fun level(): LevelResult {
    val workingDays = WorkWeekWorkingDays(project.taskManager.calendar)
    val tasks = collectLevelTasks(project.taskManager, monday, workingDays = workingDays)
    val worksOn = workingDaysOf(project.taskManager, workingDays)
    return levelTasks(tasks, monday, workingDays.projectOnly, durationAtStart(project.taskManager, worksOn),
      canStartOn = startAllowed(project.taskManager), isPresent = resourcePresence(project.taskManager),
      workingDaysOf = worksOn, isAtWorkplace = presenceTest(project.taskManager))
  }

  private val Task.id get() = taskID.toString()

  private fun Task.startDay(): LocalDate = LocalDate.of(start.year, start.month + 1, start.date)

  @Test
  fun `a day is at home if the pattern names it or a period covers it, and once if both do`() {
    val week = listOf(WorkWeekChange(null, setOf(FRIDAY)))
    val periods = listOf(HomeOfficePeriod(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 6)))
    val homeOffice = HomeOffice(week, periods)
    val friday = LocalDate.of(2026, 11, 6)
    assertTrue(homeOffice.worksFromHome(friday))
    assertTrue(homeOffice.worksFromHome(LocalDate.of(2026, 11, 4)))
    assertTrue(homeOffice.worksFromHome(LocalDate.of(2026, 11, 13)))
    assertFalse(homeOffice.worksFromHome(monday))
    // The end of a period is the day after its last day.
    assertFalse(HomeOffice(emptyList(), periods).worksFromHome(friday))
  }

  @Test
  fun `nothing entered is no day at home, and a change without days ends the pattern`() {
    assertTrue(HomeOffice().isEmpty)
    assertFalse(HomeOffice().worksFromHome(monday))
    val week = listOf(WorkWeekChange(null, setOf(MONDAY)), WorkWeekChange(LocalDate.of(2026, 11, 9), emptySet()))
    val homeOffice = HomeOffice(week)
    assertTrue(homeOffice.worksFromHome(monday))
    assertFalse(homeOffice.worksFromHome(LocalDate.of(2026, 11, 9)))
  }

  @Test
  fun `periods are read with their last day and written back as typed`() {
    val periods = parseHomeOfficePeriods("2026-11-02..2026-11-04; 2026-11-09")!!
    assertEquals(listOf(
      HomeOfficePeriod(monday, LocalDate.of(2026, 11, 5)),
      HomeOfficePeriod(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 10))), periods)
    assertEquals("2026-11-02..2026-11-04; 2026-11-09", formatHomeOfficePeriods(periods))
    assertEquals(emptyList<HomeOfficePeriod>(), parseHomeOfficePeriods("  "))
    assertNull(parseHomeOfficePeriods("2026-11-04..2026-11-02"))
    assertNull(parseHomeOfficePeriods("2026-11-02..soon"))
  }

  @Test
  fun `a day at home is no day off`() {
    val anna = resource()
    anna.homeOfficeWeek = listOf(WorkWeekChange(null, setOf(FRIDAY)))
    anna.homeOfficePeriods = listOf(HomeOfficePeriod(monday, monday.plusDays(1)))
    assertTrue(anna.daysOff.isEmpty())
    assertTrue(anna.workWeek.isEmpty())
  }

  @Test
  fun `a task which needs somebody on site takes the day at home as a day without progress`() {
    val anna = resource()
    anna.homeOfficeWeek = listOf(WorkWeekChange(null, setOf(FRIDAY)))
    val onSite = task(anna, onSite = true, effort = 40)
    val anywhere = task(anna, onSite = false, effort = 40)
    // Monday to Thursday bring 32 hours, Friday none, and the last 8 hours take the next Monday.
    assertEquals(6, onSite.duration.length)
    assertEquals(5, anywhere.duration.length)
  }

  @Test
  fun `without a day at home the task is as long as without the mark`() {
    val anna = resource()
    val onSite = task(anna, onSite = true, effort = 40)
    val anywhere = task(anna, onSite = false, effort = 40)
    assertEquals(anywhere.duration.length, onSite.duration.length)
  }

  @Test
  fun `the days at home of a resource which does not block do not matter`() {
    val anna = resource()
    anna.homeOfficeWeek = listOf(WorkWeekChange(null, setOf(FRIDAY)))
    val task = task(anna, onSite = true, effort = 40)
    task.assignments.single().isBlocking = false
    task.createMutator().also { it.setEffort(BigDecimal(40)); it.commit() }
    assertEquals(5, task.duration.length)
  }

  @Test
  fun `levelling keeps a task which needs somebody on site off the days at home`() {
    val anna = resource()
    anna.homeOfficePeriods = listOf(HomeOfficePeriod(monday.plusDays(1), monday.plusDays(2)))
    val onSite = task(anna, onSite = true, days = 2)
    val result = level()
    // Monday and Tuesday are the window of the task, and Tuesday is at home.
    assertEquals(monday.plusDays(2), result.starts[onSite.id])
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun `levelling leaves a task which may be done from home on its days`() {
    val anna = resource()
    anna.homeOfficePeriods = listOf(HomeOfficePeriod(monday.plusDays(1), monday.plusDays(2)))
    val anywhere = task(anna, onSite = false, days = 2)
    assertEquals(monday, level().starts[anywhere.id])
  }

  @Test
  fun `an effort driven task which needs somebody on site does not start on a day at home`() {
    val anna = resource()
    anna.homeOfficePeriods = listOf(HomeOfficePeriod(monday, monday.plusDays(1)))
    val onSite = task(anna, onSite = true, effort = 8)
    assertEquals(monday.plusDays(1), level().starts[onSite.id])
  }

  @Test
  fun `levelling reports a task which needs somebody on site if every day is at home, and ends`() {
    val anna = resource()
    anna.homeOfficePeriods = listOf(HomeOfficePeriod(LocalDate.of(2000, 1, 1), LocalDate.of(2999, 1, 1)))
    val fixed = task(anna, onSite = true, days = 2)
    val effortDriven = task(anna, onSite = true, effort = 16)
    val result = assertTimeoutPreemptively(Duration.ofSeconds(60), ThrowingSupplier { level() })
    val conflicts = result.conflicts.filterIsInstance<LevelConflict.NoPossibleDate>()
    assertEquals(setOf(fixed.id, effortDriven.id), conflicts.map { it.id }.toSet(), "${result.conflicts} ${result.starts}")
    assertTrue(conflicts.all { it.atHome == listOf(anna.id.toString()) && it.blocking.isEmpty() })
  }

  private fun save(): String {
    val out = ByteArrayOutputStream()
    GanttXMLSaver(project).save(out)
    return out.toString(Charsets.UTF_8)
  }

  @Test
  fun `the pattern, the periods and the mark survive a save and load`() {
    val anna = resource()
    anna.homeOfficeWeek = listOf(WorkWeekChange(null, setOf(FRIDAY)), WorkWeekChange(LocalDate.of(2027, 1, 1), emptySet()))
    anna.homeOfficePeriods = listOf(HomeOfficePeriod(monday, LocalDate.of(2026, 11, 5)))
    task(anna, onSite = true)
    task(anna, onSite = false)
    val xml = save()
    assertTrue(xml.contains("<home-office-period start=\"2026-11-02\" end=\"2026-11-05\""), xml)
    val loaded = XmlProjectImporter().import(xml)
    val resource = loaded.humanResourceManager.resources.single()
    assertEquals(anna.homeOfficeWeek, resource.homeOfficeWeek)
    assertEquals(anna.homeOfficePeriods, resource.homeOfficePeriods)
    assertEquals(listOf(true, false), loaded.taskManager.tasks.map { it.isOnSiteOnly })
  }

  @Test
  fun `a file written without days at home loads without any`() {
    val loaded = XmlProjectImporter().import("""
      <project name="p">
        <tasks><task id="1" uid="u1" name="T" start="2026-11-09" duration="1"/></tasks>
        <resources><resource id="0" name="R" function="Default:0" contacts="" phone=""/></resources>
      </project>
    """.trimIndent())
    val resource = loaded.humanResourceManager.resources.single()
    assertTrue(resource.homeOffice.isEmpty)
    assertFalse(loaded.taskManager.tasks.single().isOnSiteOnly)
    assertFalse(save().contains("home-office"))
    assertFalse(save().contains("on-site-only"))
  }

  @Test
  fun `a period which ends where it starts is read as its first day`() {
    val loaded = XmlProjectImporter().import("""
      <project name="p">
        <resources><resource id="0" name="R" function="Default:0" contacts="" phone="">
          <home-office-period start="2026-11-02" end="2026-11-02"/>
        </resource></resources>
      </project>
    """.trimIndent())
    assertEquals(listOf(HomeOfficePeriod(monday, monday.plusDays(1))), loaded.humanResourceManager.resources.single().homeOfficePeriods)
  }
}
