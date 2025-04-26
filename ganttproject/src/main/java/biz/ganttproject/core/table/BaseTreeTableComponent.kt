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
import biz.ganttproject.app.Barrier
import biz.ganttproject.app.BarrierEntrance
import biz.ganttproject.app.OnBarrierReached
import biz.ganttproject.customproperty.CustomPropertyDefinition
import biz.ganttproject.lib.fx.*
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.scene.control.TreeItem
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ProjectEventListener
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import javax.swing.event.UndoableEditEvent

/**
 * This is a base class for the tree tables in Gantt and Resource views. It defines type-parameterized methods
 * for working with the tree table items.
 */
abstract class BaseTreeTableComponent<NodeType, BuiltinColumnType: BuiltinColumn>(
  val treeTable: GPTreeTableView<NodeType>,
  private val project: IGanttProject,
  private val undoManager: GPUndoManager
  ) {

  val headerHeightProperty: ReadOnlyDoubleProperty get() = treeTable.headerHeight
  protected var projectModified: () -> Unit = { project.isModified = true }
  lateinit var columnBuilder: ColumnBuilder<NodeType, BuiltinColumnType>

  init {
    FXUtil.runLater {
      treeTable.isShowRoot = false
      treeTable.isEditable = true
      treeTable.isTableMenuButtonVisible = false
    }
    treeTable.stylesheets.add("/biz/ganttproject/app/Dialog.css")

  }

  protected fun initProjectEventHandlers() {
    project.addProjectEventListener(object : ProjectEventListener.Stub() {
      override fun projectRestoring(completion: Barrier<Document>) {
        completion.await {
          sync(keepFocus = true)
        }
      }

      override fun projectOpened(barrierRegistry: BarrierEntrance, barrier: Barrier<IGanttProject>) {
        treeTable.reload(::sync, barrierRegistry.register("Reload Task Table"))
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

  abstract fun loadDefaultColumns()
  protected abstract fun sync(keepFocus: Boolean = false)
}

fun <T> TreeItem<T>.depthFirstWalk(visitor: (TreeItem<T>) -> Boolean) {
  this.children.forEach { if (visitor(it)) it.depthFirstWalk(visitor) }
}

fun <T> TreeItem<T>.find(predicate: (TreeItem<T>) -> Boolean): TreeItem<T>? {
  if (predicate(this)) return this
  else {
    this.children.forEach {
      val result = it.find(predicate)
      if (result != null) {
        return result
      }
    }
    return null
  }
}


/**
 * Interface of the tree model which provides getters and setters of the values shown and changed in the tree.
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