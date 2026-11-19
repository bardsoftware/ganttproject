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

import biz.ganttproject.core.model.task.CapacityChange
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.algorithm.BackfillPreparation
import net.sourceforge.ganttproject.task.algorithm.BackfillSkip
import net.sourceforge.ganttproject.task.algorithm.CapacityProblem
import net.sourceforge.ganttproject.task.algorithm.applyEffortBackfill
import net.sourceforge.ganttproject.task.algorithm.prepareEffortBackfill
import net.sourceforge.ganttproject.task.algorithm.proposeEffortBackfill
import net.sourceforge.ganttproject.task.algorithm.tasksForBackfill
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/** Deriving the effort of tasks which have none from their duration and the hours of their assignments. */
class EffortBackfillTest {
  private lateinit var project: GanttProjectImpl

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

  private fun resource(hoursPerDay: String?): HumanResource =
    project.humanResourceManager.create("r", project.humanResourceManager.resources.size).also {
      it.hoursPerDay = hoursPerDay?.let(::BigDecimal)
    }

  private fun change(date: String, hours: String) = CapacityChange(LocalDate.parse(date), BigDecimal(hours))

  /** A task without effort which starts on [start] November 2026 (2 is a Monday) and lasts [days] working days. */
  private fun task(days: Long, vararg resources: HumanResource, start: Int = 2, load: Float = 100f): Task =
    project.taskManager.newTaskBuilder().withName("t").build().also { task ->
      task.createMutator().also {
        it.setStart(CalendarFactory.createGanttCalendar(2026, 10, start))
        it.setDuration(project.taskManager.createLength(days))
        it.commit()
      }
      resources.forEach { task.assignmentCollection.addAssignment(it).load = load }
    }

  private fun propose(vararg tasks: Task) = proposeEffortBackfill(tasks.toList(), project.taskManager.calendar)

  private fun fill(vararg tasks: Task) = applyEffortBackfill(propose(*tasks), project.taskManager)

  @Test
  fun `the effort is the duration multiplied by the hours per day`() {
    val t = task(3, resource("7.5"))
    assertEquals(BigDecimal("22.5"), propose(t).effort[t])
  }

  @Test
  fun `a resource without hours per day counts with the default hours`() {
    val t = task(2, resource(null))
    assertEquals(BigDecimal("16"), propose(t).effort[t])
  }

  @Test
  fun `the effort follows a change of hours and the utilization day by day`() {
    val r = resource("8").also {
      it.utilization = 50
      it.hoursSchedule = listOf(change("2026-11-04", "6"))
    }
    val t = task(5, r)
    // Mon, Tue: 8 h at 50 % = 4; Wed, Thu, Fri: 6 h at 50 % = 3
    assertEquals(BigDecimal("17"), propose(t).effort[t])
  }

  @Test
  fun `the days off are not counted`() {
    val r = resource("8").also { it.hoursSchedule = listOf(change("2026-11-09", "4")) }
    // Thursday 5 November, three working days: Thu, Fri, and after the weekend Mon with 4 h
    val t = task(3, r, start = 5)
    assertEquals(BigDecimal("20"), propose(t).effort[t])
  }

  @Test
  fun `filling sets effort and effort driven and keeps the duration`() {
    val r = resource("8").also {
      it.utilization = 50
      it.hoursSchedule = listOf(change("2026-11-04", "6"))
    }
    val t = task(5, r)
    fill(t)
    assertEquals(BigDecimal("17"), t.effort)
    assertTrue(t.isEffortDriven)
    assertEquals(5, t.duration.length)
  }

  @Test
  fun `the hours of several resources are added up`() {
    val t = task(2, resource("8"), resource("4"))
    assertEquals(BigDecimal("24"), propose(t).effort[t])
  }

  @Test
  fun `the load of the assignment scales the effort`() {
    val t = task(3, resource("8"), load = 50f)
    assertEquals(BigDecimal("12"), propose(t).effort[t])
  }

  @Test
  fun `a task with effort keeps it`() {
    val t = task(3, resource("8")).also {
      it.createMutator().apply { setEffort(BigDecimal("5")); commit() }
    }
    val proposal = propose(t)
    assertEquals(BackfillSkip.HAS_EFFORT, proposal.skipped[t])
    applyEffortBackfill(proposal, project.taskManager)
    assertEquals(BigDecimal("5"), t.effort)
    assertFalse(t.isEffortDriven)
  }

  @Test
  fun `a task without assignments gets no effort`() {
    val t = task(3)
    fill(t)
    assertEquals(BackfillSkip.NO_ASSIGNMENT, propose(t).skipped[t])
    assertNull(t.effort)
    assertFalse(t.isEffortDriven)
  }

