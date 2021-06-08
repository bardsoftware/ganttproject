package biz.ganttproject.ganttview

import biz.ganttproject.app.*
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.core.table.ColumnList.ColumnStub
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.task.TaskActions
import javafx.application.Platform
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.Parent
import javafx.scene.control.SelectionMode
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.cell.TextFieldTreeTableCell
import javafx.scene.input.KeyCode
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.DefaultStringConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.CustomPropertyManager
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
import javax.swing.SwingUtilities

/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTable(
  private val project: IGanttProject,
  private val taskManager: TaskManager,
  private val taskTableChartConnector: TaskTableChartConnector,
  private val treeCollapseView: TreeCollapseView<Task>,
  private val selectionManager: TaskSelectionManager,
  private val taskActions: TaskActions
) {
  val headerHeightProperty: ReadOnlyDoubleProperty get() = treeTable.headerHeight
  private val treeModel = taskManager.taskHierarchy
  private val rootItem = TreeItem(treeModel.rootTask)
  private val treeTable = GPTreeTableView<Task>(rootItem)
  private val taskTableModel = TaskTableModel(taskManager, taskManager.customPropertyManager)
  private val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()

  val control: Parent get() = treeTable
  val actionConnector by lazy {
    TaskTableActionConnector(
      commitEdit = {},
      runKeepingExpansion = { task, code -> code(task) },
      scrollTo = {}
    )
  }
  private val columns = FXCollections.observableArrayList(TaskDefaultColumn.getColumnStubs().map { ColumnStub(it) }.toList())
  val columnList: ColumnList = ColumnListImpl(columns, taskManager.customPropertyManager) { treeTable.columns }
  var requestSwingFocus: () -> Unit = {}
  init {
    initTaskEventHandlers()
    GlobalScope.launch(Dispatchers.JavaFx) {
      treeTable.isShowRoot = false
      treeTable.isEditable = true
      treeTable.isTableMenuButtonVisible = true
      treeTable.columns.clear()
      buildColumns(0, columns)
      reload()
    }
    initProjectEventHandlers()
    initChartConnector()
    initKeyboardEventHandlers()
    treeTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
    treeTable.selectionModel.selectedItems.addListener(ListChangeListener {  c ->
      SwingUtilities.invokeLater {
        selectionManager.selectedTasks = treeTable.selectionModel.selectedItems.map { it.value }
      }
    })
    treeTable.focusModel.focusedCellProperty().addListener { observable, oldValue, newValue ->
      if (newValue.column == -1) {
        treeTable.focusModel.focus(newValue.row, findNameColumn())
      }
    }
    treeTable.onSort = EventHandler {
      GlobalScope.launch(Dispatchers.JavaFx) {
        if (treeTable.sortOrder.isEmpty()) {
          reload()
        } else {
          taskTableChartConnector.visibleTasks.clear()
          getExpandedTasks().also {
            taskTableChartConnector.visibleTasks.addAll(it)
          }
        }
      }
    }
    columns.addListener(ListChangeListener { change ->
      GlobalScope.launch(Dispatchers.JavaFx) {
        while (change.next()) {
          change.removed.forEach { column ->
            treeTable.columns.removeIf { (it.userData as ColumnStub).id == column.id }
          }
          if (change.wasAdded()) {
            buildColumns(change.from, change.addedSubList)
          }
        }
      }
    })
  }

  private fun initKeyboardEventHandlers() {
    treeTable.onKeyPressed = EventHandler { event ->
      val action = taskActions.all().firstOrNull { action ->
        action.triggeredBy(event)
      }
      if (action != null) {
        action.actionPerformed(null)
      } else {
        if (event.getModifiers() == 0) {
          when (event.code) {
            KeyCode.LEFT -> treeTable.focusModel.focusLeftCell()
            KeyCode.RIGHT -> treeTable.focusModel.focusRightCell()
          }
        }
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
      override fun projectModified() {
      }
      override fun projectSaved() {}
      override fun projectClosed() {
        reload()
      }
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
        if (e.taskSource == TaskManager.EventSource.USER) {
          val nameColumn = findNameColumn()
          Platform.runLater {
            treeTable.selectionModel.clearSelection()
            val treeItem = e.newContainer.addChildTreeItem(e.task, e.indexAtNew)
            taskTableChartConnector.visibleTasks.clear()
            taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
            treeTable.selectionModel.select(treeItem)
            treeTable.edit(treeTable.getRow(treeItem), nameColumn)
            requestSwingFocus()
          }
        } else {
          keepSelection {
            e.newContainer.addChildTreeItem(e.task, e.indexAtNew)
            taskTableChartConnector.visibleTasks.clear()
            taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
          }
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
          task2treeItem[e.newContainer]?.children?.add(e.indexAtNew, taskTreeItem).also {
          }// ?: run {println("new container not found")}
          taskTableChartConnector.visibleTasks.clear()
          taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
        }
      }

      override fun taskRemoved(e: TaskHierarchyEvent) {
        keepSelection {
          task2treeItem[e.oldContainer]?.let {
            val idx = it.children.indexOfFirst { it.value == e.task }
            if (idx >= 0) {
              it.children.removeAt(idx)
            }
          }
          task2treeItem.remove(e.task)
          taskTableChartConnector.visibleTasks.clear()
          taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
        }
      }
    })
  }

  private fun findNameColumn() = treeTable.columns.find { (it.userData as ColumnStub).id == TaskDefaultColumn.NAME.stub.id }

  private fun buildColumns(idxStart: Int, columns: List<ColumnList.Column>) {
    for (idx in 0 until columns.size) {
      val column = columns[idx]
        TaskDefaultColumn.find(column.id)?.let { taskDefaultColumn ->
          when {
            taskDefaultColumn.valueClass == java.lang.String::class.java -> {
              TreeTableColumn<Task, String>(taskDefaultColumn.getName()).apply {
                setCellValueFactory {
                  ReadOnlyStringWrapper(taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal).toString())
                }
                cellFactory = Callback { TextCell(DefaultStringConverter()) }
                if (taskDefaultColumn == TaskDefaultColumn.NAME) {
                  treeTable.treeColumn = this
                }
                onEditCommit = EventHandler { event ->
                  taskTableModel.setValue(event.newValue, event.rowValue.value, taskDefaultColumn.ordinal)
                }
              }
            }
            GregorianCalendar::class.java.isAssignableFrom(taskDefaultColumn.valueClass) -> {
              TreeTableColumn<Task, GanttCalendar>(taskDefaultColumn.getName()).apply {
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
              TreeTableColumn<Task, Number>(taskDefaultColumn.getName()).apply {
                setCellValueFactory {
                  if (taskDefaultColumn == TaskDefaultColumn.DURATION) {
                    ReadOnlyIntegerWrapper((taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal) as TimeDuration).length)
                  } else {
                    ReadOnlyIntegerWrapper(taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal) as Int)
                  }
                }
              }
            }
            else -> TreeTableColumn<Task, String>(taskDefaultColumn.getName()).apply {
              setCellValueFactory {
                ReadOnlyStringWrapper(taskTableModel.getValueAt(it.value.value, taskDefaultColumn.ordinal).toString())
              }
            }
          }?.let {
            treeTable.columns.add(idxStart + idx, it)
            it.isEditable = taskDefaultColumn.isEditable(null)
            it.isVisible = column.isVisible
            it.userData = column
          }
        }
    }
  }

  fun reload() {
    Platform.runLater {
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
  }

  private fun Task.addChildTreeItem(child: Task, pos: Int = -1): TreeItem<Task> {
    val parentItem = task2treeItem[this]!!
    val childItem = createTreeItem(child)
    if (pos == -1) {
      parentItem.children.add(childItem)
    } else {
      parentItem.children.add(pos, childItem)
    }
    task2treeItem[child] = childItem
    return childItem
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

  private fun keepSelection(code: ()->Unit) {
    Platform.runLater {
      val selectedTasks = treeTable.selectionModel.selectedItems.map { it.value to (it.previousSibling() ?: it.parent) }.toMap()
      code()
      treeTable.selectionModel.clearSelection()
      selectedTasks.forEach { task, parentTreeItem ->
        val whatSelect = task2treeItem[task] ?: parentTreeItem
        treeTable.selectionModel.select(whatSelect)
      }
      treeTable.requestFocus()
    }
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

class ColumnListImpl(
  private val columnList: MutableList<ColumnStub>,
  private val customPropertyManager: CustomPropertyManager,
  private val tableColumns: () -> List<TreeTableColumn<*,*>>
) : ColumnList {

  override fun getSize(): Int = columnList.size

  override fun getField(index: Int): ColumnList.Column = columnList[index]

  override fun clear() = columnList.clear()

  override fun add(id: String, order: Int, width: Int) {
    (this.columnList.firstOrNull { it.id == id } ?: run {
      customPropertyManager.getCustomPropertyDefinition(id)?.let { def ->
        ColumnStub(id, def.name, true,
          if (order == -1) tableColumns().count { it.isVisible } else order,
          if (width == -1) 75 else width
        )
      }
    })?.let {
      this.columnList.add(it)
    }
  }

  override fun importData(source: ColumnList, keepVisibleColumns: Boolean) {
    val remainVisible = if (keepVisibleColumns) {
      tableColumns().filter { it.isVisible }.map { it.userData as ColumnStub }
    } else emptyList()

    var importedList = source.copyOf()
    remainVisible.forEach { old -> importedList.firstOrNull { new -> new.id == old.id }?.isVisible = true }
    if (importedList.firstOrNull { it.isVisible } == null) {
      importedList = ColumnList.Immutable.fromList(TaskDefaultColumn.getColumnStubs()).copyOf()
    }
    importedList = importedList.sortedWith { left, right ->
      val test1 = (if (left.isVisible) -1 else 0) + if (right.isVisible) 1 else 0
      when {
        test1 != 0 -> test1
        !left.isVisible && !right.isVisible && left.name != null && right.name != null -> {
          left.name.compareTo(right.name)
        }
        left.order == right.order -> left.id.compareTo(right.id)
        else -> left.order - right.order
      }
    }
    columnList.clear()
    columnList.addAll(importedList)
  }
}

fun ColumnList.copyOf(): List<ColumnStub> {
  val importedList = mutableListOf<ColumnStub>()
  for (i in 0 until this.size) {
    val foreign = this.getField(i)
    importedList.add(ColumnStub(foreign))
  }
  return importedList
}

