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
package biz.ganttproject.task.recurrence

import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.model.task.RecurrenceOrigin
import biz.ganttproject.core.model.task.RecurrenceProblem
import biz.ganttproject.core.model.task.RecurrenceRule
import biz.ganttproject.core.model.task.RecurrenceUnit
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.LocalDate

class TestRecurrenceAdapter {
  private val builder = TestSetupHelper.newTaskManagerBuilder().withCalendar(WeekendCalendarImpl())
  private val taskManager: TaskManager = builder.build()

  /** Counts the edits and runs them at once. Everything else is not expected to be called. */
  private class RecordingUndoManager {
    val editNames = mutableListOf<String>()
    val manager: GPUndoManager = Proxy.newProxyInstance(
      GPUndoManager::class.java.classLoader, arrayOf(GPUndoManager::class.java)) { _, method, args ->
      check(method.name == "undoableEdit") { "unexpected call ${method.name}" }
      editNames.add(args[0] as String)
      (args[1] as Runnable).run()
      null
    } as GPUndoManager
  }

  private val undo = RecordingUndoManager()

  // 2026-11-09 is a Monday.
  private val monday = LocalDate.of(2026, 11, 9)

  private fun source(
    start: LocalDate = monday, rule: RecurrenceRule? = RecurrenceRule(RecurrenceUnit.WEEK, count = 4),
    effort: String? = "6"
  ): Task {
    val task = taskManager.newTaskBuilder().withName("VAT return").withStartDate(start.toModelDate()).build()
    task.createMutator().also {
      it.setRecurrenceRule(rule)
      it.setEffort(effort?.let(::BigDecimal))
      it.setCompletionPercentage(40)
      it.commit()
    }
    return task
  }

  private fun run(): Int = applyRecurrences(planRecurrences(taskManager), taskManager, undo.manager, "Create")

  private fun repeated(sourceTask: Task): List<Task> = taskManager.tasks
    .filter { it.recurrenceOrigin?.let { o -> o.sourceTaskId == sourceTask.taskID && !o.isGroup } == true }
    .sortedBy { it.recurrenceOrigin!!.date }

  @Test
  fun `repeated tasks are created for every date after the first`() {
    val task = source()
    assertEquals(3, run())
    assertEquals(
      listOf(monday.plusWeeks(1), monday.plusWeeks(2), monday.plusWeeks(3)),
      repeated(task).map { it.start.time.toModelLocalDate() })
    assertEquals(
      listOf(monday.plusWeeks(1), monday.plusWeeks(2), monday.plusWeeks(3)),
      repeated(task).map { it.recurrenceOrigin!!.date })
  }

  @Test
  fun `every repeated task has the effort of the source and no rule of its own`() {
    val task = source(effort = "6")
    run()
    repeated(task).forEach {
      assertEquals(BigDecimal("6"), it.effort)
      assertNull(it.recurrenceRule, "a repeated task must not repeat itself")
      assertEquals(task.name, it.name)
      assertEquals(0, it.completionPercentage)
    }
    assertEquals(BigDecimal("6"), task.effort, "the source task is not touched")
    assertEquals(task.recurrenceRule, RecurrenceRule(RecurrenceUnit.WEEK, count = 4))
  }

  @Test
  fun `a source without effort gives repeated tasks without effort`() {
    val task = source(effort = null)
    run()
    assertEquals(3, repeated(task).size)
    repeated(task).forEach { assertNull(it.effort) }
  }

  @Test
  fun `assignments of the source are copied`() {
    val resource = builder.resourceManager.newHumanResource().also {
      it.name = "Alice"
      builder.resourceManager.add(it)
    }
    val task = source()
    task.assignmentCollection.addAssignment(resource).load = 50f
    run()
    repeated(task).forEach {
      val assignment = it.assignments.single()
      assertSame(resource, assignment.resource)
      assertEquals(50f, assignment.load)
    }
  }

  @Test
  fun `a second run creates nothing`() {
    source()
    assertEquals(3, run())
    val before = taskManager.taskCount
    assertEquals(0, run())
    assertEquals(before, taskManager.taskCount)
    assertEquals(1, undo.editNames.size, "the second run had nothing to do and must not open an edit")
  }

  @Test
  fun `a longer rule adds the missing dates and keeps the existing ones`() {
    val task = source(rule = RecurrenceRule(RecurrenceUnit.WEEK, count = 2))
    run()
    val first = repeated(task).single()
    first.createMutator().also {
      it.setCompletionPercentage(100)
      it.commit()
    }
    task.createMutator().also {
      it.setRecurrenceRule(RecurrenceRule(RecurrenceUnit.WEEK, count = 4))
      it.commit()
    }
    assertEquals(2, run())
    assertEquals(3, repeated(task).size)
    assertSame(first, repeated(task).first(), "the date which existed is the same task")
    assertEquals(100, first.completionPercentage, "what the user did to a repeated task stays")
  }

