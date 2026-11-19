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
import biz.ganttproject.core.model.task.levelTasks
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.applyLevelling
import net.sourceforge.ganttproject.task.algorithm.collectLevelTasks
import net.sourceforge.ganttproject.task.algorithm.durationAtStart
import net.sourceforge.ganttproject.task.algorithm.startAllowed
import net.sourceforge.ganttproject.task.algorithm.workingDayTest
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * Only an assignment marked as blocking is held up by a day off. The day off of a blocking resource takes the whole
 * day of the task: nobody makes progress on it, but it still counts as a day of the task. A day off of a resource
 * which is not blocking only takes the hours of that resource. Levelling does not start an effort driven task on a day
 * off of a blocking resource, and ignores the days off of all the others. The week starts on Monday 2026-11-02.
 */
class EffortBlockingDaysOffTest {
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

  private fun resource(name: String): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal("8")
    }

  private fun date(day: LocalDate) = CalendarFactory.createGanttCalendar(day.year, day.monthValue - 1, day.dayOfMonth).time

  /** The day off the way the person dialog stores it: the end is the day after the last day off. */
  private fun HumanResource.dayOff(first: LocalDate, last: LocalDate = first) {
    addDaysOff(GanttDaysOff(date(first), date(last.plusDays(1))))
  }

  private fun effortTask(effort: String): Task =
    project.taskManager.newTaskBuilder().withName("t").withStartDate(date(monday))
      .withDuration(project.taskManager.createLength(1)).build().also {
        it.createMutator().also { mutator ->
          mutator.setEffort(BigDecimal(effort))
          mutator.setEffortDriven(true)
          mutator.commit()
        }
      }

  private fun Task.assign(resource: HumanResource, blocking: Boolean = false, noEffort: Boolean = false, load: Float = 100f) {
    assignmentCollection.addAssignment(resource).also {
      it.load = load
      it.isBlocking = blocking
      it.isNoEffort = noEffort
    }
  }

  private fun Task.days() = duration.length

  private fun Task.startDay(): LocalDate = LocalDate.of(start.year, start.month + 1, start.date)

  private fun level() {
    val tasks = collectLevelTasks(project.taskManager, monday)
    val result = levelTasks(tasks, monday, workingDayTest(project.taskManager.calendar),
      durationAtStart(project.taskManager), canStartOn = startAllowed(project.taskManager))
    applyLevelling(result.starts, result.durations, project.taskManager, undoManager, "level")
  }

  @Test
  fun `a day off of a blocking resource takes the whole day of the task`() {
    val anna = resource("anna")
    val bob = resource("bob")
    bob.dayOff(monday)
    val task = effortTask("24")
    task.assign(anna)
    task.assign(bob, blocking = true)
    // Monday nobody works, Tuesday 16 hours, Wednesday 8 hours.
    assertEquals(3, task.days())
  }

  @Test
  fun `a day off of a blocking resource with load zero does not take the day`() {
    val anna = resource("anna")
    val bob = resource("bob")
    bob.dayOff(monday)
    val task = effortTask("16")
    task.assign(anna)
    task.assign(bob, blocking = true, load = 0f)
    // Bob is not involved: Monday and Tuesday 8 hours each.
    assertEquals(2, task.days())
  }

  @Test
  fun `a day off of a blocking resource with some load takes the day`() {
    val anna = resource("anna")
    val bob = resource("bob")
    bob.dayOff(monday)
    val task = effortTask("16")
    task.assign(anna)
    task.assign(bob, blocking = true, load = 50f)
    // Monday nobody works, Tuesday and Wednesday 8 hours plus 4 hours.
    assertEquals(3, task.days())
  }

  @Test
  fun `a day off of a resource which is not blocking only takes its own hours`() {
    val anna = resource("anna")
    val bob = resource("bob")
    bob.dayOff(monday)
    val task = effortTask("24")
    task.assign(anna)
    task.assign(bob)
    // Monday 8 hours, Tuesday 16 hours.
    assertEquals(2, task.days())
  }

  @Test
  fun `a blocking resource which brings no effort still takes the day`() {
    val anna = resource("anna")
    val reviewer = resource("reviewer")
    reviewer.dayOff(monday.plusDays(1))
    val task = effortTask("16")
    task.assign(anna)
    task.assign(reviewer, blocking = true, noEffort = true)
    // Monday 8 hours, Tuesday nobody, Wednesday 8 hours.
    assertEquals(3, task.days())
  }

  @Test
  fun `the same reviewer without the blocking flag changes nothing`() {
    val anna = resource("anna")
    val reviewer = resource("reviewer")
    reviewer.dayOff(monday.plusDays(1))
    val task = effortTask("16")
    task.assign(anna)
    task.assign(reviewer, noEffort = true)
    assertEquals(2, task.days())
  }

  @Test
  fun `a blocking day off on a non working day changes nothing`() {
    val anna = resource("anna")
    anna.dayOff(monday.plusDays(5))
    val task = effortTask("40")
    task.assign(anna, blocking = true)
    assertEquals(5, task.days())
  }

  @Test
  fun `the duration follows when the blocking flag is set`() {
    val anna = resource("anna")
    anna.dayOff(monday.plusDays(1))
    val task = effortTask("24")
    task.assign(anna)
    assertEquals(4, task.days())
    // The day off already costs anna's hours; blocking makes it cost the others' as well.
    val bob = resource("bob")
    task.assign(bob, blocking = true)
    assertEquals(2, task.days())
    bob.dayOff(monday)
    assertEquals(3, task.days())
  }

  @Test
  fun `levelling starts a task when its blocking resource is back`() {
    val anna = resource("anna")
    anna.dayOff(monday, monday.plusDays(1))
    val task = effortTask("16")
    task.assign(anna, blocking = true)
    level()
    assertEquals(monday.plusDays(2), task.startDay())
    assertEquals(2, task.days())
  }

  @Test
  fun `levelling does not hold a task back for a day off of a resource which is not blocking`() {
    val anna = resource("anna")
    anna.dayOff(monday, monday.plusDays(1))
    val task = effortTask("16")
    task.assign(anna)
    level()
    assertEquals(monday, task.startDay())
    assertEquals(4, task.days())
  }

  @Test
  fun `levelling waits for every blocking resource but not for the others`() {
    val anna = resource("anna")
    val bob = resource("bob")
    val carl = resource("carl")
    anna.dayOff(monday)
    bob.dayOff(monday, monday.plusDays(1))
    carl.dayOff(monday, monday.plusDays(2))
    val task = effortTask("16")
    task.assign(anna, blocking = true)
    task.assign(bob, blocking = true)
    task.assign(carl)
    level()
    assertEquals(monday.plusDays(2), task.startDay())
  }

  @Test
  fun `levelling does not hold a task back for a blocking resource with load zero`() {
    val anna = resource("anna")
    val bob = resource("bob")
    bob.dayOff(monday)
    val task = effortTask("16")
    task.assign(anna)
    task.assign(bob, blocking = true, load = 0f)
    level()
    assertEquals(monday, task.startDay())
  }
}
