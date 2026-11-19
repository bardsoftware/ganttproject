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

import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.action.task.TaskBackfillEffortAction
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskSelectionManager
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Locale
import javax.swing.Action

/** The backfill action: what it asks, what it refuses, and that a confirmed backfill is one undoable edit. */
class TaskBackfillEffortActionTest {
  private lateinit var project: GanttProjectImpl
  private val errors = mutableListOf<String>()
  private val options = mutableListOf<Pair<String, Array<Action>>>()
  private val edits = mutableListOf<String>()

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

  private val undoManager = object : GPUndoManager {
    override fun undoableEdit(localizedName: String, runnableEdit: Runnable) {
      edits.add(localizedName)
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

  /** Records the dialogs instead of showing them. */
  private val uiFacade = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(UIFacade::class.java)) { _, method, args ->
    when (method.name) {
      "showErrorDialog" -> { errors.add(args[0].toString()); null }
      "showOptionDialog" -> {
        @Suppress("UNCHECKED_CAST")
        options.add(args[1] as String to args[2] as Array<Action>)
        null
      }
      "getUndoManager" -> undoManager
      else -> null
    }
  } as UIFacade

  private fun action() = TaskBackfillEffortAction(project.taskManager, TaskSelectionManager { project.taskManager }, uiFacade)

  private fun task(days: Long, hoursPerDay: String, utilization: Int? = null): Task {
    val resource = project.humanResourceManager.create("r", project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal(hoursPerDay)
      it.utilization = utilization
    }
    return project.taskManager.newTaskBuilder().withName("t").build().also { task ->
      task.createMutator().also {
        it.setStart(CalendarFactory.createGanttCalendar(2026, 10, 2))
        it.setDuration(project.taskManager.createLength(days))
        it.commit()
      }
      task.assignmentCollection.addAssignment(resource).load = 100f
    }
  }

  @Test
  fun `a capacity problem shows an error and changes nothing`() {
    val good = task(2, "8")
    task(2, "8", utilization = 0)
    action().actionPerformed(null)
    assertEquals(1, errors.size)
    assertTrue(options.isEmpty())
    assertTrue(edits.isEmpty())
    assertNull(good.effort)
  }

  @Test
  fun `nothing is written before the user confirms, and then as one undoable edit`() {
    val t1 = task(2, "8")
    val t2 = task(3, "6")
    action().actionPerformed(null)
    assertEquals(1, options.size)
    assertNull(t1.effort)
    assertTrue(edits.isEmpty())

    options[0].second[0].actionPerformed(null)
    assertEquals(1, edits.size)
    assertEquals(BigDecimal("16"), t1.effort)
    assertEquals(BigDecimal("18"), t2.effort)
  }

  @Test
  fun `cancelling writes nothing`() {
    val t = task(2, "8")
    action().actionPerformed(null)
    options[0].second[1].actionPerformed(null)
    assertTrue(edits.isEmpty())
    assertNull(t.effort)
  }

  @Test
  fun `without anything to fill the user is told so`() {
    task(2, "8").also { it.createMutator().apply { setEffort(BigDecimal("3")); commit() } }
    action().actionPerformed(null)
    assertEquals(1, options.size)
    assertEquals(1, options[0].second.size)
    assertTrue(edits.isEmpty())
  }
}
