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
import de.jensd.fx.glyphs.GlyphIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.ContentDisplay
import javafx.scene.control.SelectionMode
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.cell.CheckBoxTreeTableCell
import javafx.scene.input.*
import javafx.scene.layout.*
import javafx.scene.paint.Color.rgb
import javafx.scene.shape.Circle
import javafx.util.Callback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.*
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.chart.export.TreeTableApi
import net.sourceforge.ganttproject.chart.gantt.ClipboardContents
import net.sourceforge.ganttproject.chart.gantt.ClipboardTaskProcessor
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskSelectionManager
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.awt.Component
import java.math.BigDecimal
import java.util.*
import java.util.List.copyOf


/**
 * @author dbarashev@bardsoftware.com
 */
class TaskTable(
  val project: IGanttProject,
  private val taskManager: TaskManager,
  val taskTableChartConnector: TaskTableChartConnector,
  private val treeCollapseView: TreeCollapseView<Task>,
  private val selectionManager: TaskSelectionManager,
  private val taskActions: TaskActions,
  private val undoManager: GPUndoManager
) {
  val headerHeightProperty: ReadOnlyDoubleProperty get() = treeTable.headerHeight
  private val treeModel = taskManager.taskHierarchy
  val rootItem = TreeItem(treeModel.rootTask)
  val newTaskActor = NewTaskActor<Task>().also { it.start() }
  val treeTable = GPTreeTableView<Task>(rootItem)
  val taskTableModel = TaskTableModel(taskManager, taskManager.customPropertyManager)
  private val task2treeItem = mutableMapOf<Task, TreeItem<Task>>()

  val control: Parent get() = treeTable
  val actionConnector by lazy {
    TaskTableActionConnector(
      commitEdit = {},
      runKeepingExpansion = { task, code -> code(task) },
      scrollTo = {},
      columnList = { columnList },
      canAddTask = { newTaskActor.canAddTask },
      taskPropertiesAction = { taskActions.propertiesAction },
      contextMenuActions = this::contextMenuActions
    )
  }
  private val columns: ObservableList<ColumnList.Column> = FXCollections.observableArrayList()
  val columnList: ColumnListImpl = ColumnListImpl(columns, taskManager.customPropertyManager,
    { treeTable.columns },
    { onColumnsChange() },
    BuiltinColumns(
      isZeroWidth = {
        when (TaskDefaultColumn.find(it)) {
          TaskDefaultColumn.COLOR, TaskDefaultColumn.INFO -> true
          else -> false
        }
      },
      allColumns = {
        ColumnList.Immutable.fromList(TaskDefaultColumn.getColumnStubs()).copyOf()
      }
    )
  )
  val columnListWidthProperty = SimpleDoubleProperty()
  var requestSwingFocus: () -> Unit = {}
  lateinit var swingComponent: Component


  init {
    TaskDefaultColumn.setLocaleApi { key -> GanttLanguage.getInstance().getText(key) }

    columnList.totalWidthProperty.addListener { _, oldValue, newValue ->
      if (oldValue != newValue) {
        // We add vertical scroll bar width to the sum width of all columns, so that the split pane
        // which contains the table was resized appropriately.
        columnListWidthProperty.value = newValue.toDouble() + treeTable.vbarWidth()
      }
    }
    Platform.runLater {
      treeTable.isShowRoot = false
      treeTable.isEditable = true
      treeTable.isTableMenuButtonVisible = true
    }
    initTaskEventHandlers()
    initProjectEventHandlers()
    initChartConnector()
    initKeyboardEventHandlers()
    initSelectionListeners()
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

  fun loadDefaultColumns() = Platform.runLater {
    treeTable.columns.clear()
    columnList.importData(ColumnList.Immutable.fromList(TaskDefaultColumn.getColumnStubs().map { ColumnStub(it) }.toList()), false)
    buildColumns(columnList.columns())
    reload()
  }

  private fun onColumnsChange() = Platform.runLater {
    columnList.columns().forEach { it.taskDefaultColumn()?.isVisible = it.isVisible }
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
            KeyCode.LEFT -> {
              treeTable.focusModel.focusLeftCell()
              event.consume()
            }
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
    taskTableChartConnector.exportTreeTableApi = {
      TreeTableApi(
        rowHeight = { treeTable.fixedCellSize.toInt() },
        tableHeaderHeight = { treeTable.headerHeight.intValue() },
        width = { columnList.totalWidth.toInt() },
        tableHeaderComponent = { null },
        tableComponent = { null },
        tablePainter = { this.buildImage(it) }
      )
    }
    taskTableChartConnector.focus = {
      treeTable.requestFocus()
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
        loadDefaultColumns()
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
          task2treeItem[e.oldContainer]?.let { treeItem ->
            treeItem.depthFirstWalk { child ->
              task2treeItem.remove(child.value)
              true
            }
            val idx = treeItem.children.indexOfFirst { it.value == e.task }
            if (idx >= 0) {
              treeItem.children.removeAt(idx)
            }
          }
          taskTableChartConnector.visibleTasks.setAll(getExpandedTasks())
        }
      }

      override fun taskModelReset() {
        reload()
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
            } else {
              //println("editing cell is ${treeTable.editingCell}")
            }
          }
          is CommitEditing -> {
            commitEditing()
          }
        }
      }
    }
  }

  private suspend fun commitEditing() {
    ourNameCellFactory.editingCell?.commitEdit()
    newTaskActor.inboxChannel.send(EditingCompleted())
  }

  private fun initSelectionListeners() {
    this.treeTable.focusedProperty().addListener { _, oldValue, newValue ->
      if (newValue && newValue != oldValue) {
        this.selectionManager.userInputConsumer = this
      }
    }

    this.selectionManager.addSelectionListener(object : TaskSelectionManager.Listener {
      override fun selectionChanged(currentSelection: List<Task>) {
        if (this@TaskTable.selectionManager.userInputConsumer != this@TaskTable) {
          for (task in currentSelection) {
            task2treeItem[task]?.let { treeTable.selectionModel.select(it) }
          }
        }
      }

      override fun userInputConsumerChanged(newConsumer: Any?) {
        // TODO: commit editing
      }
    })
  }
  private fun findNameColumn() = treeTable.columns.find { (it.userData as ColumnList.Column).id == TaskDefaultColumn.NAME.stub.id }

  private fun buildColumns(columns: List<ColumnList.Column>) {
    val tableColumns =
      columns.mapNotNull { column ->
        when (val taskDefaultColumn = TaskDefaultColumn.find(column.id)) {
          TaskDefaultColumn.COLOR -> null
          null -> createCustomColumn(column)
          else -> createDefaultColumn(column, taskDefaultColumn)
        }?.also {
          it.prefWidth = column.width.toDouble()
        }
      }.toList()
    //(treeTable.lookup(".virtual-flow") as Region).minWidth = columnList.totalWidth.toDouble()
    treeTable.setColumns(tableColumns)
  }

  private fun createDefaultColumn(column: ColumnList.Column, taskDefaultColumn: TaskDefaultColumn) =
    when {
      taskDefaultColumn.valueClass == java.lang.String::class.java -> {
        if (taskDefaultColumn == TaskDefaultColumn.NAME) {
          TreeTableColumn<Task, Task>(taskDefaultColumn.getName()).apply {
            setCellValueFactory {
              ReadOnlyObjectWrapper(it.value.value)
            }
            cellFactory = ourNameCellFactory
            onEditCommit = EventHandler { event ->
              val targetTask: Task = event.rowValue.value
              val copyTask: Task = event.newValue
              taskTableModel.setValue(copyTask.name, targetTask, taskDefaultColumn)
              runBlocking { newTaskActor.inboxChannel.send(EditingCompleted()) }
            }
            onEditCancel = EventHandler {
              runBlocking { newTaskActor.inboxChannel.send(EditingCompleted()) }
            }
            treeTable.treeColumn = this
          }
        } else {
          createTextColumn(taskDefaultColumn.getName(),
            { taskTableModel.getValueAt(it, taskDefaultColumn).toString() },
            { task, value -> taskTableModel.setValue(value, task, taskDefaultColumn) }
          )
        }
      }
      GregorianCalendar::class.java.isAssignableFrom(taskDefaultColumn.valueClass) -> {
        createDateColumn(taskDefaultColumn.getName(),
          { taskTableModel.getValueAt(it, taskDefaultColumn) as GanttCalendar? },
          { task, value -> taskTableModel.setValue(value, task, taskDefaultColumn) }
        )
      }
      taskDefaultColumn.valueClass == java.lang.Integer::class.java -> {
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
      taskDefaultColumn.valueClass == java.lang.Double::class.java -> {
        createDoubleColumn(taskDefaultColumn.getName(),
          { taskTableModel.getValueAt(it, taskDefaultColumn) as Double },
          { task, value -> taskTableModel.setValue(value, task, taskDefaultColumn) }
        )
      }
      taskDefaultColumn.valueClass == java.math.BigDecimal::class.java -> {
        createDecimalColumn(taskDefaultColumn.getName(),
          { taskTableModel.getValueAt(it, taskDefaultColumn) as BigDecimal },
          { task, value -> taskTableModel.setValue(value, task, taskDefaultColumn) }
        )
      }
      taskDefaultColumn == TaskDefaultColumn.PRIORITY -> {
        createIconColumn(
          taskDefaultColumn.getName(),
          { taskTableModel.getValueAt(it, taskDefaultColumn) as Task.Priority},
          { priority: Task.Priority -> priority.getIcon() },
          RootLocalizer.createWithRootKey("priority")
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
      it.prefWidth = column.width.toDouble()
    }

  private fun Task.Priority.getIcon(): GlyphIcon<*>? = when (this) {
    Task.Priority.HIGHEST -> FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_UP)
    Task.Priority.HIGH -> FontAwesomeIconView(FontAwesomeIcon.ANGLE_UP)
    Task.Priority.NORMAL -> null
    Task.Priority.LOW -> FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOWN)
    Task.Priority.LOWEST -> FontAwesomeIconView(FontAwesomeIcon.ANGLE_DOUBLE_DOWN)
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
          setCellValueFactory { features ->
            val task = features.value.value
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
    }.also {
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
    val parentItem = task2treeItem[this] ?: run {
      println(task2treeItem)
      throw NullPointerException("NPE! this=$this")
    }
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
  val chartScrollOffset: DoubleProperty,
  var exportTreeTableApi: () -> TreeTableApi? = { null },
  var focus: () -> Unit = {}
)

data class TaskTableActionConnector(
  val commitEdit: ()->Unit,
  val runKeepingExpansion: ((task: Task, code: (Task)->Void) -> Unit),
  val scrollTo: (task: Task) -> Unit,
  val columnList: () -> ColumnList,
  val canAddTask: () -> ReadOnlyBooleanProperty,
  val taskPropertiesAction: () -> GPAction,
  val contextMenuActions: (MenuBuilder) -> Unit
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

fun TreeItem<Task>.depthFirstWalk(visitor: (TreeItem<Task>) -> Boolean) {
  this.children.forEach { if (visitor(it)) it.depthFirstWalk(visitor) }
}


class DragAndDropSupport {
  private lateinit var clipboardContent: ClipboardContents
  private lateinit var clipboardProcessor: ClipboardTaskProcessor

  fun install(cell: TextCell<Task, Task>) {
    cell.setOnDragDetected { event ->
      dragDetected(cell)
      event.consume()
    }
    cell.setOnDragOver { event -> dragOver(event, cell) }
    cell.setOnDragDropped { drop(cell) }
    cell.setOnDragExited {
    }
  }
  private fun dragDetected(cell: TextCell<Task, Task>) {
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

  private fun dragOver(event: DragEvent, cell: TextCell<Task, Task>) {
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

  private fun drop(cell: TextCell<Task, Task>) {
    val dropTarget = cell.treeTableRow.treeItem.value
    clipboardContent.cut()
    clipboardProcessor.pasteAsChild(dropTarget, clipboardContent)
  }

}

private fun ColumnList.Column.taskDefaultColumn() = TaskDefaultColumn.find(this.id)?.stub

private val taskNameConverter = MyStringConverter<Task, Task>(
  toString = { _, task -> task?.name },
  fromString = { cell, text ->
    cell.item?.let { task ->
      task.unpluggedClone().also { it.name = text }
    } ?: run {
      println("no item in this cell! cell=$cell")
      null
    }
  }
)

private val dragAndDropSupport = DragAndDropSupport()
private val ourNameCellFactory = TextCellFactory(converter = taskNameConverter) { cell ->
  dragAndDropSupport.install(cell)
  cell.graphicSupplier = { task: Task ->

    if (TaskDefaultColumn.COLOR.stub.isVisible || TaskDefaultColumn.INFO.stub.isVisible) {
      HBox().also { hbox ->
        hbox.alignment = Pos.CENTER
        Region().also {
          hbox.children.add(it)
          HBox.setHgrow(it, Priority.ALWAYS)
        }
        if (TaskDefaultColumn.INFO.stub.isVisible) {
          task.getProgressStatus().getIcon()?.let { icon ->
            StackPane(icon).also {
              it.styleClass.add("badge")
              hbox.children.add(it)
            }

          }
        }
        if (TaskDefaultColumn.COLOR.stub.isVisible) {
          StackPane(Circle().also {
            it.fill = rgb(task.color.red, task.color.green, task.color.blue)
            it.radius = 4.0
          }).also {
            it.styleClass.add("badge")
            hbox.children.add(it)
          }
        }
      }
    } else null
  }
  cell.contentDisplay = ContentDisplay.RIGHT
  cell.alignment = Pos.CENTER_LEFT
}

private fun Task.ProgressStatus.getIcon() : GlyphIcon<*>? =
  when (this) {
    Task.ProgressStatus.NOT_YET -> null
    Task.ProgressStatus.INPROGRESS -> FontAwesomeIconView(FontAwesomeIcon.HOURGLASS_HALF)
    Task.ProgressStatus.DEADLINE_MISS -> FontAwesomeIconView(FontAwesomeIcon.HOURGLASS_END)
  }

private val TEXT_FORMAT = DataFormat("text/ganttproject-task-node")
