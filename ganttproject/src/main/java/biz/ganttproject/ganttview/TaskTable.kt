package biz.ganttproject.ganttview

import biz.ganttproject.app.GPTreeTableView
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Parent
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.cell.TreeItemPropertyValueFactory
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskManagerImpl.FacadeImpl
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTable(
  private val taskManager: TaskManager,
  private val columnList: ColumnList) {
  private val treeModel = FacadeImpl(taskManager.rootTask)
  private val rootItem = TreeItem(treeModel.rootTask)
  private val treeTable = GPTreeTableView<Task>(rootItem)
  private val taskTableModel = TaskTableModel(taskManager)

  val headerHeight: Int get() = treeTable.headerHeight.toInt()
  val control: Parent get() = treeTable

  init {
    taskManager.addTaskListener(object : TaskListenerAdapter() {
      override fun taskModelReset() {
        reload()
      }

      override fun taskAdded(e: TaskHierarchyEvent?) {
        reload()
      }
    })
  }

  fun buildColumns() {
    for (idx in 0 until columnList.size) {
      val column = columnList.getField(idx)
      TaskDefaultColumn.find(column.id)?.let { taskDefaultColumn ->
        if (taskDefaultColumn.valueClass == java.lang.String::class.java) {
          TreeTableColumn<Task, String>(column.name).apply {
            setCellValueFactory {
              SimpleStringProperty(taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal).toString())
            }
          }.also {
            treeTable.columns.add(it)
          }
        }
      }

    }
  }

  fun reload() {
    treeTable.columns.clear()
    buildColumns()

    val treeModel = FacadeImpl(taskManager.rootTask)
    treeTable.root.children.clear()
    val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()
    task2treeItem[treeModel.rootTask] = rootItem
    treeModel.breadthFirstSearch(treeModel.rootTask) { pair ->
        if (pair?.first() == null) {
          return@breadthFirstSearch true
        }
        val parentItem = task2treeItem[pair.first()]!!
        val childItem = TreeItem(pair.second())
        parentItem.children.add(childItem)
        task2treeItem[pair.second()] = childItem
        true
    }
  }
}
