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

import biz.ganttproject.core.model.task.CapacityChange
import biz.ganttproject.core.model.task.SHARED_POOL
import biz.ganttproject.core.model.task.levelTasks
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.applyLevelling
import net.sourceforge.ganttproject.task.algorithm.capacityProblems
import net.sourceforge.ganttproject.task.algorithm.collectLevelTasks
import net.sourceforge.ganttproject.task.algorithm.durationAtStart
import net.sourceforge.ganttproject.task.algorithm.workingDayTest
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/** Levelling of the tasks of a project: what is taken from the model and what is written back. */
class TaskLevellingTest {
  private lateinit var project: GanttProjectImpl
  private val monday = LocalDate.of(2026, 11, 2)
  private var edits = 0

  // Runs every edit at once and counts them.
  private val undoManager = object : GPUndoManager {
    override fun undoableEdit(localizedName: String, runnableEdit: Runnable) {
      edits++
      runnableEdit.run()
    }
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

  private fun resource(name: String, hoursPerDay: String = "8"): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal(hoursPerDay)
    }

  private fun task(name: String, days: Int, vararg assignments: Pair<HumanResource, Float>): Task =
    project.taskManager.newTaskBuilder().withName(name)
      .withStartDate(CalendarFactory.createGanttCalendar(2026, 10, 2).time)
      .withDuration(project.taskManager.createLength(days.toLong())).build().also { task ->
        assignments.forEach { (resource, load) -> task.assignmentCollection.addAssignment(resource).load = load }
      }

  private fun effortTask(name: String, effort: String, resource: HumanResource): Task =
    task(name, 1, resource to 100f).also { task ->
      task.createMutator().also {
        it.setEffort(BigDecimal(effort))
        it.setEffortDriven(true)
        it.commit()
      }
    }

  private fun level(): Int {
    val tasks = collectLevelTasks(project.taskManager, monday)
    val result = levelTasks(tasks, monday, workingDayTest(project.taskManager.calendar), durationAtStart(project.taskManager))
    return applyLevelling(result.starts, result.durations, project.taskManager, undoManager, "level")
  }

  private fun Task.startDay(): LocalDate = start.day()

  private fun GanttCalendar.day(): LocalDate = LocalDate.of(year, month + 1, date)

  @Test
  fun `an assignment with load zero takes nothing`() {
    val anna = resource("anna")
    val watch = task("watch", 3, anna to 0f)
    val levelTask = collectLevelTasks(project.taskManager, monday).single { it.id == watch.taskID.toString() }
    assertEquals(mapOf(anna.id.toString() to 0), levelTask.loads)
  }

  @Test
  fun `each assignment books its own load and a task without resources the shared pool`() {
    val anna = resource("anna")
    val bob = resource("bob")
    val pair = task("pair", 3, anna to 50f, bob to 50f)
    val nobody = task("nobody", 1)
    val tasks = collectLevelTasks(project.taskManager, monday).associateBy { it.id }
    assertEquals(mapOf(anna.id.toString() to 50, bob.id.toString() to 50), tasks.getValue(pair.taskID.toString()).loads)
    assertEquals(mapOf(SHARED_POOL to 100), tasks.getValue(nobody.taskID.toString()).loads)
  }

  @Test
  fun `levelling moves the second task of a resource after the first in one edit`() {
    val anna = resource("anna")
    val first = task("first", 3, anna to 100f)
    val second = task("second", 2, anna to 100f)
    assertEquals(1, level())
    assertEquals(1, edits)
    assertEquals(monday, first.startDay())
    assertEquals(monday.plusDays(3), second.startDay())
    assertEquals(2, second.duration.length)
    assertEquals(monday.plusDays(3), second.third.let { LocalDate.of(it.year, it.month + 1, it.date) })
  }

  @Test
  fun `levelling which moves nothing adds no edit`() {
    val anna = resource("anna")
    task("first", 3, anna to 50f)
    task("second", 2, anna to 50f)
    assertEquals(0, level())
    assertEquals(0, edits)
  }

  @Test
  fun `a fixed date and a waiting task stay where they are`() {
    val anna = resource("anna")
    task("first", 3, anna to 100f)
    val fixed = task("fixed", 1, anna to 100f).also { t -> t.createMutator().also { it.setDateFixed(true); it.commit() } }
    val waiting = task("waiting", 2, anna to 100f).also { t -> t.createMutator().also { it.setWaitOnly(true); it.commit() } }
    level()
    assertEquals(monday, fixed.startDay())
    assertEquals(monday, waiting.startDay())
  }

