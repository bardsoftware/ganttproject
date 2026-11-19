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
import biz.ganttproject.core.model.task.LevelConflict
import biz.ganttproject.core.model.task.LevelResult
import biz.ganttproject.core.model.task.LevelTask
import biz.ganttproject.core.model.task.SHARED_POOL
import biz.ganttproject.core.model.task.levelTasks
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.applyLevelling
import net.sourceforge.ganttproject.task.algorithm.collectLevelTasks
import net.sourceforge.ganttproject.task.algorithm.durationAtStart
import net.sourceforge.ganttproject.task.algorithm.resourcePresence
import net.sourceforge.ganttproject.task.algorithm.startAllowed
import net.sourceforge.ganttproject.task.algorithm.workingDayTest
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * The blocking flag of an assignment in levelling: a task is placed only where all of its blocking resources are
 * present, and a task for which the search finds no window is reported with its reasons.
 */
class LevellingBlockingTest {
  private val monday = LocalDate.of(2026, 11, 2)

  /** The calculation alone, without a project. */
  @Nested
  inner class Calculation {
    private val weekdays: (LocalDate) -> Boolean = { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }
    private val everyDay: (LocalDate) -> Boolean = { true }

    private fun task(
      id: String, days: Int, loads: Map<String, Int> = mapOf("r" to 100), blocking: Set<String> = emptySet(),
      fixed: LocalDate? = null, frozen: Boolean = false, order: Int = 0
    ) = LevelTask(id = id, orderInPlan = order, priority = 2, durationDays = days, fixedStart = fixed, loads = loads,
      frozen = frozen, blocking = blocking)

    private fun absentOn(vararg days: LocalDate): (String, LocalDate) -> Boolean = { _, day -> day !in days }

    @Test
    fun `a task waits until its blocking resource is back`() {
      val absent = absentOn(monday, monday.plusDays(1))
      val result = levelTasks(listOf(task("a", 2, blocking = setOf("bob"))), monday, weekdays, isPresent = absent)
      assertEquals(monday.plusDays(2), result.starts["a"])
      assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `an absence inside the window moves the task behind it`() {
      val absent = absentOn(monday.plusDays(2))
      val result = levelTasks(listOf(task("a", 3, blocking = setOf("bob"))), monday, weekdays, isPresent = absent)
      assertEquals(monday.plusDays(3), result.starts["a"])
    }

    @Test
    fun `a blocking resource without load blocks as well`() {
      val absent = absentOn(monday)
      val result = levelTasks(listOf(task("a", 1, loads = mapOf("r" to 100, "bob" to 0), blocking = setOf("bob"))),
        monday, weekdays, isPresent = absent)
      assertEquals(monday.plusDays(1), result.starts["a"])
    }

    @Test
    fun `several blocking resources leave only the days on which all of them are present`() {
      val isPresent: (String, LocalDate) -> Boolean = { id, day ->
        !(id == "anna" && day == monday) && !(id == "bob" && day == monday.plusDays(2))
      }
      // Monday anna is away and Wednesday bob: Monday-Tuesday and Tuesday-Wednesday do not fit, Thursday-Friday does.
      val result = levelTasks(listOf(task("a", 2, blocking = setOf("anna", "bob"))), monday, weekdays,
        isPresent = isPresent)
      assertEquals(monday.plusDays(3), result.starts["a"])
    }

    @Test
    fun `without blocking resources the presence is not asked`() {
      val tasks = listOf(task("a", 3), task("b", 2, order = 1), task("c", 1, loads = emptyMap(), order = 2))
      val asked = mutableListOf<String>()
      val result = levelTasks(tasks, monday, weekdays, isPresent = { id, _ -> asked.add(id); false })
      assertEquals(levelTasks(tasks, monday, weekdays), result)
      assertTrue(asked.isEmpty())
    }

    @Test
    fun `a fixed date and frozen work stay on a day of absence`() {
      val absent = absentOn(monday, monday.plusDays(1))
      val result = levelTasks(listOf(
        task("fixed", 1, loads = mapOf("x" to 100), blocking = setOf("bob"), fixed = monday),
        task("frozen", 1, loads = mapOf("y" to 100), blocking = setOf("bob"), fixed = monday.plusDays(1), frozen = true, order = 1)
      ), monday, weekdays, isPresent = absent)
      assertEquals(monday, result.starts["fixed"])
      assertEquals(monday.plusDays(1), result.starts["frozen"])
      assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `a day of absence frees the capacity of the blocking resource for other tasks`() {
      val tasks = listOf(
        task("fixed", 2, loads = mapOf("bob" to 100), blocking = setOf("bob"), fixed = monday),
        task("other", 1, loads = mapOf("bob" to 100), order = 1)
      )
      val result = levelTasks(tasks, monday, weekdays, isPresent = absentOn(monday))
      // Bob is away on Monday, so the other task takes the day; Tuesday is booked by the fixed task.
      assertEquals(monday, result.starts["other"])
      assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `a blocked day frees the capacity of another, not blocking person on the task`() {
      val tasks = listOf(
        task("fixed", 2, loads = mapOf("bob" to 100, "anna" to 100), blocking = setOf("bob"), fixed = monday),
        task("other", 1, loads = mapOf("anna" to 100), order = 1)
      )
      // Bob is away on Monday and blocks the task, so nobody works on it: Anna is free for the other task that day.
      val result = levelTasks(tasks, monday, weekdays, isPresent = { who, day -> who != "bob" || day != monday })
      assertEquals(monday, result.starts["other"])
      assertTrue(result.conflicts.isEmpty())
      // Without the absence Anna is booked on Monday and Tuesday.
      assertEquals(monday.plusDays(2), levelTasks(tasks, monday, weekdays).starts["other"])
    }

    @Test
    fun `a day at home frees the capacity only for a task which needs somebody on site`() {
      fun onSite(required: Boolean) = LevelTask(id = "fixed", orderInPlan = 0, priority = 2, durationDays = 1, fixedStart = monday,
        loads = mapOf("bob" to 100), blocking = setOf("bob"), requiresPresence = required)
      val other = task("other", 1, loads = mapOf("bob" to 100), order = 1)
      val atHome: (String, LocalDate) -> Boolean = { _, day -> day != monday }
      assertEquals(monday, levelTasks(listOf(onSite(true), other), monday, weekdays, isAtWorkplace = atHome).starts["other"])
      assertEquals(monday.plusDays(1), levelTasks(listOf(onSite(false), other), monday, weekdays, isAtWorkplace = atHome).starts["other"])
    }

    @Test
    fun `without a blocked day the capacity is booked as before`() {
      val tasks = listOf(
        task("fixed", 2, loads = mapOf("bob" to 100), blocking = setOf("bob"), fixed = monday),
        task("other", 1, loads = mapOf("bob" to 100), order = 1)
      )
      val result = levelTasks(tasks, monday, weekdays, isPresent = { _, _ -> true })
      assertEquals(monday.plusDays(2), result.starts["other"])
      assertEquals(levelTasks(tasks, monday, weekdays), result)
    }

    @Test
    fun `two blocking resources which are never present together are reported`() {
      // anna is present on even days and bob on odd days.
      val isPresent: (String, LocalDate) -> Boolean = { id, day -> (day.toEpochDay() % 2 == 0L) == (id == "anna") }
      val result = levelTasks(listOf(task("a", 1, blocking = setOf("bob", "anna"))), monday, everyDay,
        isPresent = isPresent)
      assertEquals(listOf(LevelConflict.NoPossibleDate("a", listOf("anna", "bob"), emptyList())), result.conflicts)
      // The task stays on its earliest day.
      assertEquals(monday, result.starts["a"])
    }

    @Test
    fun `a task which finds no day with room is reported with the full resource`() {
      val result = levelTasks(listOf(
        task("frozen", 60_000, fixed = monday, frozen = true),
        task("a", 1, order = 1)
      ), monday, everyDay)
      assertEquals(listOf(LevelConflict.NoPossibleDate("a", emptyList(), listOf("r"))),
        result.conflicts.filterIsInstance<LevelConflict.NoPossibleDate>())
      assertEquals(monday, result.starts["a"])
    }

    @Test
    fun `the shared pool is named when a task without resources finds no room`() {
      val result = levelTasks(listOf(
        task("frozen", 60_000, loads = emptyMap(), fixed = monday, frozen = true),
        task("a", 1, loads = emptyMap(), order = 1)
      ), monday, everyDay)
      assertEquals(listOf(LevelConflict.NoPossibleDate("a", emptyList(), listOf(SHARED_POOL))),
        result.conflicts.filterIsInstance<LevelConflict.NoPossibleDate>())
    }

    @Test
    fun `a blocking resource which was always present is not named as a reason`() {
      val result = levelTasks(listOf(
        task("frozen", 60_000, fixed = monday, frozen = true),
        task("a", 1, blocking = setOf("bob"), order = 1)
      ), monday, everyDay)
      assertEquals(listOf(LevelConflict.NoPossibleDate("a", emptyList(), listOf("r"))),
        result.conflicts.filterIsInstance<LevelConflict.NoPossibleDate>())
    }

    @Test
    fun `a task which fails for both reasons is reported once with both`() {
      // bob is present on even days only, and r is full on every day.
      val isPresent: (String, LocalDate) -> Boolean = { _, day -> day.toEpochDay() % 2 == 0L }
      val result = levelTasks(listOf(
        task("frozen", 60_000, fixed = monday, frozen = true),
        task("a", 1, blocking = setOf("bob"), order = 1)
      ), monday, everyDay, isPresent = isPresent)
      assertEquals(listOf(LevelConflict.NoPossibleDate("a", listOf("bob"), listOf("r"))),
        result.conflicts.filterIsInstance<LevelConflict.NoPossibleDate>())
    }

    @Test
    fun `a turned down start day counts as absence of a blocking resource which is away that day`() {
      val isPresent: (String, LocalDate) -> Boolean = { _, _ -> false }
      val result = levelTasks(listOf(task("a", 1, blocking = setOf("bob"))), monday, everyDay,
        canStartOn = { _, day -> isPresent("bob", day) }, isPresent = isPresent)
      assertEquals(listOf(LevelConflict.NoPossibleDate("a", listOf("bob"), emptyList())), result.conflicts)
      assertEquals(monday, result.starts["a"])
    }

    @Test
    fun `a start turned down while the blocking resource is present is not blamed on its absence`() {
      val result = levelTasks(listOf(task("a", 1, blocking = setOf("bob"))), monday, everyDay,
        canStartOn = { _, _ -> false })
      assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `turned down days are not reported when a window is found`() {
      val absent = absentOn(monday)
      val result = levelTasks(listOf(task("first", 2), task("a", 1, blocking = setOf("bob"), order = 1)),
        monday, weekdays, isPresent = absent)
      assertEquals(monday.plusDays(2), result.starts["a"])
      assertTrue(result.conflicts.isEmpty())
    }
  }

  /** The task model: which assignments block, and when a resource is present. */
  @Nested
  inner class Project {
    private lateinit var project: GanttProjectImpl

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
        it.hoursPerDay = BigDecimal(8)
      }

    private fun date(day: LocalDate) = CalendarFactory.createGanttCalendar(day.year, day.monthValue - 1, day.dayOfMonth).time

    private fun HumanResource.dayOff(first: LocalDate, last: LocalDate = first) {
      addDaysOff(GanttDaysOff(date(first), date(last.plusDays(1))))
    }

    private fun plainTask(days: Long): Task = project.taskManager.newTaskBuilder().withName("plain")
      .withStartDate(date(monday)).withDuration(project.taskManager.createLength(days)).build()

    private fun effortTask(effort: String): Task =
      project.taskManager.newTaskBuilder().withName("t")
        .withStartDate(date(monday)).withDuration(project.taskManager.createLength(1)).build().also { task ->
          task.createMutator().also {
            it.setEffort(BigDecimal(effort))
            it.setEffortDriven(true)
            it.commit()
          }
        }

    private fun Task.assign(resource: HumanResource, load: Float, blocking: Boolean = false, noEffort: Boolean = false) {
      assignmentCollection.addAssignment(resource).also {
        it.load = load
        it.isBlocking = blocking
        it.isNoEffort = noEffort
      }
    }

    private fun Task.startDay(): LocalDate = LocalDate.of(start.year, start.month + 1, start.date)

    private fun level(): LevelResult {
      val tasks = collectLevelTasks(project.taskManager, monday)
      val result = levelTasks(tasks, monday, workingDayTest(project.taskManager.calendar),
        durationAtStart(project.taskManager), canStartOn = startAllowed(project.taskManager),
        isPresent = resourcePresence(project.taskManager))
      applyLevelling(result.starts, result.durations, project.taskManager, undoManager, "level")
      return result
    }

    @Test
    fun `the blocking assignments of a task become its blocking resources, also with load zero`() {
      val anna = resource("anna")
      val bob = resource("bob")
      val carl = resource("carl")
      val task = plainTask(2)
      task.assign(anna, 100f)
      task.assign(bob, 0f, blocking = true)
      task.assign(carl, 50f, blocking = true)
      val levelTask = collectLevelTasks(project.taskManager, monday).single()
      assertEquals(setOf(bob.id.toString(), carl.id.toString()), levelTask.blocking)
    }

    @Test
    fun `a resource is absent on its days off and present on every other day`() {
      val anna = resource("anna")
      anna.dayOff(monday.plusDays(1), monday.plusDays(2))
      plainTask(1).assign(anna, 100f)
      val isPresent = resourcePresence(project.taskManager)
      val id = anna.id.toString()
      assertTrue(isPresent(id, monday))
      assertFalse(isPresent(id, monday.plusDays(1)))
      assertFalse(isPresent(id, monday.plusDays(2)))
      assertTrue(isPresent(id, monday.plusDays(3)))
      assertTrue(isPresent("4711", monday))
    }

    @Test
    fun `levelling moves a plain task behind a day off of its blocking resource`() {
      val anna = resource("anna")
      val bob = resource("bob")
      bob.dayOff(monday, monday.plusDays(1))
      val task = plainTask(2)
      task.assign(anna, 100f)
      task.assign(bob, 0f, blocking = true)
      level()
      assertEquals(monday.plusDays(2), task.startDay())
    }

    @Test
    fun `with both flags off levelling places the task as before`() {
      val anna = resource("anna")
      val bob = resource("bob")
      bob.dayOff(monday, monday.plusDays(1))
      val task = plainTask(2)
      task.assign(anna, 100f)
      task.assign(bob, 0f)
      level()
      assertEquals(monday, task.startDay())
    }

    @Test
    fun `a day off of a no effort assignment does not hold back the start of an effort driven task`() {
      val anna = resource("anna")
      val bob = resource("bob")
      bob.dayOff(monday)
      val task = effortTask("16")
      task.assign(anna, 100f)
      task.assign(bob, 100f, noEffort = true)
      val canStartOn = startAllowed(project.taskManager)
      assertTrue(canStartOn(collectLevelTasks(project.taskManager, monday).single(), monday))
      level()
      assertEquals(monday, task.startDay())
    }

    @Test
    fun `an effort driven task starts behind a day off of its blocking resource`() {
      val anna = resource("anna")
      val bob = resource("bob")
      bob.dayOff(monday, monday.plusDays(1))
      val task = effortTask("16")
      task.assign(anna, 100f)
      task.assign(bob, 100f, blocking = true)
      val result = level()
      assertEquals(monday.plusDays(2), task.startDay())
      assertTrue(result.conflicts.isEmpty())
    }

    @Test
    fun `an effort driven task whose blocking resource is never present is reported`() {
      val anna = resource("anna")
      val bob = resource("bob")
      bob.dayOff(monday, monday.plusYears(200))
      val task = effortTask("16")
      task.assign(anna, 100f)
      task.assign(bob, 100f, blocking = true)
      val result = level()
      assertEquals(listOf(LevelConflict.NoPossibleDate(task.taskID.toString(), listOf(bob.id.toString()), emptyList())),
        result.conflicts)
    }
  }
}
