/*
Copyright 2026 Dmitry Barashev,  BarD Software s.r.o

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
package net.sourceforge.ganttproject.chart.gantt

import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.export.ConsoleUIFacade
import net.sourceforge.ganttproject.importer.BufferProject
import net.sourceforge.ganttproject.resource.HumanResourceMerger
import net.sourceforge.ganttproject.resource.MergeResourcesEnum
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests placing the tasks pasted from another project after the selected task.
 * See https://github.com/bardsoftware/ganttproject/issues/2861
 */
class MoveImportedTasksTest {
  @Test fun testMoveAfterNestedTask() {
    val project = GanttProjectImpl()
    val taskManager = project.taskManager
    val summary = taskManager.newTaskBuilder().withName("summary").build()
    val child1 = taskManager.newTaskBuilder().withName("child1").withParent(summary).build()
    taskManager.newTaskBuilder().withName("child2").withParent(summary).build()
    taskManager.newTaskBuilder().withName("task2").build()

    val bufferProject = BufferProject(project, ConsoleUIFacade(null))
    val bufferTaskManager = bufferProject.taskManager
    val pasted1 = bufferTaskManager.newTaskBuilder().withName("pasted1").build()
    bufferTaskManager.newTaskBuilder().withName("pasted1.1").withParent(pasted1).build()
    bufferTaskManager.newTaskBuilder().withName("pasted2").build()

    val mergeOption = HumanResourceMerger.MergeResourcesOption().also { it.setSelectedValue(MergeResourcesEnum.NO) }
    val taskMapping = project.importProject(bufferProject, mergeOption, null, false)
    val moved = moveImportedTasksAfter(child1, bufferProject, taskMapping)

    assertEquals(listOf("pasted1", "pasted2"), moved.map { it.name })
    val hierarchy = taskManager.taskHierarchy
    assertEquals(listOf("child1", "pasted1", "pasted2", "child2"), hierarchy.getNestedTasks(summary).map { it.name })
    assertEquals(listOf("pasted1.1"), hierarchy.getNestedTasks(moved[0]).map { it.name })
    assertEquals(listOf("summary", "task2"), hierarchy.getNestedTasks(taskManager.rootTask).map { it.name })
  }
}