  @Test
  fun `a moved milestone ends where it starts and its supertask encloses it`() {
    val anna = resource("anna")
    task("first", 3, anna to 100f)
    val phase = task("phase", 1)
    val second = project.taskManager.newTaskBuilder().withName("second").withParent(phase)
      .withStartDate(CalendarFactory.createGanttCalendar(2026, 10, 2).time)
      .withDuration(project.taskManager.createLength(2)).build().also { it.assignmentCollection.addAssignment(anna).load = 100f }
    val done = project.taskManager.newTaskBuilder().withName("done").withParent(phase)
      .withStartDate(CalendarFactory.createGanttCalendar(2026, 10, 4).time).build().also { it.isMilestone = true }
    project.taskManager.dependencyCollection.createDependency(done, second)
    val before = done.startDay()
    // Reading the end keeps it in the task, as the chart and the table do.
    assertEquals(done.start, done.end)
    level()
    assertTrue(done.startDay() > before)
    assertEquals(done.start, done.end)
    assertTrue(phase.start <= second.start) { "phase ${phase.start} starts after second ${second.start}" }
    // The scheduler ends a supertask on Saturday rather than on the Monday after, so no working day may lie between.
    val isWorkingDay = workingDayTest(project.taskManager.calendar)
    val gap = generateSequence(phase.end.day()) { it.plusDays(1) }.takeWhile { it < done.end.day() }
    assertTrue(gap.none(isWorkingDay)) { "phase ${phase.end} ends before done ${done.end}" }
  }

  @Test
  fun `a milestone in the past which started work depends on stays and so does the work`() {
    // As in HouseBuildingSample: a supertask with finished work depends on a milestone which is not marked as done.
    val past = CalendarFactory.createGanttCalendar(2026, 9, 26).time
    val release = project.taskManager.newTaskBuilder().withName("release").withStartDate(past).build()
      .also { it.isMilestone = true }
    val design = task("design", 1)
    val draft = project.taskManager.newTaskBuilder().withName("draft").withParent(design).withStartDate(past)
      .withDuration(project.taskManager.createLength(3)).build()
      .also { t -> t.createMutator().also { it.setCompletionPercentage(100); it.commit() } }
    project.taskManager.dependencyCollection.createDependency(design, release)
    val levelTasks = collectLevelTasks(project.taskManager, monday).associateBy { it.id }
    assertTrue(levelTasks.getValue(release.taskID.toString()).frozen)
    level()
    assertEquals(LocalDate.of(2026, 10, 26), release.startDay())
    assertEquals(release.start, release.end)
    assertEquals(LocalDate.of(2026, 10, 26), draft.startDay())
  }

  @Test
  fun `a levelled task ends where a new task with the same days ends`() {
    val anna = resource("anna")
    task("first", 3, anna to 100f)
    val second = task("second", 2, anna to 100f)
    level()
    // Thursday and Friday: the end is Saturday, so that Friday is shown as the last day, not Sunday.
    val fresh = project.taskManager.newTaskBuilder().withName("fresh").withStartDate(second.start.time)
      .withDuration(project.taskManager.createLength(2)).build()
    assertEquals(monday.plusDays(3), second.startDay())
    assertEquals(fresh.end, second.end)
    assertEquals(fresh.displayEnd, second.displayEnd)
    assertEquals(2, second.duration.length)
  }

  @Test
  fun `an effort driven task gets the duration of the hours where it is moved to`() {
    // anna has 8 hours a day until Wednesday and 4 from Thursday on.
    val anna = resource("anna")
    anna.hoursSchedule = listOf(CapacityChange(monday.plusDays(3), BigDecimal(4)))
    task("first", 3, anna to 100f)
    val second = effortTask("second", "16", anna)
    assertEquals(2, second.duration.length)
    val levelTask = collectLevelTasks(project.taskManager, monday).single { it.id == second.taskID.toString() }
    assertEquals(4, durationAtStart(project.taskManager)(levelTask, monday.plusDays(3)))
    level()
    assertEquals(monday.plusDays(3), second.startDay())
    assertEquals(4, second.duration.length)
  }

  @Test
  fun `an effort driven task whose resource has no hours is a capacity problem`() {
    val anna = resource("anna")
    anna.utilization = 0
    val task = effortTask("t", "16", anna)
    assertEquals(listOf(task), capacityProblems(project.taskManager))
    anna.utilization = 50
    assertTrue(capacityProblems(project.taskManager).isEmpty())
  }
}
