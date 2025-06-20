/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package biz.ganttproject.core.table

import biz.ganttproject.FXUtil
import biz.ganttproject.app.*
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.CustomPropertyDefinition
import biz.ganttproject.customproperty.CustomPropertyHolder
import biz.ganttproject.customproperty.CustomPropertyManager
import biz.ganttproject.lib.fx.BuiltinColumns
import biz.ganttproject.lib.fx.ColumnListImpl
import biz.ganttproject.lib.fx.GPTreeTableView
import biz.ganttproject.lib.fx.depthFirstWalk
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.control.TreeItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ProjectEventListener
import net.sourceforge.ganttproject.ProjectOpenActivityFactory
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import javax.swing.SwingUtilities
import javax.swing.event.UndoableEditEvent

/**
 * This is a base class for the tree tables in Gantt and Resource views. It defines type-parameterized methods
 * for working with the tree table items.
 */
abstract class BaseTreeTableComponent<NodeType, BuiltinColumnType: BuiltinColumn>(
  val treeTable: GPTreeTableView<NodeType>,
  internal val project: IGanttProject,
  private val undoManager: GPUndoManager,
  val customPropertyManager: CustomPropertyManager,
  val builtinColumns: BuiltinColumns,
  private val projectOpenActivityFactory: ProjectOpenActivityFactory
) {

  val headerHeightProperty: ReadOnlyDoubleProperty get() = treeTable.headerHeight
  val rowHeightProperty: ReadOnlyDoubleProperty get() = treeTable.fixedCellSizeProperty()

  protected var projectModified: () -> Unit = { project.isModified = true }
  lateinit var columnBuilder: ColumnBuilder<NodeType, BuiltinColumnType>
  protected val columns: ObservableList<ColumnList.Column> = FXCollections.observableArrayList()
  val columnList: ColumnListImpl = ColumnListImpl(columns, customPropertyManager,
    { treeTable.columns },
    { onColumnsChange() },
    builtinColumns
  )
  val columnListWidthProperty = SimpleObjectProperty<Pair<Double, Double>>()
  val rootItem: TreeItem<NodeType> = treeTable.root
  protected var areChangesIgnored = false

  init {
    FXUtil.runLater {
      treeTable.isShowRoot = false
      treeTable.isEditable = true
      treeTable.isTableMenuButtonVisible = false
    }
    treeTable.stylesheets.add("/biz/ganttproject/app/Dialog.css")
    treeTable.onProperties = this::onProperties
    treeTable.contextMenuActions = this::contextMenuActions
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
  }

  protected fun initProjectEventHandlers() {
    projectOpenActivityFactory.addListener { sm ->
      areChangesIgnored = true
      val tablesReadyStateEntrance = sm.stateTablesReady.register("Reload ${this.javaClass.simpleName}")
      sm.stateMainModelReady.await {
        treeTable.reload(::sync, tablesReadyStateEntrance)
      }
      sm.stateTablesReady.await {
        areChangesIgnored = false
        treeTable.updateWidth()
      }
      sm.stateFailed.await {
        areChangesIgnored = false
      }
    }
    project.addProjectEventListener(object : ProjectEventListener.Stub() {
      override fun projectRestoring(completion: Barrier<Document>) {
        completion.await {
          sync(keepFocus = true)
        }
      }

      override fun projectOpened(barrierRegistry: BarrierEntrance, barrier: Barrier<IGanttProject>) {
        //treeTable.reload(::sync, barrierRegistry.register("Reload Task Table"))
      }

      override fun projectCreated() {
        loadDefaultColumns()
        treeTable.reload(::sync)
      }
    })

    undoManager.addUndoableEditListener(object : GPUndoListener {
      override fun undoableEditHappened(e: UndoableEditEvent) {
        treeTable.coalescingRefresh()
      }

      override fun undoOrRedoHappened() {}
      override fun undoReset() {}
    })
  }

  protected fun initKeyboardEventHandlers(keyActions: List<GPAction>) {
    treeTable.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
      event.whenMatches("tree.expand") {
        val focusedCell = treeTable.focusModel.focusedCell ?: return@whenMatches
        keepSelection(keepFocus = true) {
          focusedCell.treeItem.isExpanded = focusedCell.treeItem.isExpanded.not()
        }
      }
      event.whenMatches("tree.expandAll") {
        val focusedCell = treeTable.focusModel.focusedCell ?: return@whenMatches
        keepSelection(keepFocus = true) {
          focusedCell.treeItem.isExpanded = true
          focusedCell.treeItem.depthFirstWalk {
            it.isExpanded = true
            return@depthFirstWalk true
          }
        }
      }
      event.whenMatches("tree.collapseAll") {
        val focusedCell = treeTable.focusModel.focusedCell ?: return@whenMatches
        keepSelection(keepFocus = true) {
          focusedCell.treeItem.depthFirstWalk {
            it.isExpanded = false
            return@depthFirstWalk true
          }
          focusedCell.treeItem.isExpanded = false
        }
      }
    }
    treeTable.onKeyPressed = EventHandler { event ->
      keyActions.firstOrNull { action ->
        action.triggeredBy(event)
      }?.let { action ->
        SwingUtilities.invokeLater {
          undoManager.undoableEdit(action.name) {
            action.actionPerformed(null)
          }
        }
      }

      val focusedCell = treeTable.focusModel.focusedCell
      val column = focusedCell.tableColumn
      column?.userData?.let {
        if (column.isEditable && it is ColumnList.Column) {
          this.customPropertyManager.getCustomPropertyDefinition(it.id)?.let { def ->
            if (def.propertyClass == CustomPropertyClass.BOOLEAN) {
              if (event.code == KeyCode.SPACE || event.code == KeyCode.ENTER && event.getModifiers() == 0) {
                val task = focusedCell.treeItem.value
                // intentionally java.lang.Boolean, because as? Boolean returns null
                (tableModel.getValue(task, def) as? java.lang.Boolean)?.let { value ->
                  undoManager.undoableEdit("Edit properties") {
                    tableModel.setValue(value.booleanValue().not(), task, def)
                  }
                  treeTable.refreshFocusedCell()
                }
              }
            }
          }
        }
      }
    }
  }

  abstract fun isTreeColumn(column: BuiltinColumnType): Boolean
  abstract fun getCustomValues(node: NodeType): CustomPropertyHolder
  abstract fun loadDefaultColumns()
  protected abstract fun sync(keepFocus: Boolean = false)
  protected abstract fun onProperties()
  fun onColumnsChange()  {
    FXUtil.runLater {
      columnList.columns().forEach { builtinColumns.find(it.id)?.stub?.isVisible = it.isVisible }
      val newColumns = columnBuilder.buildColumns(
        columns = columnList.columns(),
        currentColumns = treeTable.columns.map { it.userData as ColumnList.Column }.toList(),
      )
      if (newColumns.isNotEmpty()) {
        keepSelection {
          treeTable.reload(::sync, null)
          treeTable.setColumns(newColumns)
        }
      }
    }
  }

  protected abstract fun contextMenuActions(builder: MenuBuilder)
  abstract val tableModel: TableModel<NodeType, BuiltinColumnType>
  protected abstract val selectionKeeper: SelectionKeeper<NodeType>
  protected fun keepSelection(keepFocus: Boolean = false, code: () -> Unit) {
    selectionKeeper.keepSelection(keepFocus, code)
  }
}


/**
 * Interface of the table model which provides getters and setters of the values shown and changed in the tree table.
 */
interface TableModel<NodeType, DefaultColumnType: BuiltinColumn> {
  fun getValueAt(t: NodeType, defaultColumn: DefaultColumnType): Any?
  fun getValue(t: NodeType, customProperty: CustomPropertyDefinition): Any?
  fun setValue(value: Any, node: NodeType, property: DefaultColumnType)
  fun setValue(value: Any, node: NodeType, column: CustomPropertyDefinition)
}

fun <T> GPTreeTableView<T>.reload(sync: ()->Unit, termination: OnBarrierReached? = null) {
  FXUtil.runLater {
    this.root.children.clear()
    this.selectionModel.clearSelection()
    sync()
    termination?.invoke()
  }
}

internal val LOGGER = GPLogger.create("BaseTreeTable")