  @Test
  fun `a date on a non working day moves to the next working day`() {
    // Saturday the 14th and Sunday the 15th both move to Monday the 16th, which counts once.
    val friday = LocalDate.of(2026, 11, 13)
    val task = source(start = friday, rule = RecurrenceRule(RecurrenceUnit.DAY, count = 3))
    run()
    assertEquals(
      listOf(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 17)),
      repeated(task).map { it.start.time.toModelLocalDate() })
  }

  @Test
  fun `all repeated tasks are created in one undoable edit`() {
    source()
    run()
    assertEquals(listOf("Create"), undo.editNames)
  }

  @Test
  fun `an end before the start is reported and nothing is created`() {
    val task = source(rule = RecurrenceRule(RecurrenceUnit.WEEK, until = monday.minusDays(1)))
    val other = source(rule = RecurrenceRule(RecurrenceUnit.WEEK, count = 3))
    val count = taskManager.taskCount
    val plan = planRecurrences(taskManager)
    assertEquals(listOf(RecurrenceProblemReport("VAT return", RecurrenceProblem.END_BEFORE_START)), plan.problems)
    assertEquals(0, applyRecurrences(plan, taskManager, undo.manager, "Create"),
      "a series with a problem stops the whole run, otherwise the rest could not be told apart afterwards")
    assertEquals(count, taskManager.taskCount)
    assertTrue(undo.editNames.isEmpty())
    assertEquals(0, repeated(task).size + repeated(other).size)
  }

  @Test
  fun `a series longer than the limit is reported and nothing is created`() {
    source(rule = RecurrenceRule(RecurrenceUnit.DAY, count = RecurrenceRule.MAX_OCCURRENCES + 1))
    val plan = planRecurrences(taskManager)
    assertEquals(listOf(RecurrenceProblem.TOO_MANY), plan.problems.map { it.problem })
    assertEquals(0, applyRecurrences(plan, taskManager, undo.manager, "Create"))
  }

  @Test
  fun `a series at the limit is created`() {
    val task = source(rule = RecurrenceRule(RecurrenceUnit.WEEK, count = RecurrenceRule.MAX_OCCURRENCES), effort = null)
    assertEquals(RecurrenceRule.MAX_OCCURRENCES - 1, run())
    assertEquals(RecurrenceRule.MAX_OCCURRENCES - 1, repeated(task).size)
  }

  @Test
  fun `the source and its repeated tasks are siblings in a group which is not a repeated task`() {
    val task = source()
    run()
    val group = taskManager.tasks.single { it.isRecurrenceGroup() }
    assertEquals(RecurrenceOrigin(task.taskID), group.recurrenceOrigin)
    val hierarchy = taskManager.taskHierarchy
    assertSame(group, hierarchy.getContainer(task))
    repeated(task).forEach { assertSame(group, hierarchy.getContainer(it)) }
    assertFalse(task.isRecurrenceGroup())
    assertFalse(repeated(task).first().isRecurrenceGroup())
    assertTrue(hierarchy.getNestedTasks(task).isEmpty(), "repeated tasks are not put under the source task")
  }

  @Test
  fun `the group takes the place of the source task among its siblings`() {
    val before = taskManager.newTaskBuilder().withName("before").withStartDate(monday.toModelDate()).build()
    val task = source()
    val after = taskManager.newTaskBuilder().withName("after").withStartDate(monday.toModelDate()).build()
    run()
    val group = taskManager.tasks.single { it.isRecurrenceGroup() }
    val hierarchy = taskManager.taskHierarchy
    assertEquals(listOf(before, group, after), hierarchy.getNestedTasks(hierarchy.rootTask).toList())
    assertSame(task, hierarchy.getNestedTasks(group).first(), "the source is the first task of the group")
  }

  @Test
  fun `a second run reuses the group`() {
    val task = source(rule = RecurrenceRule(RecurrenceUnit.WEEK, count = 2))
    run()
    task.createMutator().also {
      it.setRecurrenceRule(RecurrenceRule(RecurrenceUnit.WEEK, count = 3))
      it.commit()
    }
    run()
    assertEquals(1, taskManager.tasks.count { it.isRecurrenceGroup() })
  }

  @Test
  fun `a deleted repeated task is created again by the next run and the others stay`() {
    val task = source()
    run()
    val all = repeated(task)
    val deleted = all[1]
    taskManager.deleteTask(deleted)
    assertEquals(2, repeated(task).size)
    assertEquals(1, run())
    assertEquals(3, repeated(task).size)
    assertSame(all[0], repeated(task)[0])
    assertSame(all[2], repeated(task)[2])
    assertEquals(deleted.recurrenceOrigin!!.date, repeated(task)[1].recurrenceOrigin!!.date)
  }

  @Test
  fun `a task created from a rule is not repeated even if it is given a rule`() {
    val task = source(rule = RecurrenceRule(RecurrenceUnit.WEEK, count = 2))
    run()
    repeated(task).single().createMutator().also {
      it.setRecurrenceRule(RecurrenceRule(RecurrenceUnit.DAY, count = 5))
      it.commit()
    }
    assertEquals(0, run())
  }

  @Test
  fun `tasks without a rule are left alone`() {
    taskManager.newTaskBuilder().withName("plain").build()
    val plan = planRecurrences(taskManager)
    assertTrue(plan.occurrences.isEmpty())
    assertFalse(plan.hasProblems)
    assertEquals(0, plan.seriesCount)
  }

  @Test
  fun `a copy of a task has neither the rule nor the origin`() {
    val task = source()
    run()
    val copy = taskManager.newTaskBuilder().withPrototype(task).build()
    assertNull(copy.recurrenceRule)
    assertNull(copy.recurrenceOrigin)
    assertEquals(BigDecimal("6"), copy.effort, "the effort is copied as before")
  }
}
