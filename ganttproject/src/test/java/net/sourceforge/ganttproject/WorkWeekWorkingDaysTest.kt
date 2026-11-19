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
import biz.ganttproject.core.model.task.WorkWeekChange
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import biz.ganttproject.task.recurrence.toModelDate
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.WorkWeekWorkingDays
import net.sourceforge.ganttproject.task.algorithm.isInvolvedInWorkWeek
import net.sourceforge.ganttproject.task.algorithm.workingDayTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.util.Locale

/**
 * The working days which [WorkWeekWorkingDays] gives an effort driven task in a real project calendar: which days a
 * working week takes away or adds, from which day a dated change applies, which assignments decide, and that holidays
 * stay free. The week starts on Monday 2026-11-02; the project calendar has the weekend free.
 */
class WorkWeekWorkingDaysTest {
  private lateinit var project: GanttProjectImpl
  private val monday = LocalDate.of(2026, 11, 2)
  private val tuesday = monday.plusDays(1)
  private val wednesday = monday.plusDays(2)
  private val thursday = monday.plusDays(3)
  private val friday = monday.plusDays(4)
  private val saturday = monday.plusDays(5)
  private val sunday = monday.plusDays(6)

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

  private val weekdays = arrayOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

  private fun resource(name: String, vararg week: WorkWeekChange): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal(8)
      it.workWeek = week.toList()
    }

  private fun effortTask(): Task =
    project.taskManager.newTaskBuilder().withName("t").withStartDate(monday.toModelDate())
      .withDuration(project.taskManager.createLength(1)).build().also { task ->
        task.createMutator().also {
          it.setEffort(BigDecimal(8))
          it.setEffortDriven(true)
          it.commit()
        }
      }

  private fun Task.assign(resource: HumanResource, load: Float = 100f): ResourceAssignment =
    assignmentCollection.addAssignment(resource).also { it.load = load }

  private fun holidays(vararg days: LocalDate) {
    project.taskManager.calendar.publicHolidays = days.map {
      CalendarEvent.newEvent(it.toModelDate(), false, CalendarEvent.Type.HOLIDAY, null, null)
    }
  }

  private fun workingDays(task: Task) = WorkWeekWorkingDays(project.taskManager.calendar).forTask(task)

  private fun daysOf(task: Task, from: LocalDate = monday, to: LocalDate = sunday): List<LocalDate> {
    val worksOn = workingDays(task)
    return generateSequence(from) { if (it < to) it.plusDays(1) else null }.filter(worksOn).toList()
  }

  // Without a statement nothing changes

  @Test
  fun `a week equal to the project week gives the days of the project calendar, holidays included`() {
    holidays(LocalDate.of(2026, 12, 25))
    val task = effortTask()
    task.assign(resource("anna", week(*weekdays)))
    task.assign(resource("bob"))
    // 400 days cross many weekends, a holiday and a year boundary.
    val projectDays = workingDayTest(project.taskManager.calendar)
    val worksOn = workingDays(task)
    assertEquals(emptyList<LocalDate>(),
      generateSequence(monday) { it.plusDays(1) }.take(400).filter { projectDays(it) != worksOn(it) }.toList())
  }

  @Test
  fun `a task without assignments keeps the project calendar and does not work on the weekend`() {
    assertEquals(listOf(monday, tuesday, wednesday, thursday, friday), daysOf(effortTask()))
  }

  // Taking days away and adding them

  @Test
  fun `a resource working Monday, Tuesday, Friday and Saturday gives the task Monday, Tuesday and Friday`() {
    val task = effortTask()
    task.assign(resource("anna", week(MONDAY, TUESDAY, FRIDAY, SATURDAY)))
    assertEquals(listOf(monday, tuesday, friday), daysOf(task))
  }

  @Test
  fun `Sunday is no day of the task although the resource names it, because the project is free on Sunday`() {
    val task = effortTask()
    task.assign(resource("anna", week(MONDAY, SUNDAY)))
    assertEquals(listOf(monday), daysOf(task))
  }

  @Test
  fun `a dated change which takes a day away applies on its date, not a day earlier or later`() {
    val task = effortTask()
    task.assign(resource("anna", week(MONDAY, TUESDAY, THURSDAY, FRIDAY, from = wednesday)))
    val worksOn = workingDays(task)
    // Before the change the resource makes no statement, so the project calendar applies.
    assertTrue(worksOn(tuesday))
    assertFalse(worksOn(wednesday))
    assertTrue(worksOn(thursday))
  }

  @Test
  fun `a dated change which takes a weekday away applies from its date on, every week`() {
    val task = effortTask()
    task.assign(resource("anna", week(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, from = friday)))
    val worksOn = workingDays(task)
    assertTrue(worksOn(friday.minusDays(7)))
    assertFalse(worksOn(friday))
    assertFalse(worksOn(friday.plusDays(7)))
  }

  // Several resources

  @Test
  fun `Saturday is never a day of the task, not even when both resources work on it`() {
    val oneOfTwo = effortTask()
    oneOfTwo.assign(resource("anna", week(*weekdays, SATURDAY)))
    oneOfTwo.assign(resource("bob", week(*weekdays)))
    assertFalse(workingDays(oneOfTwo)(saturday))

    val both = effortTask()
    both.assign(resource("carl", week(*weekdays, SATURDAY)))
    both.assign(resource("dora", week(*weekdays, SATURDAY)))
    assertFalse(workingDays(both)(saturday))
  }

  @Test
  fun `a resource without a working week takes no weekday away, while another resource still can`() {
    val task = effortTask()
    task.assign(resource("anna", week(MONDAY, TUESDAY, THURSDAY, FRIDAY)))
    task.assign(resource("bob"))
    assertFalse(workingDays(task)(wednesday))
    assertTrue(workingDays(task)(thursday))
  }

  // Holidays

  @Test
  fun `a holiday stays free even if every involved resource works on its weekday`() {
    holidays(wednesday)
    val task = effortTask()
    task.assign(resource("anna", week(*weekdays, SATURDAY)))
    task.assign(resource("bob", week(*weekdays, SATURDAY)))
    val worksOn = workingDays(task)
    assertFalse(worksOn(wednesday))
    // Saturday is free in the project too, so the task does not work on it either.
    assertFalse(worksOn(saturday))
  }

  // Which assignments decide

  @Test
  fun `a blocking assignment which brings no effort decides on a weekday`() {
    val task = effortTask()
    task.assign(resource("anna", week(*weekdays)))
    task.assign(resource("bob", week(MONDAY, TUESDAY, WEDNESDAY, THURSDAY))).also {
      it.isNoEffort = true
      it.isBlocking = true
    }
    assertFalse(workingDays(task)(friday))
  }

  @Test
  fun `an assignment which brings no effort and is not blocking does not decide`() {
    val task = effortTask()
    task.assign(resource("anna", week(*weekdays)))
    task.assign(resource("bob", week(MONDAY, TUESDAY, WEDNESDAY, THURSDAY))).also {
      it.isNoEffort = true
      it.isBlocking = false
    }
    assertTrue(workingDays(task)(friday))
  }

  @Test
  fun `an ordinary assignment is involved although it is not blocking, an assignment without load is not`() {
    val task = effortTask()
    val ordinary = task.assign(resource("anna", week(*weekdays)))
    assertFalse(ordinary.isBlocking)
    assertTrue(ordinary.isInvolvedInWorkWeek)
    val withoutLoad = task.assign(resource("bob", week(*weekdays)), load = 0f).also { it.isBlocking = true }
    assertFalse(withoutLoad.isInvolvedInWorkWeek)
  }
}