  @Test
  fun `a milestone gets no effort`() {
    val t = task(1, resource("8")).also {
      it.createMutator().apply { setMilestone(true); commit() }
    }
    assertEquals(BackfillSkip.MILESTONE, propose(t).skipped[t])
  }

  @Test
  fun `a supertask gets no effort but its subtasks do`() {
    val r = resource("8")
    val parent = task(3, r)
    val child = task(2, r)
    project.taskManager.taskHierarchy.move(child, parent)
    val proposal = propose(parent, child)
    assertEquals(BackfillSkip.SUPERTASK, proposal.skipped[parent])
    assertEquals(BigDecimal("16"), proposal.effort[child])
  }

  @Test
  fun `an assignment without load gives no hours`() {
    val t = task(3, resource("8"), load = 0f)
    assertEquals(BackfillSkip.NO_HOURS, propose(t).skipped[t])
  }

  @Test
  fun `a task whose last day has no hours is left alone because its duration would shrink`() {
    val r = resource("8").also { it.hoursSchedule = listOf(change("2026-11-04", "0")) }
    // Mon, Tue with 8 h, Wed with 0 h: 16 h would be done after two days
    val t = task(3, r)
    assertEquals(BackfillSkip.DURATION_WOULD_CHANGE, propose(t).skipped[t])
  }

  @Test
  fun `days without hours in the middle are counted as days of the task`() {
    val r = resource("8").also { it.hoursSchedule = listOf(change("2026-11-03", "0"), change("2026-11-04", "8")) }
    val t = task(3, r)
    fill(t)
    assertEquals(BigDecimal("16"), t.effort)
    assertEquals(3, t.duration.length)
  }

  @Test
  fun `a utilization of zero from a file stops the whole backfill`() {
    val bad = resource("8").also { it.utilization = 0 }
    val good = task(3, resource("8"))
    val t = task(3, bad)
    val preparation = prepareEffortBackfill(listOf(good, t), project.taskManager.calendar)
    assertTrue(preparation is BackfillPreparation.Problems)
    assertEquals(mapOf(bad to listOf(CapacityProblem.UTILIZATION)), (preparation as BackfillPreparation.Problems).problems)
    assertNull(good.effort)
  }

  @Test
  fun `hours per day of zero and hours above 24 are capacity problems`() {
    val zero = resource("0")
    val many = resource("8").also { it.hoursSchedule = listOf(change("2026-11-04", "25")) }
    val preparation = prepareEffortBackfill(listOf(task(1, zero), task(1, many)), project.taskManager.calendar)
    assertEquals(mapOf(zero to listOf(CapacityProblem.HOURS_PER_DAY), many to listOf(CapacityProblem.HOURS_CHANGE)),
      (preparation as BackfillPreparation.Problems).problems)
  }

  @Test
  fun `resources of tasks which keep their effort are not checked`() {
    val bad = resource("8").also { it.utilization = 0 }
    val t = task(3, bad).also { it.createMutator().apply { setEffort(BigDecimal("5")); commit() } }
    assertTrue(prepareEffortBackfill(listOf(t), project.taskManager.calendar) is BackfillPreparation.Ready)
  }

  @Test
  fun `the selection is filled with its subtasks, and everything without a selection`() {
    val parent = task(3)
    val child = task(2)
    val other = task(1)
    project.taskManager.taskHierarchy.move(child, parent)
    assertEquals(setOf(parent, child), tasksForBackfill(listOf(parent), project.taskManager).toSet())
    assertEquals(setOf(parent, child, other), tasksForBackfill(emptyList(), project.taskManager).toSet())
  }

  @Test
  fun `all tasks are filled within one undoable edit`() {
    val r = resource("8")
    val t1 = task(2, r)
    val t2 = task(3, r)
    val proposal = propose(t1, t2)
    val undoManager = RecordingUndoManager()
    applyEffortBackfill(proposal, project.taskManager, undoManager, "backfill")
    assertEquals(listOf("backfill"), undoManager.edits)
    assertEquals(listOf(null, null), undoManager.effortBefore)
    assertEquals(BigDecimal("16"), t1.effort)
    assertEquals(BigDecimal("24"), t2.effort)
  }

  /** Runs each edit and records what the tasks looked like when the edit began. */
  private inner class RecordingUndoManager : GPUndoManager {
    val edits = mutableListOf<String>()
    val effortBefore = mutableListOf<BigDecimal?>()
    override fun undoableEdit(localizedName: String, runnableEdit: Runnable) {
      edits.add(localizedName)
      project.taskManager.tasks.forEach { effortBefore.add(it.effort) }
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
}
