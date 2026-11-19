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

import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.io.XmlProjectImporter
import biz.ganttproject.core.model.task.LevelTask
import biz.ganttproject.core.model.task.WorkWeekChange
import biz.ganttproject.core.model.task.WorkWeekSchedule
import biz.ganttproject.core.model.task.formatWorkWeek
import biz.ganttproject.core.model.task.levelTasks
import biz.ganttproject.core.model.task.parseWorkWeek
import biz.ganttproject.core.model.task.taskWorksOn
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.applyLevelling
import net.sourceforge.ganttproject.task.algorithm.collectLevelTasks
import net.sourceforge.ganttproject.task.algorithm.durationAtStart
import net.sourceforge.ganttproject.task.algorithm.workingDaysOf
import net.sourceforge.ganttproject.task.algorithm.WorkWeekWorkingDays
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.ThrowingSupplier
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.TUESDAY
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

/**
 * The working week of a resource: the model, the project file, and its effect on the duration of effort driven tasks
 * and on levelling. The week starts on Monday 2026-11-02; the project calendar has the weekend free.
 */
class WorkWeekTest {
  private lateinit var project: GanttProjectImpl
  private val monday = LocalDate.of(2026, 11, 2)

  private val undoManager = object : GPUndoManager {
    override fun undoableEdit(localizedName: String, runnableEdit: Runnable) = runnableEdit.run()
    override fun canUndo() = false
    override fun canRedo() = false
    override fun undo() {}
    override fun redo() {}
    override val undoPresentationName = ""
    override val redoPresentationName = ""
    override fun addUndoableEditListener(listener: GPUndoListener) {}
    override fun removeUndoableEditListener(listener: GPUndoListener) {}
    override fun die() {}
    override fun addUndoableEditTxnFactory(factory: UndoableEditTxnFactory) {}
  }

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

  private fun week(vararg days: DayOfWeek, from: LocalDate? = null) = WorkWeekChange(from, days.toSet())

