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

import biz.ganttproject.FXUtil
import biz.ganttproject.app.*
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.BaseTreeTableComponent
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.core.table.ColumnList.ColumnStub
import biz.ganttproject.core.table.SelectionKeeper
import biz.ganttproject.core.table.depthFirstWalk
import biz.ganttproject.core.table.reload
import biz.ganttproject.lib.fx.*
import biz.ganttproject.task.TaskActions
import biz.ganttproject.task.ancestors
import com.sun.javafx.scene.control.behavior.CellBehaviorBase
import de.jensd.fx.glyphs.GlyphIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.application.Platform
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.input.*
import javafx.scene.layout.*
import javafx.scene.paint.Color.rgb
import javafx.scene.shape.Circle
import javafx.scene.text.TextAlignment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.chart.export.TreeTableApi
import net.sourceforge.ganttproject.chart.gantt.ClipboardContents
import net.sourceforge.ganttproject.chart.gantt.ClipboardTaskProcessor
import net.sourceforge.ganttproject.task.*
import net.sourceforge.ganttproject.task.algorithm.RetainRootsAlgorithm
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.util.List.copyOf
import java.util.function.Consumer
import javax.swing.SwingUtilities
import kotlin.math.ceil


/**
 * Customization of the generic tree table for the task display purposes.
 *
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
  val filterManager: TaskFilterManager,
  initializationPromise: TwoPhaseBarrierImpl<*>,
  private val newTaskActor: NewTaskActor<Task>
):
  BaseTreeTableComponent<Task, TaskDefaultColumn>(
    GPTreeTableView<Task>(TreeItem(taskManager.taskHierarchy.rootTask)),
    project,
    undoManager,
    taskManager.customPropertyManager
  ) {

  private val isSortedProperty = SimpleBooleanProperty()
  val rootItem: TreeItem<Task> = treeTable.root
  public override val tableModel = TaskTableModel(taskManager.customPropertyManager)
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
        TaskDefaultColumn.find(it)?.isIconified ?: false
      },
      allColumns = {
        ColumnList.Immutable.fromList(TaskDefaultColumn.getColumnStubs()).copyOf()
      }
    )
  )

  val columnListWidthProperty = SimpleObjectProperty<Pair<Double, Double>>()

  private val placeholderShowHidden by lazy {
    Button(RootLocalizer.formatText("taskTable.placeholder.showHiddenTasks")).also {
      it.styleClass.add("btn-attention")
      it.onAction = EventHandler {
        filterManager.activeFilter = VOID_FILTER
      }
    }
  }
  private val placeholderEmpty by lazy { Pane() }

  private val initializationCompleted = initializationPromise.register("Task table initialization")
  override val selectionKeeper = SelectionKeeper(logger = LOGGER, treeTable = this.treeTable, node2treeItem = { task ->
    taskManager.getTask(task.taskID)?.let {
      task2treeItem[it]
    }
  })
  private val treeTableSelectionListener = TreeSelectionListenerImpl(treeTable.selectionModel.selectedItems, selectionKeeper, selectionManager, this@TaskTable)
  private val dragAndDropSupport = DragAndDropSupport(selectionManager)
  private val ourNameCellFactory = TextCellFactory(converter = taskNameConverter) { cell ->
    dragAndDropSupport.install(cell)

    cell.alignment = Pos.CENTER_LEFT
    cell.onEditingCompleted = {
      runBlocking {
        LOGGER.debug("cell::onEditingCompleted(name): cell={}", cell)
        newTaskActor.inboxChannel.send(EditingCompleted(cell.item))
      }
    }
    cell.graphicSupplier = { task: Task? ->
      if (task == null) {
        null
      } else {
        fun setupIcon(icon: GlyphIcon<*>, scale: Double = 0.75, code: (StackPane, Button) -> Unit) {
          icon.glyphSize = scale * (minCellHeight.value - cellPadding)
          icon.textAlignment = TextAlignment.CENTER
          val btn = Button("", icon).also {
            it.contentDisplay = ContentDisplay.GRAPHIC_ONLY
          }
          StackPane(btn).also {
            it.styleClass.add("badge")
            //it.alignment = Pos.CENTER
            it.prefWidth = minCellHeight.value - cellPadding + 5.0
            it.prefHeight = it.prefWidth
            it.minWidth = it.prefWidth
            code(it, btn)
          }
        }
        if (TaskDefaultColumn.ATTACHMENTS.stub.isVisible || TaskDefaultColumn.COLOR.stub.isVisible || TaskDefaultColumn.INFO.stub.isVisible || TaskDefaultColumn.NOTES.stub.isVisible) {
          HBox().also { hbox ->
            hbox.alignment = Pos.CENTER
            hbox.spacing = 3.0
            Region().also {
              hbox.children.add(it)
              HBox.setHgrow(it, Priority.ALWAYS)
            }
            if (TaskDefaultColumn.ATTACHMENTS.stub.isVisible) {
              when (task.attachments.size) {
                0 -> {}
                1 -> {
                  setupIcon(MaterialIconView(MaterialIcon.ATTACH_FILE), scale = 1.5) { stackPane, button ->
                    hbox.children.add(stackPane)
                    button.tooltip = Tooltip(task.attachments[0].uri.toString())
                    button.onAction = EventHandler {
                      openInBrowser(task.attachments[0].uri.toString())
                    }
                    button.styleClass.add("btn-regular")
                  }
                }
                else -> {
                  TODO("More than one attachment is not yet supported")
                }
              }
            }
            if (TaskDefaultColumn.NOTES.stub.isVisible && !task.notes.isNullOrBlank()) {
              setupIcon(FontAwesomeIconView(FontAwesomeIcon.FILE_TEXT_ALT), scale=1.0) {stackPane, btn ->
                hbox.children.add(stackPane)
                btn.tooltip = Tooltip(task.notes)
              }
            }
            if (TaskDefaultColumn.INFO.stub.isVisible) {
              task.getProgressStatus().getIcon()?.let { icon ->
                setupIcon(icon) { stackPane, _ ->
                  hbox.children.add(stackPane)
                  if ("true" == System.getProperty("table.badges.colored", "true")) {
                    stackPane.styleClass.add("colored")
                    stackPane.styleClass.add(
                      when (task.getProgressStatus()) {
                        Task.ProgressStatus.DEADLINE_MISS -> "badge-error"
                        Task.ProgressStatus.INPROGRESS -> "badge-warning"
                        else -> ""
                      }
                    )
                  }
                }
              }
            }
            if (TaskDefaultColumn.COLOR.stub.isVisible) {
              StackPane().also {
                it.styleClass.addAll("badge")
                it.children.add(Circle().also {circle ->
                  circle.fill = rgb(task.color.red, task.color.green, task.color.blue)
                  circle.radius = (minCellHeight.value - cellPadding) / 2.0 - 1.0
                })
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

  init {
    columnBuilder = TaskColumnBuilder(
      tableModel, taskManager.customPropertyManager, undoManager, ourNameCellFactory,
      onNameEditCompleted = { task ->
        runBlocking {
          newTaskActor.inboxChannel.send(EditingCompleted(task))
        }
      }
    )
    columnList.totalWidthProperty.addListener { _, oldValue, newValue ->
      if (oldValue != newValue) {
        // We add vertical scroll bar width to the sum width of all columns, so that the split pane
        // which contains the table was resized appropriately.
        columnListWidthProperty.value = newValue.toDouble() to treeTable.vbarWidth()
      }
    }
    treeTable.onColumnResize = {
      columnList.onColumnResize
      projectModified()
    }
    initTaskEventHandlers()
    initProjectEventHandlers()
    initChartConnector()
    initKeyboardEventHandlers(taskActions.all())
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
    initNewTaskActor()

    filterManager.sync = { this.sync() }
    minCellHeight.addListener { observable, oldValue, newValue ->
      if (oldValue != newValue) {
        treeTable.coalescingRefresh()
      }
    }
  }

  override fun loadDefaultColumns() = FXUtil.runLater {
    treeTable.columns.clear()
    columnList.importData(ColumnList.Immutable.fromList(
      TaskDefaultColumn.getColumnStubs().map {
        ColumnStub(it)
      }.toList()
    ), false)
    val tableColumns = columnBuilder.buildColumns(
      columns = columnList.columns(),
      currentColumns = treeTable.columns.map { it.userData as ColumnList.Column }.toList(),
    )
    treeTable.setColumns(tableColumns)
    reload()
    columns.addListener(ListChangeListener {
      onColumnsChange()
    })
  }

  private fun onColumnsChange()  {
    FXUtil.runLater {
      columnList.columns().forEach { it.taskDefaultColumn()?.isVisible = it.isVisible }
      val newColumns = columnBuilder.buildColumns(
        columns = columnList.columns(),
        currentColumns = treeTable.columns.map { it.userData as ColumnList.Column }.toList(),
      )
      if (newColumns.isNotEmpty()) {
        keepSelection {
          reload()
          treeTable.setColumns(newColumns)
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
      FXUtil.runLater {
        treeTable.requestFocus()
      }
    }
  }

  private fun initTaskEventHandlers() {
    taskManager.addTaskListener(object : TaskListenerAdapter() {
      override fun taskAdded(e: TaskHierarchyEvent) {
        LOGGER.debug("taskAdded: event={}", e)
        if (e.taskSource == TaskManager.EventSource.USER) {
          runBlocking { newTaskActor.inboxChannel.send(TaskReady(e.task)) }
          FXUtil.runLater {
            sync()
            treeTable.selectionModel.clearSelection()
            CellBehaviorBase.removeAnchor(treeTable)
            val treeItem = task2treeItem[e.task]!!
            taskTableChartConnector.visibleTasks.clear()
            taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
            treeTable.selectionModel.select(treeItem)
            runBlocking { newTaskActor.inboxChannel.send(TreeItemReady(treeItem)) }
          }
        } else {
          keepSelection {
            addChildTreeItem(e.newContainer!!, e.task, e.indexAtNew, task2treeItem, ::onCreateTreeItem)
            taskTableChartConnector.visibleTasks.clear()
            taskTableChartConnector.visibleTasks.addAll(getExpandedTasks())
          }
        }
      }

      override fun taskMoved(e: TaskHierarchyEvent)  {
        if (e.oldContainer == null) {
          return
        }
        FXUtil.runLater {
          sync(true)
          // Force selection changed event because some actions depend on the relative location of tasks.
          selectionManager.fireSelectionChanged()
        }
      }

      override fun taskRemoved(e: TaskHierarchyEvent) {
        FXUtil.runLater { sync() }
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
            if (treeTable.editingCell == null) {
              val idx = treeTable.getRow(cmd.treeItem)
              treeTable.edit(idx, findNameColumn())
            } else {
              //println("there is an editing cell")
              //println("editing cell is ${treeTable.editingCell}")
            }
          }
          is CommitEditing -> {
            commitEditing()
          }
          is StartScrolling -> {
            treeTable.scrollTo(cmd.treeItem)
            FXUtil.runLater {
              runBlocking {  newTaskActor.inboxChannel.send(TreeItemScrolled(cmd.treeItem)) }
            }
          }
        }
      }
    }
  }

  private fun commitEditing() {
    ourNameCellFactory.editingCell?.commitEdit()
    //newTaskActor.inboxChannel.send(EditingCompleted())
  }

  private fun initSelectionListeners() {
    this.treeTable.focusedProperty().addListener { _, oldValue, newValue ->
      if (newValue && newValue != oldValue) {
        this.selectionManager.setUserInputConsumer(this@TaskTable)
        //this.treeTableSelectionListener.onChanged(null)
      }
    }
    this.treeTable.focusModel.focusedCellProperty().addListener { _, oldValue, newValue ->
      LOGGER.debug("Focus changed: newValue={}", newValue.row to newValue.column)
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

  fun reload(termination: OnBarrierReached? = null) {
    treeTable.reload(::sync, termination)
  }

  override fun sync(keepFocus: Boolean) {
    keepSelection(keepFocus) {
      try {
        doSync(keepFocus)
      } catch (ex: Exception) {
        LOGGER.error("Failure when syncing the task table", exception = ex)
      }
    }
  }
  private fun doSync(keepFocus: Boolean) {
      LOGGER.debug("Sync >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
      val treeModel = taskManager.taskHierarchy
      task2treeItem.clear()

      val syncAlgorithm = SyncAlgorithm(treeModel, task2treeItem, rootItem, filterManager.activeFilter.filterFxn, ::onCreateTreeItem)
      syncAlgorithm.sync()
      val visibleTasks = getExpandedTasks()
      taskTableChartConnector.visibleTasks.setAll(visibleTasks)
      if (visibleTasks.isEmpty()) {
        treeTable.placeholder = if (syncAlgorithm.filteredCount > 0) {
          placeholderShowHidden
        } else {
          placeholderEmpty
        }
      }
      filterManager.hiddenTaskCount.set(syncAlgorithm.filteredCount)
      initializationCompleted()
      LOGGER.debug("Sync <<<<<<<<<<<<<<<<<")
  }


  private fun onCreateTreeItem(treeItem: TreeItem<Task>) {
    val task = treeItem.value
    treeItem.isExpanded = treeCollapseView.isExpanded(task)
    treeItem.expandedProperty().subscribe(::onExpanded)
    treeTable.registerTreeItem(treeItem)
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

  override fun onProperties() {
    SwingUtilities.invokeLater {
      taskActions.propertiesAction.actionPerformed(null)
    }
  }
  override fun contextMenuActions(builder: MenuBuilder) {
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
}

internal class SyncAlgorithm(
  private val treeModel: TaskContainmentHierarchyFacade,
  private val task2treeItem: MutableMap<Task, TreeItem<Task>>,
  private val rootItem: TreeItem<Task>,
  private val activeFilter: TaskFilterFxn,
  private val onCreateTreeItem: (TreeItem<Task>) -> Unit
) {

  internal var filteredCount = 0

  fun sync() {
    task2treeItem[treeModel.rootTask] = rootItem

    treeModel.depthFirstWalk(treeModel.rootTask) { parent, child, idx, level ->
      LOGGER.debug(">>> [walk] parent={} child={} idx={}", parent, child, idx)
      val parentItem = task2treeItem[parent]!!
      val result = if (!this.activeFilter(parent, child)) {
        LOGGER.debug("...child={} is filtered out", child)
        if (idx < parentItem.children.size) {
          parentItem.children.remove(idx, parentItem.children.size)
        } else {
          LOGGER.debug("It seemed to be removed before, because its sibling was filtered, so we just skip it here")
        }
        LOGGER.debug("...now parentItem.children={}", parentItem.children)
        filteredCount++
        false
      } else {
        if (child == null) {
          parentItem.children.remove(idx, parentItem.children.size)
        } else {
          LOGGER.debug("...parentItem.children={}", parentItem.children)
          if (parentItem.children.size > idx) {
            val childItem = parentItem.children[idx]
            LOGGER.debug("...child@{}={}", idx, child)
            if (childItem.value.taskID == child.taskID) {
              childItem.value = child
              task2treeItem[child] = childItem
            } else {
              LOGGER.debug("...replacing child")
              parentItem.children.removeAt(idx)
              parent.addChildTreeItem(child, idx)
            }
          } else {
            LOGGER.debug("...adding child")
            parent.addChildTreeItem(child)
          }
        }
        true
      }
      LOGGER.debug("<<< [walk] parent={} child={} idx={}", parent, child, idx)
      result
    }
  }

  internal fun Task.addChildTreeItem(child: Task, pos: Int = -1) =
    addChildTreeItem(this, child, pos, task2treeItem, onCreateTreeItem)

}

internal fun addChildTreeItem(parent: Task, child: Task, pos: Int = -1,
                              task2treeItem: MutableMap<Task, TreeItem<Task>>,
                              onCreateTreeItem: (TreeItem<Task>) -> Unit): TreeItem<Task> {
  val parentItem = task2treeItem[parent] ?: run {
    //println(task2treeItem)
    throw NullPointerException("NPE! this=$parent")
  }


  val childItem = task2treeItem[child] ?: TreeItem(child).also { onCreateTreeItem(it) }
  childItem.parent?.children?.remove(childItem)
  if (pos == -1 || pos > parentItem.children.size) {
    parentItem.children.add(childItem)
  } else {
    parentItem.children.add(pos, childItem)
  }
//    LOGGER.debug("addChildTreeItem: child=$child pos=$pos parent=$parentItem")
//    LOGGER.debug("addChildTreeItem: parentItem.children=${parentItem.children}")
//    LOGGER.delegate().debug("Stack: ", Exception())
  task2treeItem[child] = childItem
  return childItem
}

private class TreeSelectionListenerImpl(
  private val selectedItems: ObservableList<TreeItem<Task>?>,
  private val selectionKeeper: SelectionKeeper<Task>,
  private val selectionManager: TaskSelectionManager,
  private val selectionSource: Any
)
  : ListChangeListener<TreeItem<Task>> {

  override fun onChanged(c: ListChangeListener.Change<out TreeItem<Task>>?) {
    LOGGER.debug("Selection changed: currentSelection={}", selectedItems)
    if (!selectionKeeper.ignoreSelectionChange) {
      copyOf(selectedItems.filterNotNull()).map { it.value }
        .filter { it.manager.taskHierarchy.contains(it) }.also {
          SwingUtilities.invokeLater {
            selectionManager.setSelectedTasks(it, selectionSource)
          }
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


class DragAndDropSupport(private val selectionManager: TaskSelectionManager) {
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
    val task = cell.tableRow?.treeItem?.value ?: return
    clipboardContent = ClipboardContents(task.manager).also { clipboard ->
      clipboard.addTasks(selectionManager.selectedTasks.let {selection ->
        mutableListOf<Task>().also {
          ourRetainRootsAlgorithm.run(selection, getParentTask, it)
        }
      })
    }
    clipboardProcessor = ClipboardTaskProcessor(task.manager)
    val db = cell.startDragAndDrop(TransferMode.COPY)
    val content = ClipboardContent()
    content[TEXT_FORMAT] = task.taskID
    db.setContent(content)
    db.dragView = cell.snapshot(null, null)
    cell.setOnDragExited { db.clear() }
  }

  private fun dragOver(event: DragEvent, cell: TextCell<Task, Task>) {
    if (!event.dragboard.hasContent(TEXT_FORMAT)) return
    val thisItem = cell.tableRow.treeItem

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
    val dropTarget = cell.tableRow.treeItem.value
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

private fun Task.ProgressStatus.getIcon() : GlyphIcon<*>? =
  when (this) {
    Task.ProgressStatus.NOT_YET -> null
    Task.ProgressStatus.INPROGRESS -> FontAwesomeIconView(FontAwesomeIcon.HOURGLASS_HALF)
    Task.ProgressStatus.DEADLINE_MISS -> FontAwesomeIconView(FontAwesomeIcon.HOURGLASS_END)
  }


private val getParentTask =  { task: Task -> task.manager.taskHierarchy.getContainer(task) }

private val ourRetainRootsAlgorithm = RetainRootsAlgorithm<Task>()

private val TEXT_FORMAT = DataFormat.lookupMimeType("text/ganttproject-task-node") ?: DataFormat("text/ganttproject-task-node")
private val LOGGER = GPLogger.create("TaskTable")