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
import net.sourceforge.ganttproject.action.resource.DaysOffPreview
import net.sourceforge.ganttproject.action.resource.endShifts
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.daysOffRanges
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * What new days off of a person move, shown before they are saved. The week starts on Monday 2026-11-02, and the task
 * of 16 hours of anna takes Monday and Tuesday.
 */
class DaysOffPreviewTest {
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

  private fun date(day: LocalDate) = CalendarFactory.createGanttCalendar(day.year, day.monthValue - 1, day.dayOfMonth).time

  private fun anna(): HumanResource =
    project.humanResourceManager.create("anna", 0).also { it.hoursPerDay = BigDecimal("8") }

  private fun task(name: String, person: HumanResource): Task =
    project.taskManager.newTaskBuilder().withName(name).withStartDate(date(monday))
      .withDuration(project.taskManager.createLength(2)).build().also {
        it.createMutator().also { mutator ->
          mutator.setEffort(BigDecimal("16"))
          mutator.setEffortDriven(true)
          mutator.commit()
        }
        it.assignmentCollection.addAssignment(person).also { assignment ->
          assignment.load = 100f
          assignment.isBlocking = true
        }
      }

  /** The day off the way the person dialog stores it: the end is the day after the last day off. */
  private fun dayOff(first: LocalDate, last: LocalDate = first) = GanttDaysOff(date(first), date(last.plusDays(1)))

  private class Answers(private val answer: Boolean) {
    val asked = mutableListOf<String>()
    val confirmation = net.sourceforge.ganttproject.action.task.LevellingConfirmation { message, callback ->
      asked.add(message)
      callback(answer)
    }
  }

  private fun preview(answers: Answers) = DaysOffPreview(project.taskManager, answers.confirmation) { monday }

  @Test
  fun `the preview lists the task which ends later`() {
    val person = anna()
    val task = task("report", person)
    val shifts = endShifts(project.taskManager, person, listOf(monday.plusDays(1) to monday.plusDays(2)), monday)
    assertEquals(listOf(task), shifts.map { it.task })
    assertEquals(monday.plusDays(1), shifts.single().oldEnd)
    assertEquals(monday.plusDays(3), shifts.single().newEnd)
    // Asking does not write the days off of the person.
    assertTrue(person.daysOffRanges().isEmpty())
  }

  @Test
  fun `the dialog names the task with its old and new end`() {
    val person = anna()
    task("report", person)
    val answers = Answers(true)
    preview(answers).confirmThen(person, listOf(dayOff(monday.plusDays(1))), Runnable {})
    val message = answers.asked.single()
    assertTrue(message.contains("report: ${monday.plusDays(1)}"), message)
    assertTrue(message.contains(monday.plusDays(3).toString()), message)
  }

  @Test
  fun `cancelling saves nothing and leaves the tasks and the undo history alone`() {
    val person = anna()
    val task = task("report", person)
    val startBefore = task.start.time
    val endBefore = task.end.time
    val edits = mutableListOf<String>()
    val answers = Answers(false)
    preview(answers).confirmThen(person, listOf(dayOff(monday.plusDays(1))), Runnable { edits.add("saved") })
    assertEquals(1, answers.asked.size)
    assertTrue(edits.isEmpty())
    assertTrue(person.daysOff.isEmpty())
    assertEquals(startBefore, task.start.time)
    assertEquals(endBefore, task.end.time)
  }

  @Test
  fun `confirming saves`() {
    val person = anna()
    task("report", person)
    val edits = mutableListOf<String>()
    preview(Answers(true)).confirmThen(person, listOf(dayOff(monday.plusDays(1))), Runnable { edits.add("saved") })
    assertEquals(listOf("saved"), edits)
  }

  @Test
  fun `no dialog when nothing moves`() {
    val person = anna()
    task("report", person)
    val answers = Answers(false)
    val edits = mutableListOf<String>()
    // A day off long after the task, and none at all, and the days off which are there already.
    preview(answers).confirmThen(person, listOf(dayOff(monday.plusDays(60))), Runnable { edits.add("late") })
    preview(answers).confirmThen(person, emptyList(), Runnable { edits.add("none") })
    assertTrue(answers.asked.isEmpty(), answers.asked.toString())
    assertEquals(listOf("late", "none"), edits)
  }
}