  private fun resource(name: String, vararg week: WorkWeekChange): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal(8)
      it.workWeek = week.toList()
    }

  private fun date(day: LocalDate) = CalendarFactory.createGanttCalendar(day.year, day.monthValue - 1, day.dayOfMonth).time

  private fun effortTask(effort: String, vararg resources: HumanResource, start: LocalDate = monday): Task =
    project.taskManager.newTaskBuilder().withName("t")
      .withStartDate(date(start))
      .withDuration(project.taskManager.createLength(1)).build().also { task ->
        resources.forEach { task.assignmentCollection.addAssignment(it).load = 100f }
        task.createMutator().also {
          it.setEffort(BigDecimal(effort))
          it.setEffortDriven(true)
          it.commit()
        }
      }

  private fun Task.days() = duration.length

  private fun Task.startDay(): LocalDate = LocalDate.of(start.year, start.month + 1, start.date)

  private fun Task.endDay(): LocalDate = LocalDate.of(end.year, end.month + 1, end.date)

  // The model

  @Test
  fun `a resource without a working week makes no statement, a dated change applies from its date`() {
    assertNull(WorkWeekSchedule().worksOn(monday))
    val schedule = WorkWeekSchedule(listOf(week(MONDAY, from = monday.plusDays(7)), week(MONDAY, TUESDAY)))
    assertEquals(true, schedule.worksOn(monday.plusDays(1)))
    assertEquals(false, schedule.worksOn(monday.plusDays(8)))
    assertEquals(true, schedule.worksOn(monday.plusDays(7)))
    assertNull(WorkWeekSchedule(listOf(week(MONDAY, from = monday.plusDays(7)))).worksOn(monday))
  }

  @Test
  fun `a typed working week is read back after formatting, also a change without days`() {
    val changes = parseWorkWeek("1,2,5,6; 2027-03-01 = 1,2,3\n2027-05-01:")!!
    assertEquals(listOf(week(MONDAY, TUESDAY, FRIDAY, SATURDAY), week(MONDAY, TUESDAY, DayOfWeek.WEDNESDAY,
      from = LocalDate.of(2027, 3, 1)), week(from = LocalDate.of(2027, 5, 1))), changes)
    assertEquals("1,2,5,6; 2027-03-01: 1,2,3; 2027-05-01:", formatWorkWeek(changes))
    assertEquals(changes, parseWorkWeek(formatWorkWeek(changes)))
    assertEquals(listOf(week()), parseWorkWeek(formatWorkWeek(listOf(week()))))
  }

  @Test
  fun `a typed working week with one invalid entry is not read at all`() {
    listOf("1,8", "0", "Mo", "2027-13-01: 1", "1; 03.04.2027: 2").forEach { assertNull(parseWorkWeek(it), it) }
  }

  @Test
  fun `a task works on a day when the project and every involved resource do, but never on a day the project leaves free`() {
    val monTue = WorkWeekSchedule(listOf(week(MONDAY, TUESDAY)))
    val withSaturday = WorkWeekSchedule(listOf(week(MONDAY, SATURDAY)))
    val nothing = WorkWeekSchedule()
    val wednesday = monday.plusDays(2)
    val saturday = monday.plusDays(5)
    assertFalse(taskWorksOn(wednesday, true, listOf(monTue)))
    assertTrue(taskWorksOn(wednesday, true, listOf(nothing)))
    // The resource names Saturday, but the project leaves it free, be it for its weekday or as a holiday.
    assertFalse(taskWorksOn(saturday, false, listOf(withSaturday)))
    assertFalse(taskWorksOn(saturday, false, listOf(withSaturday, nothing)))
    assertTrue(taskWorksOn(wednesday, true, emptyList()))
    assertFalse(taskWorksOn(wednesday, false, emptyList()))
  }

  // The project file

  private fun save(project: IGanttProject): String {
    val out = ByteArrayOutputStream()
    GanttXMLSaver(project).save(out)
    return out.toString(Charsets.UTF_8)
  }

  @Test
  fun `the working week survives a save and load`() {
    val changes = listOf(week(MONDAY, TUESDAY), week(from = LocalDate.of(2027, 5, 1)), week(FRIDAY, from = LocalDate.of(2027, 6, 1)))
    val source = XmlProjectImporter().import("""<project name="p"><resources><resource id="0" name="anna" function="Default:0"/></resources></project>""")
    source.humanResourceManager.getById(0).workWeek = changes
    val saved = save(source)
    assertTrue(saved.contains("""<work-week days="1,2"/>"""), saved)
    assertTrue(saved.contains("""<work-week date="2027-05-01" days=""/>"""), saved)
    assertEquals(changes, XmlProjectImporter().import(saved).humanResourceManager.getById(0).workWeek, saved)
  }

  @Test
  fun `a file without a working week loads without one and saves none`() {
    val loaded = XmlProjectImporter().import(
      """<project name="p"><resources><resource id="0" name="anna" function="Default:0"/></resources></project>""")
    assertEquals(emptyList<WorkWeekChange>(), loaded.humanResourceManager.getById(0).workWeek)
    assertFalse(save(loaded).contains("work-week"))
  }

  @Test
  fun `an unreadable entry of the working week in the file is skipped`() {
    val loaded = XmlProjectImporter().import("""<project name="p"><resources><resource id="0" name="anna" function="Default:0">
      <work-week days="1,9"/><work-week date="soon" days="1"/><work-week date="2027-01-04" days="2"/>
      </resource></resources></project>""")
    assertEquals(listOf(week(TUESDAY, from = LocalDate.of(2027, 1, 4))), loaded.humanResourceManager.getById(0).workWeek)
  }

  // The duration

  @Test
  fun `without a working week the duration is the one of the project calendar`() {
    val anna = resource("anna")
    val task = effortTask("24", anna)
    assertEquals(3, task.days())
    assertSame(WorkWeekWorkingDays(project.taskManager.calendar).let { it.forTask(task) === it.projectOnly }, true)
  }

  @Test
  fun `a resource which works on Monday and Tuesday only makes the task longer in the project calendar`() {
    val anna = resource("anna", week(MONDAY, TUESDAY))
    // Monday, Tuesday, and the Monday after: the task runs until that Monday, six working days of the project.
    val task = effortTask("24", anna)
    assertEquals(6, task.days())
    assertEquals(monday.plusDays(8), task.endDay())
  }

  @Test
  fun `a Saturday which the resource names is no day of the task, the project leaves it free`() {
    val anna = resource("anna", week(MONDAY, TUESDAY, FRIDAY, SATURDAY))
    val bob = resource("bob")
    // Anna alone: Monday, Tuesday, Friday and the Monday after make four days of work, six days of the project.
    assertEquals(6, effortTask("32", anna).days())
    // With Bob, who follows the project calendar, Anna does not work Wednesday and Thursday.
    assertEquals(2, effortTask("32", anna, bob).days())
    assertEquals(6, effortTask("64", anna, bob).days())
  }

  @Test
  fun `a day off on a day which the resource does not work costs nothing`() {
    val anna = resource("anna", week(MONDAY, TUESDAY))
    anna.addDaysOff(GanttDaysOff(date(monday.plusDays(2)), date(monday.plusDays(3))))
    assertEquals(6, effortTask("24", anna).days())
  }

  @Test
  fun `a week without a working day ends with a short task instead of running on`() {
    val anna = resource("anna", week())
    val task = assertTimeoutPreemptively(Duration.ofSeconds(20), ThrowingSupplier { effortTask("24", anna) })
    assertTrue(task.days() <= 1, "a task without any working day keeps a short duration, got ${task.days()}")
  }

  // Levelling

  @Test
  fun `levelling places a task on the working days of its resource`() {
    val anna = resource("anna", week(MONDAY, TUESDAY))
    val first = effortTask("16", anna)
    val second = effortTask("16", anna)
    val workingDays = WorkWeekWorkingDays(project.taskManager.calendar)
    val tasks = collectLevelTasks(project.taskManager, monday, workingDays = workingDays)
    assertEquals(listOf(2, 2), tasks.map { it.durationDays }, "levelling counts the days of the resource")
    val worksOn = workingDaysOf(project.taskManager, workingDays)
    val result = levelTasks(tasks, monday, workingDays.projectOnly, durationAtStart(project.taskManager, worksOn),
      workingDaysOf = worksOn)
    applyLevelling(result.starts, result.durations, project.taskManager, undoManager, "level")
    assertEquals(monday, first.startDay())
    // The second task cannot start on Wednesday, which Anna does not work, but on the Monday after.
    assertEquals(monday.plusDays(7), second.startDay())
    assertEquals(monday.plusDays(9), second.endDay())
  }

  @Test
  fun `levelling without working weeks is the same as before`() {
    val anna = resource("anna")
    val first = effortTask("16", anna)
    val second = effortTask("16", anna)
    val tasks = collectLevelTasks(project.taskManager, monday)
    val result = levelTasks(tasks, monday, WorkWeekWorkingDays(project.taskManager.calendar).projectOnly,
      durationAtStart(project.taskManager))
    applyLevelling(result.starts, result.durations, project.taskManager, undoManager, "level")
    assertEquals(monday, first.startDay())
    assertEquals(monday.plusDays(2), second.startDay())
    assertEquals(2, second.days())
  }

  @Test
  fun `levelling a task whose resource works on no day ends`() {
    // The resource is booked for a long time, so that every day would be a candidate which is searched to the limit.
    val tasks = listOf(
      LevelTask(id = "a", orderInPlan = 0, priority = 0, durationDays = 20_000, predecessors = emptyList(),
        fixedStart = null, earliestStart = null, loads = mapOf("r" to 100), frozen = false, deadline = null, blocking = emptySet()),
      LevelTask(id = "b", orderInPlan = 1, priority = 0, durationDays = 2, predecessors = emptyList(),
        fixedStart = null, earliestStart = null, loads = mapOf("r" to 100), frozen = false, deadline = null, blocking = emptySet()))
    val never: (LocalDate) -> Boolean = { false }
    val result = assertTimeoutPreemptively(Duration.ofSeconds(20), ThrowingSupplier {
      levelTasks(tasks, monday, { true }, workingDaysOf = { task -> if (task.id == "b") never else { _ -> true } })
    })
    assertEquals(monday, result.starts["a"])
    assertTrue(result.starts.containsKey("b"))
  }
}
