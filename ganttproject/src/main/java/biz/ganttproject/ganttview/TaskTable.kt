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

import biz.ganttproject.app.*
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.core.table.ColumnList.ColumnStub
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.lib.fx.*
import biz.ganttproject.task.TaskActions
import biz.ganttproject.task.ancestors
import com.sun.javafx.scene.control.behavior.CellBehaviorBase
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
import javafx.scene.control.*
import javafx.scene.control.cell.CheckBoxTreeTableCell
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.*
import javafx.scene.paint.Color.rgb
import javafx.scene.shape.Circle
import javafx.util.Callback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.CustomPropertyClass
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ProjectEventListener
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
import java.util.function.Consumer
import javax.swing.SwingUtilities
import kotlin.math.ceil


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
  private val undoManager: GPUndoManager,
  private val filters: TaskFilterManager,
  initializationPromise: TwoPhaseBarrierImpl<*>,
  private val newTaskActor: NewTaskActor<Task>
) {
  val headerHeightProperty: ReadOnlyDoubleProperty get() = treeTable.headerHeight
  private val isSortedProperty = SimpleBooleanProperty()
  private val treeModel = taskManager.taskHierarchy
  val rootItem = TreeItem(treeModel.rootTask)
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
      taskPropertiesAction = { taskActions.propertiesAction },
      contextMenuActions = this::contextMenuActions,
      isSorted = isSortedProperty
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
  private val filterCompletedTasksAction = FilterCompletedTasks(filters, taskManager)

  private val placeholderShowHidden by lazy {
    Button(RootLocalizer.formatText("taskTable.placeholder.showHiddenTasks")).also {
      it.styleClass.add("btn-attention")
      it.onAction = EventHandler {
        filterCompletedTasksAction.setChecked(false)
      }
    }
  }
  private val placeholderEmpty by lazy { Pane() }

  private val initializationCompleted = initializationPromise.register("Task table initialization")
  private val treeTableSelectionListener = TreeSelectionListenerImpl(treeTable.selectionModel.selectedItems, selectionManager, this@TaskTable)
  private var projectModified: () -> Unit = { project.isModified = true }
  init {
    TaskDefaultColumn.setLocaleApi { key -> GanttLanguage.getInstance().getText(key) }

    columnList.totalWidthProperty.addListener { _, oldValue, newValue ->
      if (oldValue != newValue) {
        // We add vertical scroll bar width to the sum width of all columns, so that the split pane
        // which contains the table was resized appropriately.
        columnListWidthProperty.value = newValue.toDouble() + treeTable.vbarWidth()
      }
    }
    treeTable.onColumnResize = {
      columnList.onColumnResize
      projectModified()
    }
    Platform.runLater {
      treeTable.isShowRoot = false
      treeTable.isEditable = true
      treeTable.isTableMenuButtonVisible = false
    }
    treeTable.stylesheets.add("/biz/ganttproject/app/Dialog.css")
    initTaskEventHandlers()
    initProjectEventHandlers()
    initChartConnector()
    initKeyboardEventHandlers()
    initSelectionListeners()
    treeTable.onSort = EventHandler {
      // It is important to run this later because the event is sent _before_ the
      // actual sorting, so if we collect the expanded tasks synchronously, we
      // get the order before sorting.
      Platform.runLater {
        if (treeTable.sortOrder.isEmpty()) {
          isSortedProperty.value = false
          reload()
        } else {
          isSortedProperty.value = true
          taskTableChartConnector.visibleTasks.clear()
          getExpandedTasks().also {
            taskTableChartConnector.visibleTasks.addAll(it)
          }
        }
      }
    }
    columns.addListener(ListChangeListener {
      onColumnsChange()
    })
    initNewTaskActor()
    treeTable.onProperties = this::onProperties
    treeTable.contextMenuActions = this::contextMenuActions
    treeTable.tableMenuActions = this::tableMenuActions

    filters.sync = this::sync
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
      taskActions.all().firstOrNull { action ->
        action.triggeredBy(event)
      }?.let { action ->
        undoManager.undoableEdit(action.name) {
          action.actionPerformed(null)
        }
      }
    }
  }

  private fun initChartConnector() {
    taskTableChartConnector.rowHeight.addListener { _, _, newValue ->
      Platform.runLater {
        treeTable.fixedCellSize = ceil(maxOf(newValue.toDouble(), minCellHeight.value))
      }
    }
    taskTableChartConnector.chartScrollOffset = Consumer { newValue ->
      Platform.runLater {
        treeTable.scrollBy(newValue)
      }
    }
    treeTable.addScrollListener { newValue ->
      taskTableChartConnector.tableScrollOffset.value = newValue
    }
    taskTableChartConnector.exportTreeTableApi = {
      TreeTableApi(
        rowHeight = { taskTableChartConnector.rowHeight.value },
        tableHeaderHeight = { treeTable.headerHeight.intValue()  },
        width = { fullWidthNotViewport ->
          if (fullWidthNotViewport) {
            // TODO: we need to autosize onl if column widths are not in the file,
            // that is, when we import from MS Project or CSV
            val job = CoroutineScope(Dispatchers.JavaFx).launch {
              treeTable.autosizeColumns()
              columnList.reloadWidthFromUi()
            }
            runBlocking {
              job.join()
            }
            columnList.totalWidth.toInt()
          } else {
            treeTable.width.toInt() - treeTable.vbarWidth().toInt()
          }
        },
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
      override fun projectRestoring(completion: Barrier<Document>) {
        completion.await {
          sync()
        }
      }

      override fun projectOpened(
        barrierRegistry: BarrierEntrance,
        barrier: Barrier<IGanttProject>
      ) {
        barrier.await {
          this@TaskTable.projectModified = {
            project.isModified = true
          }
        }
        reload(barrierRegistry.register("Reload Task Table"))
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
            CellBehaviorBase.removeAnchor(treeTable)
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
        if (e.oldContainer == null) {
          return
        }
        keepSelection(keepFocus = true) {
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
        keepSelection {
          reload()
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
              val idx = treeTable.getRow(cmd.treeItem)
              treeTable.scrollTo(cmd.treeItem)
              treeTable.edit(idx, findNameColumn())
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
        this.selectionManager.setUserInputConsumer(this@TaskTable)
        this.treeTableSelectionListener.onChanged(null)
      }
    }

    this.selectionManager.addSelectionListener(object : TaskSelectionManager.Listener {
      override fun selectionChanged(currentSelection: List<Task>, source: Any?) {
        if (source != this@TaskTable) {
          Platform.runLater {
            ancestors(currentSelection, taskManager.taskHierarchy).reversed()
              .forEach { task2treeItem[it]?.isExpanded = true }

            treeTable.selectionModel.clearSelection()
            for (task in currentSelection) {
              task2treeItem[task]?.let {
                treeTable.selectionModel.select(it)
              }
            }
            if (currentSelection.size == 1) {
              task2treeItem[currentSelection[0]]?.let { treeTable.scrollTo(it) }
            }
          }
        }
      }

      override fun userInputConsumerChanged(newConsumer: Any?) {
        // TODO: commit editing
      }
    })
//    treeTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
//    treeTable.selectionModel.selectedItems.addListener(ListChangeListener {
//      copyOf(treeTable.selectionModel.selectedItems.filterNotNull()).map { it.value }
//        .filter { it.manager.taskHierarchy.contains(it) }.also {
//          selectionManager.setSelectedTasks(it, this@TaskTable)
//        }
//    })

    treeTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
    treeTable.selectionModel.selectedItems.addListener(treeTableSelectionListener)
  }

  private fun findNameColumn() = treeTable.columns.find { (it.userData as ColumnList.Column).id == TaskDefaultColumn.NAME.stub.id }

  private fun buildColumns(columns: List<ColumnList.Column>) {
    val tableColumns =
      columns.mapNotNull { column ->
        when (val taskDefaultColumn = TaskDefaultColumn.find(column.id)) {
          TaskDefaultColumn.COLOR, TaskDefaultColumn.INFO -> null
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
              event.newValue?.let { copyTask ->
                undoManager.undoableEdit("Edit properties of task ${copyTask.name}") {
                  taskTableModel.setValue(copyTask.name, targetTask, taskDefaultColumn)
                }
              }
              runBlocking { newTaskActor.inboxChannel.send(EditingCompleted(targetTask)) }
            }
            onEditCancel = EventHandler { event ->
              val targetTask: Task = event.rowValue.value
              runBlocking { newTaskActor.inboxChannel.send(EditingCompleted(targetTask)) }
            }
            treeTable.treeColumn = this
          }
        } else {
          createTextColumn(taskDefaultColumn.getName(),
            { taskTableModel.getValueAt(it, taskDefaultColumn).toString() },
            { task, value -> taskTableModel.setValue(value, task, taskDefaultColumn) },
            { runBlocking { newTaskActor.inboxChannel.send(EditingCompleted()) } }
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
          { task, value -> taskTableModel.setValue(value, task, customProperty) },
          { runBlocking { newTaskActor.inboxChannel.send(EditingCompleted()) } }
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
          { taskTableModel.getValue(it, customProperty) as Int? },
          { task, value -> taskTableModel.setValue(value, task, customProperty) }
        )
      }
      CustomPropertyClass.DOUBLE -> {
        createDoubleColumn(customProperty.name,
          { taskTableModel.getValue(it, customProperty) as Double? },
          { task, value -> taskTableModel.setValue(value, task, customProperty) }
        )
      }
      CustomPropertyClass.DATE -> {
        createDateColumn(customProperty.name,
          { taskTableModel.getValue(it, customProperty) as GanttCalendar? },
          { task, value -> taskTableModel.setValue(value, task, customProperty) }
        )
      }
      else -> null
    }?.also {
      it.isEditable = true
      it.isVisible = column.isVisible
      it.userData = column
      it.prefWidth = column.width.toDouble()
      it.minWidth = column.width.toDouble()
    }
  }

  fun reload(termination: OnBarrierReached? = null) {
    Platform.runLater {
      treeTable.root.children.clear()
      treeTable.selectionModel.clearSelection()
      sync()
      termination?.invoke()
    }
  }

  fun sync() {
    keepSelection {
      val treeModel = taskManager.taskHierarchy
      task2treeItem.clear()
      task2treeItem[treeModel.rootTask] = rootItem

      var filteredCount = 0
      treeModel.depthFirstWalk(treeModel.rootTask) { parent, child, idx ->
        if (!this.filters.activeFilter(parent, child)) {
          val parentItem = task2treeItem[parent]!!
          parentItem.children.remove(idx, parentItem.children.size)
          filteredCount++
          false
        } else {
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
      }
      val visibleTasks = getExpandedTasks()
      taskTableChartConnector.visibleTasks.setAll(visibleTasks)
      if (visibleTasks.isEmpty()) {
        treeTable.placeholder = if (filteredCount > 0) {
          placeholderShowHidden
        } else {
          placeholderEmpty
        }
      }
      initializationCompleted()
    }
  }

  private fun Task.addChildTreeItem(child: Task, pos: Int = -1): TreeItem<Task> {
    val parentItem = task2treeItem[this] ?: run {
      //println(task2treeItem)
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
    treeTable.registerTreeItem(it)
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

  private fun keepSelection(keepFocus: Boolean = false, code: ()->Unit) {
    val body = {
      val selectedTasks =
        treeTable.selectionModel.selectedItems.associate {
          it.value to (it.previousSibling()
            ?: it.parent?.let { parent -> if (parent == treeTable.root) null else parent }
            ?: it.nextSibling())
        }
      val focusedTask = treeTable.focusModel.focusedItem?.value
      val focusedCell = treeTable.focusModel.focusedCell
      // This way we ignore table selection changes which happen when we manipulate with the tree items in code()
      treeTableSelectionListener.disabled = true
      code()
      // Yup, sometimes clearSelection() call is not enough, and selectedIndices remain not empty after it.
      treeTable.selectionModel.clearSelection()
      treeTable.selectionModel.selectedIndices.clear()
      CellBehaviorBase.removeAnchor(treeTable)
      treeTableSelectionListener.disabled = false

      // The array of row numbers is passed as vararg argument to selectIndices
      val selectedRows = selectedTasks
        .map { task2treeItem[taskManager.getTask(it.key.taskID)] ?: it.value }
        .map { treeTable.getRow(it) }
        .toIntArray()
      treeTable.selectionModel.selectIndices(-1, *selectedRows)

      // Sometimes we need to keep the focus, e.g. when we move some task in the tree, but sometimes we want to focus
      // some other item. E.g. if a task was added due to user action, the user would expect the new task to be focused.
      if (keepFocus && focusedTask != null) {
        val liveTask = taskManager.getTask(focusedTask.taskID)
        task2treeItem[liveTask]?.let { it ->
          val row = treeTable.getRow(it)
          Platform.runLater {
            treeTable.focusModel.focus(TreeTablePosition(treeTable, row, focusedCell.tableColumn))
          }
        }
      }
      treeTable.requestFocus()
    }
    if (Platform.isFxApplicationThread()){
      body()
    } else {
      Platform.runLater { body() }
    }
  }

  private fun onProperties() {
    SwingUtilities.invokeLater {
      taskActions.propertiesAction.actionPerformed(null)
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

  fun tableMenuActions(builder: MenuBuilder) {
    builder.apply {
      items(taskActions.manageColumnsAction, this@TaskTable.filterCompletedTasksAction)
    }
  }

  fun initUserKeyboardInput() {
    treeTable.requestFocus()
    this.requestSwingFocus()
  }

  private val ourNameCellFactory = TextCellFactory(converter = taskNameConverter) { cell ->
    dragAndDropSupport.install(cell)

    cell.alignment = Pos.CENTER_LEFT
    cell.onEditingCompleted = {
      runBlocking { newTaskActor.inboxChannel.send(EditingCompleted()) }
    }
    cell.graphicSupplier = { task: Task? ->
      if (task == null) {
        null
      } else {
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
    }
    cell.contentDisplay = ContentDisplay.RIGHT
    cell.alignment = Pos.CENTER_LEFT
  }
}

private class TreeSelectionListenerImpl(
  private val selectedItems: ObservableList<TreeItem<Task>?>,
  private val selectionManager: TaskSelectionManager,
  private val selectionSource: Any)
  : ListChangeListener<TreeItem<Task>> {

  var disabled: Boolean = false

  override fun onChanged(c: ListChangeListener.Change<out TreeItem<Task>>?) {
    if (!disabled) {
      copyOf(selectedItems.filterNotNull()).map { it.value }
        .filter { it.manager.taskHierarchy.contains(it) }.also {
          selectionManager.setSelectedTasks(it, selectionSource)
        }
    }
  }
}

data class TaskTableChartConnector(
  val rowHeight: IntegerProperty,
  val visibleTasks: ObservableList<Task>,
  val tableScrollOffset: DoubleProperty,
  var isTableScrollable: Boolean,
  var chartScrollOffset: Consumer<Double>?,
  var exportTreeTableApi: () -> TreeTableApi? = { null },
  var focus: () -> Unit = {},
  val minRowHeight: DoubleProperty
)

data class TaskTableActionConnector(
  val commitEdit: ()->Unit,
  val runKeepingExpansion: ((task: Task, code: (Task)->Unit) -> Unit),
  val scrollTo: (task: Task) -> Unit,
  val columnList: () -> ColumnList,
  val taskPropertiesAction: () -> GPAction,
  val contextMenuActions: (MenuBuilder) -> Unit,
  val isSorted: ReadOnlyBooleanProperty
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

private fun Task.ProgressStatus.getIcon() : GlyphIcon<*>? =
  when (this) {
    Task.ProgressStatus.NOT_YET -> null
    Task.ProgressStatus.INPROGRESS -> FontAwesomeIconView(FontAwesomeIcon.HOURGLASS_HALF)
    Task.ProgressStatus.DEADLINE_MISS -> FontAwesomeIconView(FontAwesomeIcon.HOURGLASS_END)
  }

private val TEXT_FORMAT = DataFormat("text/ganttproject-task-node")
