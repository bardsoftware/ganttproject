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
import net.sourceforge.ganttproject.task.algorithm.LevellingRunFinished
import net.sourceforge.ganttproject.task.algorithm.LevellingRunNotifier
import net.sourceforge.ganttproject.task.algorithm.applyLevelling
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/** The event at the end of a levelling run. */
class LevellingRunNotifierTest {
  private lateinit var project: GanttProjectImpl
  private val monday = LocalDate.of(2026, 11, 2)
  private val notifier = LevellingRunNotifier()
  private val events = mutableListOf<LevellingRunFinished>()

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
    notifier.addListener { events.add(it) }
  }

  private fun task() = project.taskManager.newTaskBuilder().withName("t")
    .withStartDate(CalendarFactory.createGanttCalendar(2026, 10, 2).time)
    .withDuration(project.taskManager.createLength(2)).build()

  @Test
  fun `the run is reported after the dates are written back`() {
    val task = task()
    var startSeen: Int? = null
    notifier.addListener { startSeen = project.taskManager.getTask(task.taskID).start.date }
    applyLevelling(mapOf(task.taskID.toString() to monday.plusDays(1)), emptyMap(), project.taskManager,
      undoManager, "level", notifier)
    assertEquals(listOf(LevellingRunFinished(1)), events)
    assertEquals(3, startSeen)
  }

  @Test
  fun `a run which moves nothing is reported too`() {
    val task = task()
    applyLevelling(mapOf(task.taskID.toString() to monday), emptyMap(), project.taskManager, undoManager, "level",
      notifier)
    assertEquals(listOf(LevellingRunFinished(0)), events)
  }

  @Test
  fun `a run inside a run is reported once`() {
    notifier.runAndReport { notifier.runAndReport { 2 } + 1 }
    assertEquals(listOf(LevellingRunFinished(3)), events)
  }

  @Test
  fun `a failing listener does not stop the others`() {
    val other = LevellingRunNotifier()
    val seen = mutableListOf<Int>()
    other.addListener { throw IllegalStateException("listener fails") }
    other.addListener { seen.add(it.movedTasks) }
    assertEquals(4, other.runAndReport { 4 })
    assertEquals(listOf(4), seen)
  }
}
