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

class TestEffortFields {
  private val taskManager = TestSetupHelper.newTaskManagerBuilder().build()

  @Test
  fun `a new task has no effort and is not effort driven`() {
    val task = taskManager.newTaskBuilder().withName("t").build()
    assertNull(task.effort)
    assertFalse(task.isEffortDriven)
  }

  @Test
  fun `effort and the flag are set through the mutator`() {
    val task = taskManager.newTaskBuilder().withName("t").build()
    task.createMutator().also {
      it.setEffort(BigDecimal("12.5"))
      it.setEffortDriven(true)
      it.commit()
    }
    assertEquals(BigDecimal("12.5"), task.effort)
    assertTrue(task.isEffortDriven)

    task.createMutator().also {
      it.setEffort(null)
      it.commit()
    }
    assertNull(task.effort, "effort has to be clearable")
  }

  @Test
  fun `a task created from a prototype takes effort and the flag along`() {
    val prototype = taskManager.newTaskBuilder().withName("proto").build()
    prototype.createMutator().also {
      it.setEffort(BigDecimal("40"))
      it.setEffortDriven(true)
      it.commit()
    }
    val copy = taskManager.newTaskBuilder().withPrototype(prototype).build()
    assertEquals(BigDecimal("40"), copy.effort)
    assertTrue(copy.isEffortDriven)
  }

  @Test
  fun `the unplugged clone keeps effort and the flag`() {
    val task = taskManager.newTaskBuilder().withName("t").build()
    task.createMutator().also {
      it.setEffort(BigDecimal("8"))
      it.setEffortDriven(true)
      it.commit()
    }
    val clone = task.unpluggedClone()
    assertEquals(BigDecimal("8"), clone.effort)
    assertTrue(clone.isEffortDriven)
  }

  @Test
  fun `task loader reads effort and the flag`() {
    val xmlTask = XmlTasks.XmlTask(id = 1, uid = "", name = "T", startDate = "2026-11-09").also {
      it.effort = BigDecimal("16.25")
      it.isEffortDriven = true
    }
    val task = TaskLoader(taskManager, SimpleTreeCollapseView()).loadTask(null, xmlTask)
    assertEquals(BigDecimal("16.25"), task.effort)
    assertTrue(task.isEffortDriven)
  }
}
