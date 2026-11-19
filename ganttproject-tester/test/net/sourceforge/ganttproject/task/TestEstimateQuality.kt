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
package net.sourceforge.ganttproject.task

import biz.ganttproject.core.io.XmlTasks
import biz.ganttproject.lib.fx.SimpleTreeCollapseView
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.parser.TaskLoader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TestEstimateQuality {
  private val builder = TestSetupHelper.newTaskManagerBuilder()
  private val taskManager = builder.build()
  private val alice = builder.resourceManager.create("alice", 1)
  private val bob = builder.resourceManager.create("bob", 2)

  private fun newTask() = taskManager.newTaskBuilder().withName("t").build()

  private fun Task.edit(change: MutableTask.() -> Unit) {
    createMutator().also {
      it.change()
      it.commit()
    }
  }

  @Test
  fun `a new task has no original effort`() {
    assertNull(newTask().originalEffort)
    assertNull(newTask().originalEffortHours)
    assertNull(newTask().estimateQuality)
  }

  @Test
  fun `the first effort of a task is kept as the original effort`() {
    val task = newTask()
    task.edit { setEffort(BigDecimal("9")) }
    assertEquals(BigDecimal("9"), task.originalEffort)
  }

  @Test
  fun `a later change of the effort does not touch the original effort`() {
    val task = newTask()
    task.edit { setEffort(BigDecimal("9")) }
    task.edit { setEffort(BigDecimal("15")) }
    assertEquals(BigDecimal("15"), task.effort)
    assertEquals(BigDecimal("9"), task.originalEffort, "the original effort is kept once")
  }

  @Test
  fun `an explicit original effort wins over the first effort and can be cleared`() {
    val task = newTask()
    task.edit {
      setEffort(BigDecimal("9"))
      setOriginalEffort(BigDecimal("6"))
    }
    assertEquals(BigDecimal("6"), task.originalEffort)

    task.edit { setOriginalEffort(null) }
    assertNull(task.originalEffort, "the original effort has to be clearable")
  }

  @Test
  fun `clearing the effort leaves the original effort`() {
    val task = newTask()
    task.edit { setEffort(BigDecimal("9")) }
    task.edit { setEffort(null) }
    assertNull(task.effort)
    assertEquals(BigDecimal("9"), task.originalEffort)
  }

  @Test
  fun `the original effort does not change the planned effort or the duration`() {
    val task = newTask()
    task.edit {
      setEffort(BigDecimal("40"))
      setEffortDriven(true)
    }
    val duration = task.duration.length
    task.edit { setOriginalEffort(BigDecimal("3")) }
    assertEquals(BigDecimal("40"), task.effort)
    assertEquals(duration, task.duration.length)
  }

  @Test
  fun `the clone of a task keeps the original effort`() {
    val task = newTask()
    task.edit { setEffort(BigDecimal("9")) }
    assertEquals(BigDecimal("9"), task.unpluggedClone().originalEffort)
  }

  @Test
  fun `15 hours spent on 9 estimated is a factor of 1,67`() {
    val task = newTask()
    task.edit {
      setEffort(BigDecimal("9"))
      setActualEffort(BigDecimal("15"))
    }
    val quality = task.estimateQuality!!
    assertEquals(BigDecimal("9"), quality.originalHours)
    assertEquals(BigDecimal("6"), quality.extraHours)
    assertEquals(0, BigDecimal("15").divide(BigDecimal("9"), java.math.MathContext.DECIMAL32).compareTo(quality.factor))
    assertFalse(quality.isReplanned)
  }

  @Test
  fun `spending exactly the estimate is a factor of 1`() {
    val task = newTask()
    task.edit {
      setEffort(BigDecimal("8"))
      setActualEffort(BigDecimal("8"))
    }
    assertEquals(BigDecimal.ONE, task.estimateQuality!!.factor)
    assertEquals(0, task.estimateQuality!!.extraHours!!.signum())
  }

  @Test
  fun `a replanned effort shows next to the original one`() {
    val task = newTask()
    task.edit { setEffort(BigDecimal("9")) }
    task.edit { setEffort(BigDecimal("12")) }
    val quality = task.estimateQuality!!
    assertEquals(BigDecimal("9"), quality.originalHours)
    assertEquals(BigDecimal("12"), quality.currentHours)
    assertTrue(quality.isReplanned)
  }

  @Test
  fun `a task without actual effort has no factor`() {
    val task = newTask()
    task.edit { setEffort(BigDecimal("9")) }
    val quality = task.estimateQuality!!
    assertNull(quality.factor)
    assertNull(quality.extraHours)
  }

  @Test
  fun `an original effort of zero has no factor`() {
    val task = newTask()
    task.edit {
      setOriginalEffort(BigDecimal.ZERO)
      setActualEffort(BigDecimal("4"))
    }
    val quality = task.estimateQuality!!
    assertNull(quality.factor, "dividing by zero has to give no factor")
    assertEquals(BigDecimal("4"), quality.extraHours)
  }

  @Test
  fun `actual effort recorded only per assignment gives the factor`() {
    val task = newTask()
    task.edit { setEffort(BigDecimal("9")) }
    task.assignmentCollection.addAssignment(alice).actualEffort = BigDecimal("6")
    task.assignmentCollection.addAssignment(bob).actualEffort = BigDecimal("6")
    assertNull(task.actualEffort, "nothing was recorded on the task itself")
    val quality = task.estimateQuality!!
    assertEquals(0, BigDecimal("12").compareTo(quality.actualHours))
    assertEquals(0, BigDecimal("3").compareTo(quality.extraHours))
    assertEquals(0, BigDecimal("12").divide(BigDecimal("9"), java.math.MathContext.DECIMAL32).compareTo(quality.factor))
  }

  @Test
  fun `the actual effort per assignment wins over the task's own value in the factor`() {
    val task = newTask()
    task.edit {
      setEffort(BigDecimal("8"))
      setActualEffort(BigDecimal("2"))
    }
    task.assignmentCollection.addAssignment(alice).actualEffort = BigDecimal("4")
    task.assignmentCollection.addAssignment(bob).actualEffort = BigDecimal("8")
    assertEquals(0, BigDecimal("1.5").compareTo(task.estimateQuality!!.factor))
  }

  @Test
  fun `a task without original effort has no estimate quality even with actual effort`() {
    val task = newTask()
    task.edit { setActualEffort(BigDecimal("4")) }
    assertNull(task.estimateQuality)
  }

  @Test
  fun `a summary task has no estimate quality`() {
    val summary = newTask()
    val child = taskManager.newTaskBuilder().withName("c").withParent(summary).build()
    summary.edit { setOriginalEffort(BigDecimal("9")) }
    child.edit { setOriginalEffort(BigDecimal("9")) }
    assertNull(summary.estimateQuality, "the effort of a summary task is not its own")
    assertEquals(BigDecimal("9"), child.estimateQuality!!.originalHours)
  }

  @Test
  fun `task loader reads the original effort and loads none for a task without one`() {
    val xmlTask = XmlTasks.XmlTask(id = 1, uid = "", name = "T", startDate = "2026-11-09").also {
      it.originalEffort = BigDecimal("16.25")
    }
    val loader = TaskLoader(taskManager, SimpleTreeCollapseView())
    assertEquals(BigDecimal("16.25"), loader.loadTask(null, xmlTask).originalEffort)

    val plain = XmlTasks.XmlTask(id = 2, uid = "", name = "U", startDate = "2026-11-09").also {
      it.effort = BigDecimal("4")
    }
    assertNull(loader.loadTask(null, plain).originalEffort, "loading must not invent an original effort")
  }
}
