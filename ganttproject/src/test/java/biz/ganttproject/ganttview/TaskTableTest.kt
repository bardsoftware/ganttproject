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