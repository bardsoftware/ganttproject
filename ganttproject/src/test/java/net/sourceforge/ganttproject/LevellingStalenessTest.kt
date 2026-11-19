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

import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.LevellingRunNotifier
import net.sourceforge.ganttproject.task.algorithm.LevellingStaleness
import net.sourceforge.ganttproject.task.algorithm.applyLevelling
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/** The mark which says that the plan has changed since the last levelling run. */
class LevellingStalenessTest {
  private lateinit var project: GanttProjectImpl
  private val monday = LocalDate.of(2026, 11, 2)
  private val notifier = LevellingRunNotifier()
  private lateinit var staleness: LevellingStaleness

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
    staleness = LevellingStaleness(notifier)
    project.taskManager.addTaskListener(staleness.taskListener)
    project.humanResourceManager.addView(staleness.resourceView)
  }

  @AfterEach
  fun tearDown() {
    staleness.detach()
  }

  private fun task(): Task = project.taskManager.newTaskBuilder().withName("t")
    .withStartDate(CalendarFactory.createGanttCalendar(2026, 10, 2).time)
    .withDuration(project.taskManager.createLength(2)).build()

  @Test
  fun `a new mark is clear`() {
    assertFalse(staleness.isStale)
  }

  @Test
  fun `a task change sets the mark`() {
    val task = task()
    assertTrue(staleness.isStale)

    staleness.clear()
    task.createMutator().also { it.setDuration(project.taskManager.createLength(4)) }.commit()
    assertTrue(staleness.isStale)
  }

  @Test
  fun `a resource change sets the mark`() {
    project.humanResourceManager.create("Ann", 1)
    assertTrue(staleness.isStale)
  }

  @Test
  fun `a calendar change sets the mark`() {
    staleness.calendarListener.onCalendarChange()
    assertTrue(staleness.isStale)
  }

  @Test
  fun `a levelling run clears the mark`() {
    val task = task()
    assertTrue(staleness.isStale)
    applyLevelling(mapOf(task.taskID.toString() to monday.plusDays(1)), emptyMap(), project.taskManager,
      undoManager, "level", notifier)
    assertFalse(staleness.isStale)
  }

  @Test
  fun `a run which moves nothing clears the mark too`() {
    val task = task()
    applyLevelling(mapOf(task.taskID.toString() to monday), emptyMap(), project.taskManager, undoManager, "level",
      notifier)
    assertFalse(staleness.isStale)
  }

  @Test
  fun `a change made by a later listener of the run does not set the mark again`() {
    val first = task()
    val second = task()
    // The listener is registered after the mark, so it runs after the mark has been cleared.
    notifier.addListener {
      second.createMutator().also { it.setDuration(project.taskManager.createLength(7)) }.commit()
    }
    applyLevelling(
      mapOf(first.taskID.toString() to monday.plusDays(1), second.taskID.toString() to monday.plusDays(3)),
      emptyMap(), project.taskManager, undoManager, "level", notifier)
    assertEquals(3, first.start.date)
    assertFalse(staleness.isStale)
  }

  @Test
  fun `the dates written back by a run do not set the mark`() {
    val task = task()
    staleness.clear()
    var markedDuringRun = false
    project.taskManager.addTaskListener(object : net.sourceforge.ganttproject.task.event.TaskListenerAdapter() {
      override fun taskScheduleChanged(e: net.sourceforge.ganttproject.task.event.TaskScheduleEvent) {
        markedDuringRun = notifier.isRunning && staleness.isStale
      }
    })
    applyLevelling(mapOf(task.taskID.toString() to monday.plusDays(1)), emptyMap(), project.taskManager,
      undoManager, "level", notifier)
    assertFalse(markedDuringRun)
    assertFalse(staleness.isStale)
  }

  @Test
  fun `loading another plan clears the mark`() {
    task()
    project.humanResourceManager.create("Ann", 1)
    assertTrue(staleness.isStale)
    staleness.taskListener.taskModelReset()
    assertFalse(staleness.isStale)

    staleness.markStale()
    staleness.resourceView.resourceModelReset()
    assertFalse(staleness.isStale)
  }

  @Test
  fun `listeners hear a change of the mark and not a repeat of it`() {
    val heard = mutableListOf<Boolean>()
    staleness.addListener { heard.add(it) }
    staleness.markStale()
    staleness.markStale()
    staleness.clear()
    staleness.clear()
    assertEquals(listOf(true, false), heard)
  }

  @Test
  fun `a failing listener does not stop the others`() {
    val heard = mutableListOf<Boolean>()
    staleness.addListener { throw IllegalStateException("listener fails") }
    staleness.addListener { heard.add(it) }
    staleness.markStale()
    assertEquals(listOf(true), heard)
  }
}
