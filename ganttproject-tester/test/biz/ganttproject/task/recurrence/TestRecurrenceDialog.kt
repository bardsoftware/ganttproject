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

import biz.ganttproject.app.LocalizedString
import biz.ganttproject.app.Localizer
import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.model.task.RecurrenceRule
import biz.ganttproject.core.model.task.RecurrenceUnit
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.time.DayOfWeek
import java.time.LocalDate

class TestRecurrenceDialog {
  private val taskManager: TaskManager = TestSetupHelper.newTaskManagerBuilder().withCalendar(WeekendCalendarImpl()).build()

  /** Counts the edits and runs them at once. Everything else is not expected to be called. */
  private class RecordingUndoManager {
    val editNames = mutableListOf<String>()
    var beforeEdit: () -> Unit = {}
    val manager: GPUndoManager = Proxy.newProxyInstance(
      GPUndoManager::class.java.classLoader, arrayOf(GPUndoManager::class.java)) { _, method, args ->
      check(method.name == "undoableEdit") { "unexpected call ${method.name}" }
      editNames.add(args[0] as String)
      beforeEdit()
      (args[1] as Runnable).run()
      null
    } as GPUndoManager
  }

  private val undo = RecordingUndoManager()

  /** Gives the key, so that a test can tell which message is shown. */
  private val keys = object : Localizer {
    override fun create(key: String) = LocalizedString(key, this)
    override fun formatTextOrNull(key: String, vararg args: Any): String = key
  }

  // 2026-11-09 is a Monday.
  private val monday = LocalDate.of(2026, 11, 9)

  private fun task(rule: RecurrenceRule? = null): Task {
    val task = taskManager.newTaskBuilder().withName("VAT return").withStartDate(monday.toModelDate()).build()
    rule?.let { r ->
      task.createMutator().also {
        it.setRecurrenceRule(r)
        it.commit()
      }
    }
    return task
  }

  private fun repeated(source: Task): List<Task> = taskManager.tasks
    .filter { it.recurrenceOrigin?.let { o -> o.sourceTaskId == source.taskID && !o.isGroup } == true }

  @Test
  fun `the fields make the rule, only the selected end counts`() {
    val model = RecurrenceDialogModel(task(), taskManager, keys)
    model.unitOption.value = RecurrenceUnit.MONTH
    model.intervalOption.value = 2
    model.endOption.value = RecurrenceEnd.COUNT
    model.countOption.value = 5
    model.untilOption.value = monday.plusYears(1)
    assertEquals(Ok(RecurrenceRule(RecurrenceUnit.MONTH, 2, count = 5)), model.rule)

    model.endOption.value = RecurrenceEnd.UNTIL
    assertEquals(Ok(RecurrenceRule(RecurrenceUnit.MONTH, 2, until = monday.plusYears(1))), model.rule)
  }

  @Test
  fun `the end field which is not selected is not writable`() {
    val model = RecurrenceDialogModel(task(), taskManager, keys)
    model.endOption.value = RecurrenceEnd.COUNT
    assertTrue(model.countOption.isWritable.value)
    assertFalse(model.untilOption.isWritable.value)
    model.endOption.value = RecurrenceEnd.UNTIL
    assertFalse(model.countOption.isWritable.value)
    assertTrue(model.untilOption.isWritable.value)
  }

  @Test
  fun `the fields of a task with a rule show its rule`() {
    val model = RecurrenceDialogModel(task(RecurrenceRule(RecurrenceUnit.DAY, 3, count = 7)), taskManager, keys)
    assertEquals(RecurrenceUnit.DAY, model.unitOption.value)
    assertEquals(3, model.intervalOption.value)
    assertEquals(RecurrenceEnd.COUNT, model.endOption.value)
    assertEquals(7, model.countOption.value)
    assertTrue(model.hasRule)
  }

  @Test
  fun `invalid fields do not make a rule`() {
    assertEquals(Err(RecurrenceFieldProblem.INTERVAL_NOT_POSITIVE), ruleFromFields(RecurrenceUnit.WEEK, 0, null, 3))
    assertEquals(Err(RecurrenceFieldProblem.INTERVAL_NOT_POSITIVE), ruleFromFields(RecurrenceUnit.WEEK, null, null, 3))
    assertEquals(Err(RecurrenceFieldProblem.NO_END), ruleFromFields(RecurrenceUnit.WEEK, 1, null, null))
    assertEquals(Err(RecurrenceFieldProblem.BOTH_ENDS), ruleFromFields(RecurrenceUnit.WEEK, 1, monday, 3))
    assertEquals(Err(RecurrenceFieldProblem.COUNT_NOT_POSITIVE), ruleFromFields(RecurrenceUnit.WEEK, 1, null, 0))
    assertEquals(Ok(RecurrenceRule(RecurrenceUnit.WEEK, 1, count = 3)), ruleFromFields(RecurrenceUnit.WEEK, 1, null, 3))
  }

