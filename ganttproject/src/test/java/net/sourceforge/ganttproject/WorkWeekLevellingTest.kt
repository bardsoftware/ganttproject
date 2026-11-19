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
import biz.ganttproject.core.model.task.LevelResult
import biz.ganttproject.core.model.task.WorkWeekChange
import biz.ganttproject.core.model.task.levelTasks
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import biz.ganttproject.task.recurrence.toModelDate
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.WorkWeekWorkingDays
import net.sourceforge.ganttproject.task.algorithm.applyLevelling
import net.sourceforge.ganttproject.task.algorithm.collectLevelTasks
import net.sourceforge.ganttproject.task.algorithm.durationAtStart
import net.sourceforge.ganttproject.task.algorithm.resourcePresence
import net.sourceforge.ganttproject.task.algorithm.startAllowed
import net.sourceforge.ganttproject.task.algorithm.workingDaysOf
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.ThrowingSupplier
import java.math.BigDecimal
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

/**
 * Levelling with a working week per resource, wired as the levelling action does it: the window of a task is searched,
 * its capacity booked and its end written back on the working days of that task. The week starts on Monday
 * 2026-11-02; resources work 8 hours a day.
 */
class WorkWeekLevellingTest {
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

  private fun resource(name: String, vararg days: DayOfWeek): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal(8)
      it.workWeek = listOf(WorkWeekChange(null, days.toSet()))
    }

  private fun effortTask(effort: Int, resource: HumanResource, blocking: HumanResource? = null): Task =
    project.taskManager.newTaskBuilder().withName("t").withStartDate(monday.toModelDate())
      .withDuration(project.taskManager.createLength(1)).build().also { task ->
        task.assignmentCollection.addAssignment(resource).load = 100f
        blocking?.let { task.assignmentCollection.addAssignment(it).also { a -> a.load = 100f; a.isBlocking = true } }
        task.createMutator().also {
          it.setEffort(BigDecimal(effort))
          it.setEffortDriven(true)
          it.commit()
        }
      }

  private val workingDays get() = WorkWeekWorkingDays(project.taskManager.calendar)

  private fun level(): LevelResult {
    val workingDays = workingDays
    val tasks = collectLevelTasks(project.taskManager, monday, workingDays = workingDays)
    val worksOn = workingDaysOf(project.taskManager, workingDays)
    return levelTasks(tasks, monday, workingDays.projectOnly, durationAtStart(project.taskManager, worksOn),
      canStartOn = startAllowed(project.taskManager), isPresent = resourcePresence(project.taskManager),
      workingDaysOf = worksOn)
  }

  private val Task.id get() = taskID.toString()

  private fun Task.startDay(): LocalDate = LocalDate.of(start.year, start.month + 1, start.date)

  private fun Task.endDay(): LocalDate = LocalDate.of(end.year, end.month + 1, end.date)

  @Test
  fun `levelling books the capacity of a task on the days its duration is counted on`() {
    val anna = resource("anna", MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)
    val first = effortTask(48, anna)
    val second = effortTask(8, anna)
    val result = level()
    assertEquals(6, result.durations[first.id])
    assertEquals(monday, result.starts[first.id])
    // Anna names Saturday, but the project leaves it free: her six days are Monday to Friday and the Monday after.
    val end = workingDays.endAfter(monday, 6, workingDays.forTask(first))
    assertEquals(monday.plusDays(8), end)
    assertEquals(end, result.starts[second.id])
    applyLevelling(result.starts, result.durations, project.taskManager, undoManager, "level")
    assertEquals(end, second.startDay())
    // The task model counts the same project working days, so bar and calculation agree.
    assertEquals(6, first.duration.length)
  }

  @Test
  fun `two resources with different work weeks keep their own days in one run`() {
    val anna = resource("anna", MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)
    val otto = resource("otto", MONDAY, TUESDAY, THURSDAY, FRIDAY)
    // Anna's tasks come first, so that her days are the ones asked first.
    val annaFirst = effortTask(48, anna)
    val annaSecond = effortTask(8, anna)
    val ottoFirst = effortTask(24, otto)
    val ottoSecond = effortTask(8, otto)
    val result = level()
    assertEquals(6, result.durations[annaFirst.id])
    assertEquals(3, result.durations[ottoFirst.id])
    assertEquals(monday.plusDays(8), result.starts[annaSecond.id])
    // Otto does not work on Wednesday: Monday, Tuesday and Thursday, so his next task starts on Friday.
    assertEquals(monday.plusDays(4), result.starts[ottoSecond.id])
    assertEquals(workingDays.endAfter(monday, 3, workingDays.forTask(ottoFirst)), result.starts[ottoSecond.id])
  }

  @Test
  fun `levelling stops at the limit of the search when a blocking resource with a work week is never present`() {
    val anna = resource("anna", MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)
    val bob = resource("bob", MONDAY, TUESDAY, THURSDAY)
    // Long enough that the days of the task, Monday, Tuesday and Thursday, reach beyond the limit of the search.
    bob.addDaysOff(GanttDaysOff(monday.toModelDate(), monday.plusYears(400).toModelDate()))
    val task = effortTask(8, anna, blocking = bob)
    val result = assertTimeoutPreemptively(Duration.ofSeconds(20), ThrowingSupplier { level() })
    // The task stays on its earliest day instead of being moved four hundred years ahead.
    assertEquals(monday, result.starts[task.id])
  }
}
