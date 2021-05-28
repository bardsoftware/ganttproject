package biz.ganttproject.ganttview

import biz.ganttproject.app.GPTreeTableView
import biz.ganttproject.app.TreeCollapseView
import biz.ganttproject.app.triggeredBy
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.task.TaskActions
import javafx.beans.property.*
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.Parent
import javafx.scene.control.SelectionMode
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.cell.TextFieldTreeTableCell
import javafx.util.StringConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ProjectEventListener
import net.sourceforge.ganttproject.gui.UIUtil
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskSelectionManager
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTable(
  private val project: IGanttProject,
  private val taskManager: TaskManager,
  private val columnList: ColumnList,
  private val taskTableChartConnector: TaskTableChartConnector,
  private val treeCollapseView: TreeCollapseView<Task>,
  private val selectionManager: TaskSelectionManager,
  private val taskActions: TaskActions
) {
  private val treeModel = taskManager.taskHierarchy
  private val rootItem = TreeItem(treeModel.rootTask)
  private val treeTable = GPTreeTableView<Task>(rootItem)
  private val taskTableModel = TaskTableModel(taskManager, taskManager.customPropertyManager)
  private val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()

  val headerHeight: Int get() = treeTable.headerHeight.toInt()
  val control: Parent get() = treeTable
  val actionConnector by lazy {
    TaskTableActionConnector(
      commitEdit = {},
      runKeepingExpansion = { task, code -> code(task) },
      scrollTo = {}
    )
  }
  init {
    initTaskEventHandlers()
    GlobalScope.launch(Dispatchers.JavaFx) {
      treeTable.isShowRoot = false
      treeTable.isEditable = true
      treeTable.isTableMenuButtonVisible = true
      reload()
    }
    initProjectEventHandlers()
    initChartConnector()
    initKeyboardEventHandlers()
    treeTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
    treeTable.selectionModel.selectedItems.addListener(ListChangeListener {  c ->
      selectionManager.selectedTasks = treeTable.selectionModel.selectedItems.map { it.value }
    })
    treeTable.onSort = EventHandler {
      GlobalScope.launch(Dispatchers.JavaFx) {
        if (treeTable.sortOrder.isEmpty()) {
          reload()
        } else {
          taskTableChartConnector.visibleTasks.clear()
          getExpandedTasks().also {
            println(it)
            taskTableChartConnector.visibleTasks.addAll(it)
          }
        }
      }
    }
  }

  private fun initKeyboardEventHandlers() {
    treeTable.onKeyPressed = EventHandler { event ->
      taskActions.all().firstOrNull { action ->
        action.triggeredBy(event)
      }?.let {
        it.actionPerformed(null)
      }
    }
  }

  private fun initChartConnector() {
    if (taskTableChartConnector.rowHeight.get() == -1) {
      taskTableChartConnector.rowHeight.value = treeTable.fixedCellSize.toInt()
    }
    taskTableChartConnector.rowHeight.addListener { _, _, newValue ->
      if (newValue != treeTable.fixedCellSize && newValue.toInt() > 0) {
        treeTable.fixedCellSize = newValue.toDouble()
      }
    }
    taskTableChartConnector.chartScrollOffset.addListener { _, _, newValue ->
      GlobalScope.launch(Dispatchers.JavaFx) {
        treeTable.scrollBy(newValue.toDouble())
      }
    }
    treeTable.addScrollListener { newValue ->
      taskTableChartConnector.tableScrollOffset.value = newValue
    }
  }

  private fun initProjectEventHandlers() {
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

  private fun initTaskEventHandlers() {
    taskManager.addTaskListener(object : TaskListenerAdapter() {
      override fun taskPropertiesChanged(e: TaskPropertyEvent) {
        GlobalScope.launch(Dispatchers.JavaFx) { treeTable.refresh() }
      }

      override fun taskModelReset() {
        reload()
      }

      override fun taskAdded(e: TaskHierarchyEvent) {
        keepSelection {
          e.newContainer.addChildTreeItem(e.task)
          taskTableChartConnector.visibleTasks.clear()
          taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
        }
      }

      override fun taskMoved(e: TaskHierarchyEvent)  {
        keepSelection {
          val taskTreeItem = task2treeItem[e.oldContainer]?.let {
            val idx = it.children.indexOfFirst { it.value == e.task }
            if (idx >= 0) {
              it.children.removeAt(idx)
            } else {
              null
            }
          } ?: return@keepSelection
          task2treeItem[e.newContainer]?.let {
            it.children.add(e.indexAtNew, taskTreeItem)
          }
          taskTableChartConnector.visibleTasks.clear()
          taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
        }
      }

      override fun taskRemoved(e: TaskHierarchyEvent) {
        GlobalScope.launch(Dispatchers.JavaFx) {
          task2treeItem[e.oldContainer]?.let {
            val idx = it.children.indexOfFirst { it.value == e.task }
            if (idx >= 0) {
              it.children.removeAt(idx)
            }
          }
          taskTableChartConnector.visibleTasks.clear()
          taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
        }
      }
    })
  }

  private fun buildColumns() {
    for (idx in 0 until columnList.size) {
      val column = columnList.getField(idx)
        TaskDefaultColumn.find(column.id)?.let { taskDefaultColumn ->
          when {
            taskDefaultColumn.valueClass == java.lang.String::class.java -> {
              TreeTableColumn<Task, String>(column.name).apply {
                setCellValueFactory {
                  ReadOnlyStringWrapper(taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal).toString())
                }
                cellFactory = TextFieldTreeTableCell.forTreeTableColumn()
                if (taskDefaultColumn == TaskDefaultColumn.NAME) {
                  treeTable.treeColumn = this
                }
                onEditCommit = EventHandler { event ->
                  taskTableModel.setValue(event.newValue, event.rowValue.value, taskDefaultColumn.ordinal)
                }
              }
            }
            GregorianCalendar::class.java.isAssignableFrom(taskDefaultColumn.valueClass) -> {
              TreeTableColumn<Task, GanttCalendar>(column.name).apply {
                setCellValueFactory {
                  ReadOnlyObjectWrapper(
                    (taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal) as GanttCalendar)
                  )
                }
                val converter = GanttCalendarStringConverter()
                cellFactory = TextFieldTreeTableCell.forTreeTableColumn(converter)
                onEditCommit = EventHandler { event ->
                  taskTableModel.setValue(event.newValue, event.rowValue.value, taskDefaultColumn.ordinal)
                }
              }
            }
            taskDefaultColumn.valueClass == java.lang.Integer::class.java -> {
              TreeTableColumn<Task, Number>(column.name).apply {
                setCellValueFactory {
                  if (taskDefaultColumn == TaskDefaultColumn.DURATION) {
                    ReadOnlyIntegerWrapper((taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal) as TimeDuration).length)
                  } else {
                    ReadOnlyIntegerWrapper(taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal) as Int)
                  }
                }
              }
            }
            else -> null
          }?.let {
            it.isEditable = taskDefaultColumn.isEditable(null)
            treeTable.columns.add(it)
            it.isVisible = column.isVisible
          }
        }
    }
  }

  fun reload() = GlobalScope.launch(Dispatchers.JavaFx) {
    treeTable.columns.clear()
    buildColumns()

    val treeModel = taskManager.taskHierarchy
    treeTable.root.children.clear()
    task2treeItem.clear()
    task2treeItem[treeModel.rootTask] = rootItem

    treeModel.depthFirstWalk(treeModel.rootTask) { parent, child ->
      parent.addChildTreeItem(child)
      true
    }
    taskTableChartConnector.visibleTasks.clear()
    taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
  }

  private fun Task.addChildTreeItem(child: Task) {
    val parentItem = task2treeItem[this]!!
    val childItem = createTreeItem(child)
    parentItem.children.add(childItem)
    task2treeItem[child] = childItem
  }

  private fun createTreeItem(task: Task) = TreeItem(task).also {
    it.isExpanded = treeCollapseView.isExpanded(task)
    it.expandedProperty().addListener(this@TaskTable::onExpanded)
  }

  private fun onExpanded(observableValue: Any, old: Boolean, new: Boolean) {
    rootItem.depthFirstWalk { child ->
      treeCollapseView.setExpanded(child.value, child.isExpanded)
      true
    }
    taskTableChartConnector.visibleTasks.clear()
    taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
  }

  private fun getExpandedTasks(): List<Task> {
    val result = mutableListOf<Task>()
    rootItem.depthFirstWalk { child ->
      result.add(child.value)
      treeCollapseView.isExpanded(child.value)
    }
    return result
  }

  private fun keepSelection(code: ()->Unit) = GlobalScope.launch(Dispatchers.JavaFx) {
    val selectedTasks = treeTable.selectionModel.selectedItems.map { it.value }.toList()
    code()
    treeTable.selectionModel.clearSelection()
    val selectedItems = selectedTasks.map { task2treeItem[it] }
    selectedItems.mapNotNull { treeTable.selectionModel.select(it) }
    treeTable.requestFocus()
  }
}

data class TaskTableChartConnector(
  val rowHeight: IntegerProperty,
  val visibleTasks: ObservableList<Task>,
  val tableScrollOffset: DoubleProperty,
  var isTableScrollable: Boolean,
  val chartScrollOffset: DoubleProperty
)

data class TaskTableActionConnector(
  val commitEdit: ()->Unit,
  val runKeepingExpansion: ((task: Task, code: (Task)->Void) -> Unit),
  val scrollTo: (task: Task) -> Unit
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

class GanttCalendarStringConverter : StringConverter<GanttCalendar>() {
  private val validator = UIUtil.createStringDateValidator(null) {
    listOf(GanttLanguage.getInstance().shortDateFormat)
  }
  override fun toString(value: GanttCalendar): String = value.toString()

  override fun fromString(text: String) =
    CalendarFactory.createGanttCalendar(validator.parse(text))


}
