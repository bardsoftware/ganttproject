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

import biz.ganttproject.core.model.task.LevelConflict
import biz.ganttproject.core.model.task.LevelTask
import biz.ganttproject.core.model.task.SHARED_POOL
import biz.ganttproject.core.model.task.endAfterWorkingDays
import biz.ganttproject.core.model.task.levelTasks
import biz.ganttproject.core.model.task.pools
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.ThrowingSupplier
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate

/** Resource levelling as a calculation, without a project. */
class ResourceLevellingTest {
  private val monday = LocalDate.of(2026, 11, 2)
  private val weekdays: (LocalDate) -> Boolean = { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }

  private fun task(
    id: String, days: Int, load: Int = 100, priority: Int = 2, order: Int = 0,
    predecessors: List<String> = emptyList(), fixed: LocalDate? = null, earliest: LocalDate? = null,
    loads: Map<String, Int> = mapOf("r" to load), frozen: Boolean = false, deadline: LocalDate? = null
  ) = LevelTask(id = id, orderInPlan = order, priority = priority, durationDays = days, predecessors = predecessors,
    fixedStart = fixed, earliestStart = earliest, loads = loads, frozen = frozen, deadline = deadline)

  @Test
  fun `two full tasks of one resource run one after the other`() {
    val result = levelTasks(listOf(task("a", 3), task("b", 2, order = 1)), monday, weekdays)
    assertEquals(monday, result.starts["a"])
    assertEquals(monday.plusDays(3), result.starts["b"])
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun `two half tasks of one resource run at the same time`() {
    val result = levelTasks(listOf(task("a", 3, load = 50), task("b", 3, load = 50, order = 1)), monday, weekdays)
    assertEquals(monday, result.starts["a"])
    assertEquals(monday, result.starts["b"])
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun `a third half task waits until there is room`() {
    val result = levelTasks(
      listOf(task("a", 2, load = 50), task("b", 4, load = 50, order = 1), task("c", 1, load = 50, order = 2)),
      monday, weekdays)
    assertEquals(monday, result.starts["b"])
    assertEquals(monday.plusDays(2), result.starts["c"])
  }

  @Test
  fun `higher priority goes first and equal priority follows the plan`() {
    val byPriority = levelTasks(listOf(task("low", 2, priority = 1), task("high", 2, priority = 4, order = 1)), monday, weekdays)
    assertEquals(monday, byPriority.starts["high"])
    assertEquals(monday.plusDays(2), byPriority.starts["low"])

    val byOrder = levelTasks(listOf(task("second", 2, order = 5), task("first", 2, order = 1)), monday, weekdays)
    assertEquals(monday, byOrder.starts["first"])
    assertEquals(monday.plusDays(2), byOrder.starts["second"])
  }

  @Test
  fun `a predecessor is respected even with free capacity`() {
    val result = levelTasks(
      listOf(task("first", 2, load = 10), task("then", 1, load = 10, order = 1, predecessors = listOf("first"))),
      monday, weekdays)
    assertEquals(monday.plusDays(2), result.starts["then"])
  }

  @Test
  fun `the weekend is skipped`() {
    val thursday = monday.plusDays(3)
    val result = levelTasks(
      listOf(task("a", 3, earliest = thursday), task("b", 1, order = 1, predecessors = listOf("a"))), monday, weekdays)
    assertEquals(thursday, result.starts["a"])
    // a takes Thursday, Friday and Monday
    assertEquals(monday.plusDays(8), result.starts["b"])
  }

  @Test
  fun `the earliest start is a lower bound only`() {
    val result = levelTasks(listOf(task("blocker", 5), task("late", 1, order = 1, earliest = monday.plusDays(2))), monday, weekdays)
    assertEquals(monday.plusDays(7), result.starts["late"])
  }

  @Test
  fun `a fixed date stays and the overload is reported`() {
    val result = levelTasks(listOf(task("running", 5), task("fixed", 1, order = 1, fixed = monday)), monday, weekdays)
    assertEquals(monday, result.starts["fixed"])
    val overload = result.conflicts.filterIsInstance<LevelConflict.Overload>()
    assertEquals(listOf(LevelConflict.Overload(monday, 200, listOf("running", "fixed"), "r")), overload)
  }

  @Test
  fun `a fixed date before its predecessor ends is kept and reported`() {
    val result = levelTasks(
      listOf(task("first", 5), task("fixed", 1, order = 1, fixed = monday, predecessors = listOf("first"))),
      monday, weekdays)
    assertEquals(monday, result.starts["fixed"])
    assertEquals(
      listOf(LevelConflict.FixedDateNotReachable("fixed", monday, monday.plusDays(7))),
      result.conflicts.filterIsInstance<LevelConflict.FixedDateNotReachable>())
  }

  @Test
  fun `a fixed date which fits reports nothing`() {
    val result = levelTasks(listOf(task("fixed", 2, fixed = monday)), monday, weekdays)
    assertEquals(monday, result.starts["fixed"])
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun `frozen work keeps its days and its capacity`() {
    val result = levelTasks(
      listOf(task("movable", 2), task("started", 3, order = 1, fixed = monday, frozen = true)), monday, weekdays)
    assertEquals(monday, result.starts["started"])
    assertEquals(monday.plusDays(3), result.starts["movable"])
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun `a cycle is reported and nothing is placed`() {
    val result = levelTasks(
      listOf(task("a", 1, predecessors = listOf("b")), task("b", 1, predecessors = listOf("a"))), monday, weekdays)
    assertTrue(result.starts.isEmpty())
    assertEquals(listOf(LevelConflict.Cycle(listOf("a", "b"))), result.conflicts)
  }

  @Test
  fun `an empty plan is no error`() {
    val result = levelTasks(emptyList(), monday, weekdays)
    assertTrue(result.starts.isEmpty())
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun `tasks of different resources run at the same time`() {
    val result = levelTasks(
      listOf(task("a", 3, loads = mapOf("anna" to 100)), task("b", 3, order = 1, loads = mapOf("bob" to 100))),
      monday, weekdays)
    assertEquals(monday, result.starts["a"])
    assertEquals(monday, result.starts["b"])
  }

  @Test
  fun `each resource books its own load, not the sum of the task`() {
    // Two resources at 50 % each on one task, and another task of anna at 50 %: anna is at 100 %, nobody is over.
    val result = levelTasks(
      listOf(task("pair", 3, loads = mapOf("anna" to 50, "bob" to 50)), task("anna", 3, order = 1, loads = mapOf("anna" to 50))),
      monday, weekdays)
    assertEquals(monday, result.starts["anna"])
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun `an overload of one of several resources is reported with its own load`() {
    val result = levelTasks(
      listOf(
        task("pair", 1, fixed = monday, loads = mapOf("anna" to 50, "bob" to 50)),
        task("anna", 1, order = 1, fixed = monday, loads = mapOf("anna" to 100))),
      monday, weekdays)
    assertEquals(
      listOf(LevelConflict.Overload(monday, 150, listOf("pair", "anna"), "anna")),
      result.conflicts.filterIsInstance<LevelConflict.Overload>())
  }

  @Test
  fun `tasks without resources share one pool`() {
    val result = levelTasks(
      listOf(task("a", 2, loads = emptyMap()), task("b", 2, order = 1, loads = emptyMap()), task("c", 2, order = 2)),
      monday, weekdays)
    assertEquals(monday, result.starts["a"])
    assertEquals(monday.plusDays(2), result.starts["b"])
    assertEquals(monday, result.starts["c"])
  }

  @Test
  fun `an assignment with load zero takes nothing`() {
    val result = levelTasks(
      listOf(task("watch", 3, loads = mapOf("anna" to 0)), task("work", 3, order = 1, loads = mapOf("anna" to 100))),
      monday, weekdays)
    assertEquals(monday, result.starts["work"])
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun `an assignment with load zero is not held back by an overloaded day`() {
    val result = levelTasks(
      listOf(task("a", 1, fixed = monday), task("b", 1, order = 1, fixed = monday), task("watch", 1, order = 2, load = 0)),
      monday, weekdays)
    assertEquals(monday, result.starts["watch"])
  }

  @Test
  fun `a missed deadline is reported with the missing working days`() {
    // b waits for a and ends on Friday the next week, three working days after its deadline on Tuesday.
    val result = levelTasks(
      listOf(task("a", 5), task("b", 5, order = 1, deadline = monday.plusDays(8))), monday, weekdays)
    assertEquals(
      listOf(LevelConflict.DeadlineMissed("b", monday.plusDays(8), monday.plusDays(11), 3)),
      result.conflicts)
  }

  @Test
  fun `a deadline which is met reports nothing`() {
    val result = levelTasks(listOf(task("a", 5, deadline = monday.plusDays(4))), monday, weekdays)
    assertTrue(result.conflicts.isEmpty())
  }

  @Test
  fun `the duration is asked for the start day of every candidate`() {
    // b needs 2 days when it starts in the first week and 4 days from the second week on.
    val durationAt: (LevelTask, LocalDate) -> Int = { t, start -> if (t.id == "b" && start >= monday.plusDays(7)) 4 else t.durationDays }
    val result = levelTasks(listOf(task("a", 5), task("b", 2, order = 1)), monday, weekdays, durationAt)
    assertEquals(monday.plusDays(7), result.starts["b"])
    assertEquals(4, result.durations["b"])
    assertEquals(5, result.durations["a"])
  }

  @Test
  fun `a task which needs more than the capacity on its own still gets a window`() {
    val result = levelTasks(listOf(task("a", 2), task("b", 2, order = 1)), monday, weekdays, capacityOf = { 80 })
    assertEquals(monday, result.starts["a"])
    assertEquals(monday.plusDays(2), result.starts["b"])
  }

  @Test
  fun `a capacity of zero does not hang and places tasks one after the other`() {
    val result = assertTimeoutPreemptively(Duration.ofSeconds(30), ThrowingSupplier {
      levelTasks(listOf(task("a", 2), task("b", 2, order = 1)), monday, weekdays, capacityOf = { 0 })
    })
    assertEquals(monday, result.starts["a"])
    assertEquals(monday.plusDays(2), result.starts["b"])
  }

  @Test
  fun `a calendar without working days does not hang`() {
    val result = assertTimeoutPreemptively(Duration.ofSeconds(30), ThrowingSupplier {
      levelTasks(listOf(task("a", 2, deadline = monday), task("b", 2, order = 1)), monday, { false })
    })
    assertEquals(setOf("a", "b"), result.starts.keys)
  }

  @Test
  fun `the end is the day after the last working day of the task`() {
    // Thursday, three days: Thursday, Friday and Monday, the end is Tuesday.
    assertEquals(monday.plusDays(8), endAfterWorkingDays(monday.plusDays(3), 3, weekdays))
    // Thursday, two days: Thursday and Friday, the end is Saturday and not Monday.
    assertEquals(monday.plusDays(5), endAfterWorkingDays(monday.plusDays(3), 2, weekdays))
    assertEquals(listOf(SHARED_POOL), task("x", 1, loads = emptyMap()).pools)
  }
}
