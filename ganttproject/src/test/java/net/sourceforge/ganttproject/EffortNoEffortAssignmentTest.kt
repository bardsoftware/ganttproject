/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

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
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * An assignment marked as no effort keeps the resource on the task but brings no hours, so it does not shorten an
 * effort driven task, and its days off do not lengthen it. The week starts on Monday 2026-11-02.
 */
class EffortNoEffortAssignmentTest {
  private lateinit var project: GanttProjectImpl
  private val monday = LocalDate.of(2026, 11, 2)
  private var storedDays = 1L

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

  private fun resource(name: String, hoursPerDay: String = "8"): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal(hoursPerDay)
    }

  private fun date(day: LocalDate) = CalendarFactory.createGanttCalendar(day.year, day.monthValue - 1, day.dayOfMonth).time

  private fun HumanResource.dayOff(first: LocalDate, last: LocalDate = first) {
    addDaysOff(GanttDaysOff(date(first), date(last.plusDays(1))))
  }

  private fun effortTask(effort: String, vararg resources: HumanResource): Task =
    project.taskManager.newTaskBuilder().withName("t")
      .withStartDate(date(monday))
      .withDuration(project.taskManager.createLength(storedDays)).build().also { task ->
        resources.forEach { task.assignmentCollection.addAssignment(it).load = 100f }
        task.createMutator().also {
          it.setEffort(BigDecimal(effort))
          it.setEffortDriven(true)
          it.commit()
        }
      }

  private fun Task.days() = duration.length

  private fun Task.assignmentOf(resource: HumanResource) = assignments.single { it.resource == resource }

  @Test
  fun `with both flags off the duration is the one without flags`() {
    val anna = resource("anna")
    val bob = resource("bob")
    bob.dayOff(monday)
    val task = effortTask("32", anna, bob)
    task.assignments.forEach {
      it.isBlocking = false
      it.isNoEffort = false
    }
    // The same as in EffortDaysOffTest: Monday 8, Tuesday 16, Wednesday 16.
    assertEquals(3, task.days())
  }

  @Test
  fun `a no effort assignment does not shorten the task`() {
    val anna = resource("anna")
    val bob = resource("bob")
    val task = effortTask("32", anna, bob)
    assertEquals(2, task.days())

    task.assignmentOf(bob).isNoEffort = true
    assertEquals(4, task.days())

    task.assignmentOf(bob).isNoEffort = false
    assertEquals(2, task.days())
  }

  @Test
  fun `the days off of a no effort assignment do not lengthen the task`() {
    val anna = resource("anna")
    val bob = resource("bob")
    bob.dayOff(monday, monday.plusDays(1))
    val task = effortTask("16", anna, bob)
    // Monday 8, Tuesday 8: Bob brings nothing on his days off.
    assertEquals(2, task.days())

    task.assignmentOf(bob).isNoEffort = true
    task.assignmentOf(anna).isNoEffort = false
    assertEquals(2, task.days())

    // Anna's days off still count.
    anna.dayOff(monday.plusDays(1))
    assertEquals(3, task.days())
  }

  @Test
  fun `a no effort assignment with days off brings no hours on the other days either`() {
    val anna = resource("anna")
    val bob = resource("bob")
    bob.dayOff(monday.plusDays(2))
    val task = effortTask("32", anna, bob)
    task.assignmentOf(bob).isNoEffort = true
    // Anna alone, Monday to Thursday. Counting Bob's hours around his day off would give two days.
    assertEquals(4, task.days())
  }

  @Test
  fun `a task with only no effort assignments keeps its stored duration`() {
    val anna = resource("anna")
    anna.dayOff(monday.plusDays(1))
    storedDays = 5
    val task = effortTask("24", anna)
    assertEquals(4, task.days())

    task.assignmentOf(anna).isNoEffort = true
    // Nothing to derive a duration from, like a task without resources: the last duration stays.
    assertEquals(4, task.days())
  }

  @Test
  fun `blocking alone does not change the duration`() {
    val anna = resource("anna")
    anna.dayOff(monday.plusDays(2))
    val task = effortTask("24", anna)
    assertEquals(4, task.days())
    task.assignmentOf(anna).isBlocking = true
    assertEquals(4, task.days())
  }
}
