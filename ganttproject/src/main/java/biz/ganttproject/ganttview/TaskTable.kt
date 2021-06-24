/*
Copyright 2021 BarD Software s.r.o

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

import biz.ganttproject.app.MenuBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.getModifiers
import biz.ganttproject.app.triggeredBy
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.core.table.ColumnList.ColumnStub
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.lib.fx.*
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
import javafx.scene.control.cell.CheckBoxTreeTableCell
import javafx.scene.input.*
import javafx.scene.layout.Region
import javafx.util.Callback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.*
import net.sourceforge.ganttproject.chart.gantt.ClipboardContents
import net.sourceforge.ganttproject.chart.gantt.ClipboardTaskProcessor
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskSelectionManager
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.util.*
import java.util.List.copyOf
import kotlin.math.floor


/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTable(
  private val project: IGanttProject,
  private val taskManager: TaskManager,
  private val taskTableChartConnector: TaskTableChartConnector,
  private val treeCollapseView: TreeCollapseView<Task>,
  private val selectionManager: TaskSelectionManager,
  private val taskActions: TaskActions,
  private val undoManager: GPUndoManager,
  private val observableDocument: ReadOnlyObjectProperty<Document>
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
      scrollTo = {},
      columnList = { columnList }
    )
  }
  private val columns: ObservableList<ColumnList.Column> = FXCollections.observableArrayList(
    TaskDefaultColumn.getColumnStubs().map { ColumnStub(it) }.toList()
  )
  val columnList: ColumnListImpl = ColumnListImpl(columns, taskManager.customPropertyManager,
    { treeTable.columns },
    { onColumnsChange() })
  val columnListWidthProperty = columnList.totalWidthProperty
  var requestSwingFocus: () -> Unit = {}
  val newTaskActor = NewTaskActor().also { it.start() }


  init {
    Platform.runLater {
      treeTable.isShowRoot = false
      treeTable.isEditable = true
      treeTable.isTableMenuButtonVisible = true
      treeTable.columns.clear()
      buildColumns(columnList.columns())
      reload()
    }
    initTaskEventHandlers()
    initProjectEventHandlers()
    initChartConnector()
    initKeyboardEventHandlers()
    treeTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
    treeTable.selectionModel.selectedItems.addListener(ListChangeListener {
      val selectedItems = copyOf(treeTable.selectionModel.selectedItems)
      selectionManager.selectedTasks = selectedItems
        .map { it.value }
        .filter { it.manager.taskHierarchy.contains(it) }
    })
    treeTable.onSort = EventHandler {
      if (treeTable.sortOrder.isEmpty()) {
        reload()
      } else {
        taskTableChartConnector.visibleTasks.clear()
        getExpandedTasks().also {
          taskTableChartConnector.visibleTasks.addAll(it)
        }
      }
    }
    columns.addListener(ListChangeListener {
      onColumnsChange()
    })
    initNewTaskActor()
    treeTable.contextMenuActions = this::contextMenuActions
    treeTable.tableMenuActions = this::tableMenuActions
  }

  private fun onColumnsChange() = Platform.runLater {
    buildColumns(columnList.columns())
  }

  private fun initKeyboardEventHandlers() {
    treeTable.onKeyPressed = EventHandler { event ->
      val action = taskActions.all().firstOrNull { action ->
        action.triggeredBy(event)
      }
      if (action != null) {
        undoManager.undoableEdit(action.name) {
          action.actionPerformed(null)
        }
      } else {
        if (event.getModifiers() == 0) {
          when (event.code) {
            KeyCode.LEFT -> treeTable.focusModel.focusLeftCell()
            KeyCode.RIGHT -> treeTable.focusModel.focusRightCell()
            else -> {}
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
      Platform.runLater {
        treeTable.scrollBy(newValue.toDouble())
      }
    }
    treeTable.addScrollListener { newValue ->
      taskTableChartConnector.tableScrollOffset.value = newValue
    }

  }

  private fun initProjectEventHandlers() {
    project.addProjectEventListener(object : ProjectEventListener.Stub() {
      override fun projectRestoring(completion: CompletionPromise<Document>) {
        completion.await {
          sync()
        }
      }

      override fun projectOpened() {
        reload()
      }

      override fun projectCreated() {
        reload()
      }
    })
  }

  private fun initTaskEventHandlers() {
    taskManager.addTaskListener(object : TaskListenerAdapter() {
      override fun taskPropertiesChanged(e: TaskPropertyEvent) {
        Platform.runLater { treeTable.refresh() }
      }

      override fun taskAdded(e: TaskHierarchyEvent) {
        if (e.taskSource == TaskManager.EventSource.USER) {
          runBlocking { newTaskActor.inboxChannel.send(TaskReady(e.task)) }
          CoroutineScope(Dispatchers.JavaFx).launch {
            treeTable.selectionModel.clearSelection()
            val treeItem = e.newContainer.addChildTreeItem(e.task, e.indexAtNew)
            taskTableChartConnector.visibleTasks.clear()
            taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
            treeTable.selectionModel.select(treeItem)
            newTaskActor.inboxChannel.send(TreeItemReady(treeItem))
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
          val taskTreeItem = task2treeItem[e.oldContainer]?.let { containerItem ->
            val idx = containerItem.children.indexOfFirst { it.value == e.task }
            if (idx >= 0) {
              containerItem.children.removeAt(idx)
            } else {
              null
            }
          } ?: return@keepSelection
          task2treeItem[e.newContainer]?.children?.add(e.indexAtNew, taskTreeItem)
          taskTableChartConnector.visibleTasks.clear()
          taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
        }
      }

      override fun taskRemoved(e: TaskHierarchyEvent) {
        keepSelection {
          task2treeItem[e.oldContainer]?.let {
            it.depthFirstWalk { treeItem ->
              task2treeItem.remove(treeItem.value)
              true
            }
            val idx = it.children.indexOfFirst { it.value == e.task }
            if (idx >= 0) {
              it.children.removeAt(idx)
            }
          }
          taskTableChartConnector.visibleTasks.setAll(getExpandedTasks())
        }
      }
    })
  }

  // Launches a coroutine which is reading command messages from the New Task Actor and
  // starts or commits tree cell edits.
  private fun initNewTaskActor() {
    CoroutineScope(Dispatchers.JavaFx).launch {
      for (cmd in newTaskActor.commandChannel) {
        when (cmd) {
          is StartEditing -> {
            requestSwingFocus()
            if (treeTable.editingCell == null) {
              treeTable.edit(treeTable.getRow(cmd.treeItem), findNameColumn())
            }
          }
          is CommitEditing -> {
            ourNameCellFactory.editingCell?.commitEdit()
            newTaskActor.inboxChannel.send(EditingCompleted())
          }
        }
      }
    }
  }
  private fun findNameColumn() = treeTable.columns.find { (it.userData as ColumnList.Column).id == TaskDefaultColumn.NAME.stub.id }

  private fun buildColumns(columns: List<ColumnList.Column>) {
    val tableColumns =
      columns.mapNotNull { column ->
        TaskDefaultColumn.find(column.id)?.let { taskDefaultColumn ->
          createDefaultColumn(column, taskDefaultColumn)
        } ?: createCustomColumn(column)
      }.toList()
    println("Expecting content width=${floor(columnList.totalWidth.toDouble())}")
    (treeTable.skin as GPTreeTableViewSkin<*>).onContentWidthChange(floor(columnList.totalWidth.toDouble())) {
      println("received!")
        tableColumns.forEach {
          it.minWidth = 0.0
          treeTable.minWidth = 0.0
          (treeTable.lookup(".virtual-flow") as Region).minWidth = 0.0
        }
    }
    (treeTable.lookup(".virtual-flow") as Region).minWidth = columnList.totalWidth.toDouble()
    treeTable.columns.setAll(tableColumns)
  }

  private fun createDefaultColumn(column: ColumnList.Column, taskDefaultColumn: TaskDefaultColumn) =
    when {
      taskDefaultColumn.valueClass == java.lang.String::class.java -> {
        createTextColumn<Task>(taskDefaultColumn.getName(),
          { taskTableModel.getValueAt(it, taskDefaultColumn).toString() },
          { task, value -> taskTableModel.setValue(value, task, taskDefaultColumn) }
        ).apply {
          if (taskDefaultColumn == TaskDefaultColumn.NAME) {
            treeTable.treeColumn = this
            cellFactory = ourNameCellFactory
          }
        }
      }
      GregorianCalendar::class.java.isAssignableFrom(taskDefaultColumn.valueClass) -> {
        createDateColumn(taskDefaultColumn.getName(),
          { taskTableModel.getValueAt(it, taskDefaultColumn) as GanttCalendar? },
          { task, value -> taskTableModel.setValue(value, task, taskDefaultColumn) }
        )
      }
      taskDefaultColumn.valueClass == Integer::class.java -> {
        createIntegerColumn(taskDefaultColumn.getName(),
          {
            if (taskDefaultColumn == TaskDefaultColumn.DURATION) {
              (taskTableModel.getValueAt(it, taskDefaultColumn) as TimeDuration).length
            } else {
              taskTableModel.getValueAt(it, taskDefaultColumn) as Int
            }
          },
          { task, value -> taskTableModel.setValue(value, task, taskDefaultColumn) }
        )
      }
      else -> TreeTableColumn<Task, String>(taskDefaultColumn.getName()).apply {
        setCellValueFactory {
          ReadOnlyStringWrapper(taskTableModel.getValueAt(it.value.value, taskDefaultColumn).toString())
        }
      }
    }.also {
      it.isEditable = taskDefaultColumn.isEditable(null)
      it.isVisible = column.isVisible
      it.userData = column
      // There are a few issues with the TreeTableView:
      // - if the sum total width of all columns exceed the table content width, the constrained
      // resize policy makes width of all columns the same
      // - the table and its contents resize only to the specified width, but it happens after resizing the
      // columns. In the result we get a table with equal to the total column width, but the columns become
      // equal-width.
      // The solution is to set the min width of the columns. It shall be cleared afterwards, otherwise
      // the columns become non-resizeable. We clear the min width when we receive table's contentWidth property
      // change.
      it.prefWidth = column.width.toDouble()
      it.minWidth = column.width.toDouble()
    }

  private fun createCustomColumn(column: ColumnList.Column): TreeTableColumn<Task, *>? {
    val customProperty = taskManager.customPropertyManager.getCustomPropertyDefinition(column.id) ?: return null
    return when (customProperty.propertyClass) {
      CustomPropertyClass.TEXT -> {
        createTextColumn(customProperty.name,
          { taskTableModel.getValue(it, customProperty)?.toString() },
          { task, value -> taskTableModel.setValue(value, task, customProperty) }
        )
      }
      CustomPropertyClass.BOOLEAN -> {
        TreeTableColumn<Task, Boolean>(customProperty.name).apply {
          setCellValueFactory {
            val task = it.value.value
            SimpleBooleanProperty((taskTableModel.getValue(task, customProperty) ?: false) as Boolean).also {
              it.addListener { _, _, newValue ->
                taskTableModel.setValue(newValue, task, customProperty)
              }
            }
          }
          cellFactory =  Callback { CheckBoxTreeTableCell() }
        }
      }
      CustomPropertyClass.INTEGER -> {
        createIntegerColumn(customProperty.name,
          { taskTableModel.getValue(it, customProperty) as Int },
          { task, value -> taskTableModel.setValue(value, task, customProperty) }
        )
      }
      CustomPropertyClass.DOUBLE -> {
        createDoubleColumn(customProperty.name,
          { taskTableModel.getValue(it, customProperty) as Double },
          { task, value -> taskTableModel.setValue(value, task, customProperty) }
        )
      }
      CustomPropertyClass.DATE -> {
        createDateColumn(customProperty.name,
          { taskTableModel.getValue(it, customProperty) as GanttCalendar? },
          { task, value -> taskTableModel.setValue(value, task, customProperty) }
        )
      }
    }?.also {
      it.isEditable = true
      it.isVisible = column.isVisible
      it.userData = column
      it.prefWidth = column.width.toDouble()
      it.minWidth = column.width.toDouble()
    }
  }

  fun reload() {
    Platform.runLater {
      val treeModel = taskManager.taskHierarchy
      treeTable.root.children.clear()
      task2treeItem.clear()
      task2treeItem[treeModel.rootTask] = rootItem

      treeModel.depthFirstWalk(treeModel.rootTask) { parent, child, _ ->
        if (child != null) parent.addChildTreeItem(child)
        true
      }
      taskTableChartConnector.visibleTasks.clear()
      taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
    }
  }

  fun sync() {
    keepSelection {
      val treeModel = taskManager.taskHierarchy
      task2treeItem.clear()
      task2treeItem[treeModel.rootTask] = rootItem
      treeModel.depthFirstWalk(treeModel.rootTask) { parent, child, idx ->
        if (child == null) {
          val parentItem = task2treeItem[parent]!!
          parentItem.children.remove(idx, parentItem.children.size)
        } else {
          val parentItem = task2treeItem[parent]!!
          if (parentItem.children.size > idx) {
            val childItem = parentItem.children[idx]
            if (childItem.value.taskID == child.taskID) {
              childItem.value = child
              task2treeItem[child] = childItem
            } else {
              parentItem.children.removeAt(idx)
              parent.addChildTreeItem(child, idx)
            }
          } else {
            parent.addChildTreeItem(child)
          }
        }
        true
      }
      taskTableChartConnector.visibleTasks.setAll(getExpandedTasks())
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
    it.expandedProperty().addListener { _, _, _ -> onExpanded() }
  }

  private fun onExpanded() {
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
      val selectedTasks =
        treeTable.selectionModel.selectedItems.associate { it.value to (it.previousSibling() ?: it.parent) }
      code()
      treeTable.selectionModel.clearSelection()
      for ((task, parentTreeItem) in selectedTasks) {
        val liveTask = taskManager.getTask(task.taskID)
        val whatSelect = task2treeItem[liveTask] ?: parentTreeItem
        treeTable.selectionModel.select(whatSelect)
      }
      treeTable.requestFocus()
    }
  }

  private fun contextMenuActions(builder: MenuBuilder) {
    builder.apply {
      items(taskActions.createAction)
    }
    if (selectionManager.selectedTasks.isNotEmpty()) {
      builder.apply {
        items(taskActions.propertiesAction)
        separator()
        items(
          taskActions.unindentAction,
          taskActions.indentAction,
          taskActions.moveUpAction,
          taskActions.moveDownAction,
        )
        separator()
        items(
          taskActions.copyAction,
          taskActions.cutAction,
          taskActions.pasteAction,
          taskActions.deleteAction
        )
        if (selectionManager.selectedTasks.size == 1) {
          separator()
          submenu(RootLocalizer.formatText("assignments")) {
            items(taskActions.assignments(selectionManager.selectedTasks[0], project.humanResourceManager, undoManager))
          }
        }
      }
    }
  }

  private fun tableMenuActions(builder: MenuBuilder) {
    builder.apply {
      items(taskActions.manageColumnsAction)
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
  val scrollTo: (task: Task) -> Unit,
  val columnList: () -> ColumnList
)

private fun TaskContainmentHierarchyFacade.depthFirstWalk(root: Task, visitor: (Task, Task?, Int) -> Boolean) {
  getNestedTasks(root).let { children ->
    children.forEachIndexed { idx, child ->
      if (visitor(root, child, idx)) {
        this.depthFirstWalk(child, visitor)
      }
    }
    visitor(root, null, children.size)
  }

}

private fun TreeItem<Task>.depthFirstWalk(visitor: (TreeItem<Task>) -> Boolean) {
  this.children.forEach { if (visitor(it)) it.depthFirstWalk(visitor) }
}

class ColumnListImpl(
  private val columnList: MutableList<ColumnList.Column>,
  private val customPropertyManager: CustomPropertyManager,
  private val tableColumns: () -> List<TreeTableColumn<*,*>>,
  private val onColumnChange: () -> Unit = {}
) : ColumnList {

  val totalWidth get()  = totalWidthProperty.value
  val totalWidthProperty = SimpleDoubleProperty()

  override fun getSize(): Int = columnList.size

  override fun getField(index: Int): ColumnList.Column = columnList[index]

  override fun clear() = synchronized(columnList) {
    columnList.clear()
    updateTotalWidth()
  }

  override fun add(id: String, order: Int, width: Int) {
    synchronized(columnList) {
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
      updateTotalWidth()
    }
  }

  override fun importData(source: ColumnList, keepVisibleColumns: Boolean) {
    val remainVisible = if (keepVisibleColumns) {
      tableColumns().filter { it.isVisible }.map { it.userData as ColumnList.Column }
    } else emptyList()

    var importedList = source.copyOf()
    remainVisible.forEach { old -> importedList.firstOrNull { new -> new.id == old.id }?.isVisible = true }
    if (importedList.firstOrNull { it.isVisible } == null) {
      importedList = ColumnList.Immutable.fromList(TaskDefaultColumn.getColumnStubs()).copyOf()
    }
    importedList = importedList.sortedWith { left, right ->
      // test1 places visible columns before invisible
      val test1 = (if (left.isVisible) -1 else 0) + if (right.isVisible) 1 else 0
      when {
        test1 != 0 -> test1
        // Invisible columns are compared by name (why?)
        !left.isVisible && !right.isVisible && left.name != null && right.name != null -> {
          left.name.compareTo(right.name)
        }
        // If both columns are visible and their order value happens to be the same (may happen when importing
        // one project into another) then we compare ids (what we do if they are equal too?)
        left.order == right.order -> left.id.compareTo(right.id)
        // Otherwise we use order value
        else -> left.order - right.order
      }
    }

    // Here we merge the imported list with the currently available one.
    // We maintain the invariant: the list prefix [0, idxImported) is the same in the imported list and
    // in the result list.
    synchronized(columnList) {
      val currentList = columnList.map { it as ColumnList.Column }.toMutableList()
      importedList.forEachIndexed { idxImported, column ->
        val idxCurrent = currentList.indexOfFirst { it.id == column.id }
        if (idxCurrent >= 0) {
          if (idxCurrent != idxImported) {
            // Because of the invariant, it can only be greater. We will remove all
            // the columns between the imported and existing. This may be an excessive measure
            // because the removed columns may be found later in the imported list, but that's ok.
            assert(idxCurrent > idxImported) {
              "Unexpected column indices: imported=$idxImported current=$idxCurrent for column=$column"
            }
            currentList.subList(idxImported, idxCurrent).clear()
          }
          if (currentList[idxImported] != column) {
            currentList[idxImported] = ColumnStub(column).also {
              it.setOnChange { onColumnChange() }
            }
          }
        } else {
          currentList.add(idxImported, ColumnStub(column).also {
            it.setOnChange { onColumnChange() }
          })
        }
        assert(currentList.subList(0, idxImported) == importedList.subList(0, idxImported))
      }
      // Finally clear the remaining tail.
      if (currentList.size > importedList.size) {
        currentList.subList(importedList.size, currentList.size).clear()
      }
      columnList.clear()
      columnList.addAll(currentList)
      updateTotalWidth()
    }
  }

  override fun exportData(): List<ColumnList.Column> {
    synchronized(columnList) {
      tableColumns().forEachIndexed { index, column ->
        (column.userData as ColumnList.Column).let { userData ->
          columnList.firstOrNull { it.id == userData.id }?.let {
            it.order = index
            it.width = column.width.toInt()
          }
        }
      }
      return copyOf()
    }
  }

  fun columns(): List<ColumnList.Column> {
    synchronized(columnList) {
      return copyOf()
    }
  }

  private fun updateTotalWidth() {
    totalWidthProperty.value = columnList.filter { it.isVisible  }.sumOf { it.width }.toDouble()
  }
}

fun ColumnList.copyOf(): List<ColumnList.Column> {
  val copy = mutableListOf<ColumnStub>()
  for (i in 0 until this.size) {
    copy.add(ColumnStub(this.getField(i)))
  }
  return copy
}

class DragAndDropSupport {
  private lateinit var clipboardContent: ClipboardContents
  private lateinit var clipboardProcessor: ClipboardTaskProcessor

  fun install(cell: TextCell<Task, String>) {
    cell.setOnDragDetected { event ->
      dragDetected(cell)
      event.consume()
    }
    cell.setOnDragOver { event -> dragOver(event, cell) }
    cell.setOnDragDropped { drop(cell) }
    cell.setOnDragExited {
    }
  }
  fun dragDetected(cell: TextCell<Task, String>) {
    val task = cell.treeTableRow.treeItem.value
    clipboardContent = ClipboardContents(task.manager).also {
      it.addTasks(listOf(task))
    }
    clipboardProcessor = ClipboardTaskProcessor(task.manager)
    val db = cell.startDragAndDrop(TransferMode.COPY)
    val content = ClipboardContent()
    content[TEXT_FORMAT] = cell.treeTableRow.treeItem.value.taskID
    db.setContent(content)
    db.dragView = cell.snapshot(null, null)
    cell.setOnDragExited { db.clear() }
  }

  fun dragOver(event: DragEvent, cell: TextCell<Task, String>) {
    if (!event.dragboard.hasContent(TEXT_FORMAT)) return
    val thisItem = cell.treeTableRow.treeItem

    if (!clipboardProcessor.canMove(thisItem.value, clipboardContent)) {
      clearDropLocation()
      return
    }

    event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE)
//    if (dropZone != treeCell) {
//      clearDropLocation()
//      this.dropZone = treeCell
//      dropZone.setStyle(DROP_HINT_STYLE)
//    }
  }

  private fun clearDropLocation() {
  }

  fun drop(cell: TextCell<Task, String>) {
    val dropTarget = cell.treeTableRow.treeItem.value
    clipboardContent.cut()
    clipboardProcessor.pasteAsChild(dropTarget, clipboardContent)
  }

}

private val dragAndDropSupport = DragAndDropSupport()
private val ourNameCellFactory = TextCellFactory<Task>() {
  dragAndDropSupport.install(it)
}

private val TEXT_FORMAT = DataFormat("text/ganttproject-task-node")
