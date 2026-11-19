/*
Copyright 2026

This file is part of GanttProject, an open-source project management tool.

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

import biz.ganttproject.core.time.CalendarFactory
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.util.Locale

class BaselineCatchUpTest {
  private lateinit var taskManager: TaskManager
  private lateinit var oldTask: Task
  private lateinit var summary: Task

  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
    taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    oldTask = newTask("old", 2026, 8, 11)
    summary = newTask("summary", 2026, 8, 11)
  }

  private fun newTask(name: String, year: Int, month: Int, day: Int, parent: Task? = null): Task =
    taskManager.newTaskBuilder().withName(name).withParent(parent)
      .withStartDate(CalendarFactory.createGanttCalendar(year, month, day).time)
      .withDuration(taskManager.createLength(2)).build()

  private fun takeBaseline(name: String) = GanttPreviousState(name, GanttPreviousState.createTasks(taskManager))

  private fun rows(baseline: GanttPreviousState) = baseline.tasks.map {
    listOf(it.id, it.start.toXMLString(), it.duration, it.isMilestone, it.hasNested())
  }

  @Test
  fun `a project without a baseline says so, and an empty project says nothing`() {
    assertEquals(BaselineGap.NoBaseline, baselineGap(taskManager, emptyList(), null))
    val empty = TestSetupHelper.newTaskManagerBuilder().build()
    assertEquals(BaselineGap.Covered, baselineGap(empty, emptyList(), null))
  }

  @Test
  fun `tasks created after the baseline are missing, including subtasks and milestones`() {
    val base = takeBaseline("base")
    val child = newTask("child", 2026, 9, 1, summary)
    val milestone = newTask("milestone", 2026, 9, 2).also { task -> task.createMutator().also { it.setMilestone(true); it.commit() } }

    val gap = baselineGap(taskManager, listOf(base), null) as BaselineGap.Missing
    assertSame(base, gap.baseline)
    assertEquals(listOf(child, milestone), gap.tasks)
  }

  @Test
  fun `a deleted task is not missing, and a baseline with every task is covered`() {
    val extra = newTask("extra", 2026, 9, 1)
    val base = takeBaseline("base")
    assertEquals(BaselineGap.Covered, baselineGap(taskManager, listOf(base), null))

    val later = newTask("later", 2026, 9, 3)
    taskManager.deleteTask(later)
    taskManager.deleteTask(extra)
    assertEquals(BaselineGap.Covered, baselineGap(taskManager, listOf(base), null))
  }

  @Test
  fun `the shown baseline is the target, otherwise the last one`() {
    val first = takeBaseline("first")
    newTask("new", 2026, 9, 1)
    val second = takeBaseline("second")

    assertSame(second, catchUpTarget(listOf(first, second), null))
    assertSame(first, catchUpTarget(listOf(first, second), first))
    assertEquals(BaselineGap.Covered, baselineGap(taskManager, listOf(first, second), null))
    assertEquals(1, (baselineGap(taskManager, listOf(first, second), first) as BaselineGap.Missing).tasks.size)
    // A shown baseline which was removed from the list does not count.
    assertSame(second, catchUpTarget(listOf(second), first))
  }

  @Test
  fun `catching up adds exactly the missing tasks`() {
    val base = takeBaseline("base")
    val child = newTask("child", 2026, 9, 1, summary)
    val other = newTask("other", 2026, 9, 5)
    val list = mutableListOf(base)

    val result = catchUpBaseline(list, base, taskManager, RecordingUndoManager(), "Add missing tasks")!!

    assertSame(result, list.single())
    assertEquals("base", result.name)
    assertEquals(listOf(oldTask.taskID, summary.taskID, child.taskID, other.taskID), result.tasks.map { it.id })
    assertEquals(BaselineGap.Covered, baselineGap(taskManager, list, result))
  }

  @Test
  fun `catching up keeps the existing entries as they were taken`() {
    val base = takeBaseline("base")
    val before = rows(base)
    oldTask.start.setTime(CalendarFactory.createGanttCalendar(2027, 0, 1).time)
    newTask("new", 2026, 9, 1)

    val result = catchUpBaseline(mutableListOf(base), base, taskManager, RecordingUndoManager(), "x")!!

    assertEquals(before, rows(result).take(before.size))
    assertEquals(before, rows(base), "the source baseline must not change")
  }

  @Test
  fun `catching up is one undoable edit`() {
    val base = takeBaseline("base")
    newTask("a", 2026, 9, 1)
    newTask("b", 2026, 9, 2)
    val list = mutableListOf(base)
    val undoManager = RecordingUndoManager(list)

    val result = catchUpBaseline(list, base, taskManager, undoManager, "Add missing tasks")

    assertEquals(listOf("Add missing tasks"), undoManager.names)
    undoManager.undo()
    assertSame(base, list.single())
    undoManager.redo()
    assertSame(result, list.single())
  }

  @Test
  fun `nothing missing means no edit`() {
    val base = takeBaseline("base")
    val list = mutableListOf(base)
    val undoManager = RecordingUndoManager()

    assertNull(catchUpBaseline(list, base, taskManager, undoManager, "x"))
    assertEquals(emptyList<String>(), undoManager.names)
    assertSame(base, list.single())
  }

  /** Records the edits and, like the real manager, keeps the project before and after the last one. */
  private class RecordingUndoManager(private val list: MutableList<GanttPreviousState> = mutableListOf()) : GPUndoManager {
    val names = mutableListOf<String>()
    private var before = listOf<GanttPreviousState>()
    private var after = listOf<GanttPreviousState>()

    override fun undoableEdit(localizedName: String, runnableEdit: Runnable) {
      names.add(localizedName)
      before = list.toList()
      runnableEdit.run()
      after = list.toList()
    }

    private fun restore(state: List<GanttPreviousState>) {
      list.clear()
      list.addAll(state)
    }

    override fun undo() = restore(before)
    override fun redo() = restore(after)
    override fun canUndo() = names.isNotEmpty()
    override fun canRedo() = false
    override val undoPresentationName = ""
    override val redoPresentationName = ""
    override fun addUndoableEditListener(listener: GPUndoListener) {}
    override fun removeUndoableEditListener(listener: GPUndoListener) {}
    override fun die() {}
    override fun addUndoableEditTxnFactory(factory: UndoableEditTxnFactory) {}
  }
}
