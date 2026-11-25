/*
Copyright 2026 BarD Software s.r.o.

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
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.ganttview.TaskTableModel
import biz.ganttproject.lib.fx.SimpleTreeCollapseView
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.parser.TaskLoader
import net.sourceforge.ganttproject.storage.ProjectDatabase.TaskUpdateBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
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
  fun `the mutator passes effort and the flag to the task update builder`() {
    val calls = mutableListOf<String>()
    val updateBuilder = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(TaskUpdateBuilder::class.java)) { _, method, args ->
      calls.add("${method.name}${args?.toList() ?: emptyList<Any?>()}")
      null
    } as TaskUpdateBuilder
    val taskManager = TestSetupHelper.newTaskManagerBuilder().also {
      it.setTaskUpdateBuilderFactory { updateBuilder }
    }.build()
    val task = taskManager.newTaskBuilder().withName("t").build()
    task.createMutator().also {
      it.setEffort(BigDecimal("12.5"))
      it.setEffortDriven(true)
      it.commit()
    }
    assertTrue("setEffort[null, 12.5]" in calls, "calls: $calls")
    assertTrue("setEffortDriven[false, true]" in calls, "calls: $calls")
  }

  @Test
  fun `the effort cell sets the effort and a blank cell clears it`() {
    val model = TaskTableModel(taskManager.customPropertyManager)
    val task = taskManager.newTaskBuilder().withName("t").build()
    model.setValue(BigDecimal("12.5"), task, TaskDefaultColumn.EFFORT)
    assertEquals(BigDecimal("12.5"), task.effort)
    model.setValue(null, task, TaskDefaultColumn.EFFORT)
    assertNull(task.effort)
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
  fun `importing tasks, as pasting from the clipboard does, keeps effort and the flag`() {
    val source = TestSetupHelper.newTaskManagerBuilder().build()
    source.newTaskBuilder().withName("t").build().createMutator().also {
      it.setEffort(BigDecimal("12.5"))
      it.setEffortDriven(true)
      it.commit()
    }
    taskManager.importData(source, emptyMap())
    val imported = taskManager.tasks.single()
    assertEquals(BigDecimal("12.5"), imported.effort)
    assertTrue(imported.isEffortDriven)
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
