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
import biz.ganttproject.core.model.task.WorkWeekChange
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import biz.ganttproject.task.recurrence.toModelDate
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.WorkWeekWorkingDays
import net.sourceforge.ganttproject.task.algorithm.collectLevelTasks
import net.sourceforge.ganttproject.task.algorithm.durationAtStart
import net.sourceforge.ganttproject.task.algorithm.effortDrivenDurationDays
import net.sourceforge.ganttproject.task.algorithm.effortInputs
import net.sourceforge.ganttproject.task.algorithm.workingDaysOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.util.Locale

/**
 * Working weeks and days off together: the derived duration and levelling count the days off on the working days of
 * the task only, and both give the same number of days. Durations are in working days of the task. The week starts
 * on Monday 2026-11-02; resources work 8 hours a day.
 */
class WorkWeekDaysOffTest {
  private lateinit var project: GanttProjectImpl
  private val monday = LocalDate.of(2026, 11, 2)
  private val wednesday = monday.plusDays(2)
  private val thursday = monday.plusDays(3)

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

  private fun week(vararg days: DayOfWeek) = listOf(WorkWeekChange(null, days.toSet()))

  private val monTueThuFri = week(MONDAY, TUESDAY, THURSDAY, FRIDAY)

  private fun resource(name: String, week: List<WorkWeekChange> = emptyList()): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal(8)
      it.workWeek = week
    }

  private fun HumanResource.dayOff(day: LocalDate) = addDaysOff(GanttDaysOff(day.toModelDate(), day.plusDays(1).toModelDate()))

  private fun effortTask(effort: Int, vararg resources: HumanResource, blocking: HumanResource? = null): Task =
    project.taskManager.newTaskBuilder().withName("t").withStartDate(monday.toModelDate())
      .withDuration(project.taskManager.createLength(1)).build().also { task ->
        resources.forEach { task.assignmentCollection.addAssignment(it).load = 100f }
        blocking?.let { task.assignmentCollection.addAssignment(it).also { a -> a.load = 100f; a.isBlocking = true } }
        task.createMutator().also {
          it.setEffort(BigDecimal(effort))
          it.setEffortDriven(true)
          it.commit()
        }
      }

  /** The duration which the scheduler derives, in working days of the task. */
  private fun derivedDays(task: Task): Long? {
    val worksOn = WorkWeekWorkingDays(project.taskManager.calendar).forTask(task)
    return effortDrivenDurationDays(task.effort, effortInputs(task.assignments), monday, worksOn)
  }

  /** The durations which one levelling run gives the tasks when they start on Monday. */
  private fun levelledDays(vararg tasks: Task): List<Int> {
    val workingDays = WorkWeekWorkingDays(project.taskManager.calendar)
    val levelTasks = collectLevelTasks(project.taskManager, monday, workingDays = workingDays)
    val duration = durationAtStart(project.taskManager, workingDaysOf(project.taskManager, workingDays))
    return tasks.map { task -> duration(levelTasks.single { it.id == task.taskID.toString() }, monday) }
  }

  @Test
  fun `the scheduler and levelling count a day off on the working days of the task`() {
    val anna = resource("anna", week(MONDAY, TUESDAY, THURSDAY, FRIDAY))
    anna.dayOff(wednesday)
    anna.dayOff(thursday)
    // Wednesday is no day of the task, Thursday is one and Anna is off: Monday, Tuesday, Friday and the Monday after.
    // Without the work week it would be six days, without the days off four.
    val task = effortTask(32, anna)
    assertEquals(5L, derivedDays(task))
    assertEquals(listOf(5), levelledDays(task))
    // In the task model this is the project calendar from Monday to the Monday after.
    assertEquals(6, task.duration.length)
  }

  @Test
  fun `a day off of a blocking resource on a day its work week takes away costs nothing more`() {
    val anna = resource("anna")
    val bob = resource("bob", monTueThuFri)
    val withoutDayOff = effortTask(56, anna, blocking = bob)
    assertEquals(listOf(4), levelledDays(withoutDayOff))
    bob.dayOff(wednesday)
    val withDayOff = effortTask(56, anna, blocking = bob)
    assertEquals(4L, derivedDays(withDayOff))
    assertEquals(4, levelledDays(withDayOff).single())
  }

  @Test
  fun `a day off of a blocking resource on a working day of the task takes the whole day`() {
    val anna = resource("anna")
    val bob = resource("bob", monTueThuFri)
    bob.dayOff(thursday)
    // Nobody brings hours on Thursday, Anna neither, so 56 hours at 16 a day take five days.
    val task = effortTask(56, anna, blocking = bob)
    assertEquals(5L, derivedDays(task))
    assertEquals(listOf(5), levelledDays(task))
  }

  @Test
  fun `two tasks with different work weeks get their own days in one levelling run`() {
    val sam = resource("sam", week(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY))
    val wolf = resource("wolf", week(MONDAY, TUESDAY, THURSDAY, FRIDAY))
    sam.dayOff(wednesday)
    wolf.dayOff(wednesday)
    // Wednesday is a day of Sam's task and costs a day; it is no day of Wolf's task and costs nothing.
    val withWednesday = effortTask(48, sam)
    val withoutWednesday = effortTask(48, wolf)
    assertEquals(listOf(7, 6), levelledDays(withWednesday, withoutWednesday))
    assertEquals(listOf(6, 7), levelledDays(withoutWednesday, withWednesday))
    assertEquals(7L, derivedDays(withWednesday))
    assertEquals(6L, derivedDays(withoutWednesday))
  }
}
