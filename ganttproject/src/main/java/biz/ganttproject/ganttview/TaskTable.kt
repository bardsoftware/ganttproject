package biz.ganttproject.ganttview

import biz.ganttproject.app.GPTreeTableView
import biz.ganttproject.app.TreeCollapseView
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.collections.ObservableList
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
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import org.jetbrains.annotations.NotNull
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTable(
  private val project: @NotNull IGanttProject,
  private val taskManager: @NotNull TaskManager,
  private val columnList: @NotNull ColumnList,
  private val taskTableChartSocket: TaskTableChartSocket,
  private val treeCollapseView: TreeCollapseView<Task>
) {
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
    if (taskTableChartSocket.rowHeight.get() == -1) {
      taskTableChartSocket.rowHeight.value = treeTable.fixedCellSize.toInt()
    }
    taskTableChartSocket.rowHeight.addListener { _, _, newValue ->
      if (newValue != treeTable.fixedCellSize && newValue.toInt() > 0) {
        treeTable.fixedCellSize = newValue.toDouble()
      }
    }
    treeTable.addScrollListener { newValue ->
      taskTableChartSocket.tableScrollOffset.value = newValue
    }
  }

  fun buildColumns() {
    for (idx in 0 until columnList.size) {
      val column = columnList.getField(idx)
      if (column.isVisible) {
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

    treeModel.depthFirstWalk(treeModel.rootTask) { parent, child ->
      val parentItem = task2treeItem[parent]!!
      val childItem = TreeItem(child).also {
        it.isExpanded = treeCollapseView.isExpanded(child)
        it.expandedProperty().addListener(this@TaskTable::onExpanded)
      }
      parentItem.children.add(childItem)
      task2treeItem[child] = childItem
      true
    }
    taskTableChartSocket.visibleTasks.clear()
    taskTableChartSocket.visibleTasks.addAll(getExpandedTasks())
  }

  private fun onExpanded(observableValue: Any, old: Boolean, new: Boolean) {
    rootItem.depthFirstWalk { child ->
      treeCollapseView.setExpanded(child.value, child.isExpanded)
      true
    }
    taskTableChartSocket.visibleTasks.clear()
    taskTableChartSocket.visibleTasks.addAll(getExpandedTasks())
  }

  private fun getExpandedTasks(): List<Task> {
    val result = mutableListOf<Task>()
    rootItem.depthFirstWalk { child ->
      result.add(child.value)
      treeCollapseView.isExpanded(child.value)
    }
    return result
  }
}

data class TaskTableChartSocket(
  val rowHeight: IntegerProperty,
  val visibleTasks: ObservableList<Task>,
  val tableScrollOffset: DoubleProperty
)

private fun TaskContainmentHierarchyFacade.depthFirstWalk(root: Task, visitor: (Task, Task) -> Boolean) {
  this.getNestedTasks(root).forEach { child ->
    if (visitor(root, child)) {
      this.depthFirstWalk(child, visitor)
    }
  }
}

private fun TreeItem<Task>.depthFirstWalk(visitor: (TreeItem<Task>) -> Boolean) {
  this.children.forEach { if (visitor(it)) it.depthFirstWalk(visitor) }
}