  @Test
  fun `an interval of zero is reported and cannot be applied`() {
    val model = RecurrenceDialogModel(task(), taskManager, keys)
    model.intervalOption.value = 0
    assertNull(model.validRule)
    assertEquals("task.recurrence.error.intervalNotPositive", model.previewText)
  }

  @Test
  fun `an end before the start is reported and cannot be applied`() {
    val model = RecurrenceDialogModel(task(), taskManager, keys)
    model.endOption.value = RecurrenceEnd.UNTIL
    model.untilOption.value = monday.minusDays(1)
    assertNull(model.validRule)
    assertEquals("task.recurrence.error.endBeforeStart", model.previewText)
  }

  @Test
  fun `too many tasks are reported and cannot be applied`() {
    val model = RecurrenceDialogModel(task(), taskManager, keys)
    model.unitOption.value = RecurrenceUnit.DAY
    model.endOption.value = RecurrenceEnd.COUNT
    model.countOption.value = RecurrenceRule.MAX_OCCURRENCES + 1
    assertNull(model.validRule)
    assertEquals("task.recurrence.error.tooMany", model.previewText)
  }

  @Test
  fun `the preview counts the source task and moves dates off the weekend`() {
    // Every 5 days from a Monday: Mon 9, Sat 14 -> Mon 16, Thu 19.
    val preview = previewOf(RecurrenceRule(RecurrenceUnit.DAY, 5, count = 3), monday) { it.dayOfWeek < DayOfWeek.SATURDAY }
    assertEquals(Ok(RecurrencePreview(3, monday, monday.plusDays(10))), preview)

    val model = RecurrenceDialogModel(task(), taskManager, keys)
    model.endOption.value = RecurrenceEnd.COUNT
    model.countOption.value = 3
    assertEquals(RecurrenceRule(RecurrenceUnit.WEEK, 1, count = 3), model.validRule)
    assertEquals("task.recurrence.preview", model.previewText)
  }

  @Test
  fun `applying sets the rule and creates the tasks as one undoable edit`() {
    val source = task()
    val tasksBefore = taskManager.taskCount
    val stateAtEdit = mutableListOf<Pair<RecurrenceRule?, Int>>()
    undo.beforeEdit = { stateAtEdit.add(source.recurrenceRule to taskManager.taskCount) }
    val created = setRecurrence(source, RecurrenceRule(RecurrenceUnit.WEEK, count = 4), taskManager, undo.manager, "Apply")
    assertEquals(3, created)
    // Nothing was changed before the edit started, so undoing it takes back the rule and the tasks.
    assertEquals(listOf<Pair<RecurrenceRule?, Int>>(null to tasksBefore), stateAtEdit)
    assertEquals(listOf("Apply"), undo.editNames)
    assertEquals(RecurrenceRule(RecurrenceUnit.WEEK, count = 4), source.recurrenceRule)
    assertEquals(3, repeated(source).size)
  }

  @Test
  fun `applying a rule which cannot be applied changes nothing`() {
    val source = task()
    val before = taskManager.taskCount
    val created = setRecurrence(source, RecurrenceRule(RecurrenceUnit.WEEK, until = monday.minusDays(1)), taskManager, undo.manager, "Apply")
    assertNull(created)
    assertEquals(emptyList<String>(), undo.editNames)
    assertNull(source.recurrenceRule)
    assertEquals(before, taskManager.taskCount)
  }

  @Test
  fun `applying a changed rule keeps the existing tasks and adds the missing ones`() {
    val source = task()
    setRecurrence(source, RecurrenceRule(RecurrenceUnit.WEEK, count = 3), taskManager, undo.manager, "Apply")
    val created = setRecurrence(source, RecurrenceRule(RecurrenceUnit.WEEK, count = 5), taskManager, undo.manager, "Apply")
    assertEquals(2, created)
    assertEquals(4, repeated(source).size)
  }

  @Test
  fun `removing the rule keeps the repeated tasks as one undoable edit`() {
    val source = task()
    setRecurrence(source, RecurrenceRule(RecurrenceUnit.WEEK, count = 4), taskManager, undo.manager, "Apply")
    removeRecurrence(source, undo.manager, "Remove")
    assertNull(source.recurrenceRule)
    assertEquals(listOf("Apply", "Remove"), undo.editNames)
    assertEquals(3, repeated(source).size)
  }

  @Test
  fun `removing from a task without a rule makes no edit`() {
    removeRecurrence(task(), undo.manager, "Remove")
    assertEquals(emptyList<String>(), undo.editNames)
  }

  @Test
  fun `a rule can be set on a plain task but not on a repeated task or a group`() {
    val source = task()
    assertTrue(canRecur(source, taskManager))
    setRecurrence(source, RecurrenceRule(RecurrenceUnit.WEEK, count = 2), taskManager, undo.manager, "Apply")
    assertTrue(canRecur(source, taskManager))
    assertFalse(canRecur(repeated(source).single(), taskManager))
    val group = taskManager.tasks.single { it.isRecurrenceGroup() }
    assertFalse(canRecur(group, taskManager))
  }
}
