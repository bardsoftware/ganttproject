package biz.ganttproject.ganttview

import biz.ganttproject.app.GPTreeTableView
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.scene.Parent
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ProjectEventListener
import net.sourceforge.ganttproject.task.FacadeImpl
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTable(
  private val project: IGanttProject,
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

      override fun taskMoved(e: TaskHierarchyEvent?) {
        reload()
      }
    })
    GlobalScope.launch(Dispatchers.JavaFx) {
      treeTable.isShowRoot = false
      buildColumns()
    }
    project.addProjectEventListener(object : ProjectEventListener {
      override fun projectModified() {}
      override fun projectSaved() {}
      override fun projectClosed() {}
      override fun projectCreated() {}

      override fun projectOpened() {
        reload()
      }
    })

  }

  fun buildColumns() {
    for (idx in 0 until columnList.size) {
      val column = columnList.getField(idx)
      if (column.isVisible || true) {
        TaskDefaultColumn.find(column.id)?.let { taskDefaultColumn ->

          when {
            taskDefaultColumn.valueClass == java.lang.String::class.java -> {
              TreeTableColumn<Task, String>(column.name).apply {
                setCellValueFactory {
                  ReadOnlyStringWrapper(taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal).toString())
                }
              }
            }
            GregorianCalendar::class.java.isAssignableFrom(taskDefaultColumn.valueClass) -> {
              TreeTableColumn<Task, GregorianCalendar>(column.name).apply {
                setCellValueFactory {
                  ReadOnlyObjectWrapper(
                    (taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal) as GregorianCalendar)
                  )
                }
              }
            }
            else -> null
          }?.let {
            treeTable.columns.add(it)
          }
        }
      }
    }
  }

  fun reload() = GlobalScope.launch(Dispatchers.JavaFx) {
    treeTable.columns.clear()
    buildColumns()

    val treeModel = taskManager.taskHierarchy
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
