/*
Copyright 2025 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.ganttview

import javafx.scene.control.TreeItem
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TaskTableTest {
  @Test
  fun `empty task table sync`() {
    val taskModel = TestSetupHelper.newTaskManagerBuilder().build()
    val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()
    val filter: TaskFilterFxn = {_, _ -> false }
    val rootItem = TreeItem(taskModel.rootTask)

    val sync = SyncAlgorithm(taskModel.taskHierarchy, task2treeItem, rootItem, filter) {}
    sync.sync()
    assertEquals(1, task2treeItem.size)
    assertEquals(taskModel.rootTask, task2treeItem.keys.first())
  }

  @Test
  fun `add first task sync`() {
    val taskModel = TestSetupHelper.newTaskManagerBuilder().build()
    val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()
    val filter: TaskFilterFxn = {_, _ -> true }
    val rootItem = TreeItem(taskModel.rootTask)

    val sync = SyncAlgorithm(taskModel.taskHierarchy, task2treeItem, rootItem, filter) {}
    sync.sync()

    taskModel.newTaskBuilder().withName("Task0").withParent(taskModel.rootTask).build()
    sync.sync()
    assertEquals(2, task2treeItem.size)
  }

  @Test
  fun `indent task sync`() {
    val taskModel = TestSetupHelper.newTaskManagerBuilder().build()
    val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()
    val filter: TaskFilterFxn = {_, _ -> true }
    val rootItem = TreeItem(taskModel.rootTask)
    val task0 = taskModel.newTaskBuilder().withName("Task0").withParent(taskModel.rootTask).build()
    val task1 = taskModel.newTaskBuilder().withName("Task1").withParent(taskModel.rootTask).build()

    val sync = SyncAlgorithm(taskModel.taskHierarchy, task2treeItem, rootItem, filter) {}
    sync.sync()

    task1.move(task0)
    sync.sync()
    assertEquals(3, task2treeItem.size)
    assertEquals(task2treeItem[task0]!!, task2treeItem[task1]!!.parent)
    assertEquals(1, rootItem.children.size)
  }



}