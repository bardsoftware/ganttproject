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
package biz.ganttproject.lib.fx

import biz.ganttproject.app.MenuBuilder
import biz.ganttproject.app.RootLocalizer
import javafx.application.Platform
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.collections.MapChangeListener
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.skin.TreeTableViewSkin
import javafx.scene.control.skin.VirtualFlow
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.util.StringConverter

/**
 * @author dbarashev@bardsoftware.com
 */
class GPTreeTableView<T>(rootItem: TreeItem<T>) : TreeTableView<T>(rootItem) {
  internal val tableMenu = ContextMenu()
  var contextMenuActions: (MenuBuilder) -> Unit = { }
  var tableMenuActions: (MenuBuilder) -> Unit = {}

  init {
    columnResizePolicy = CONSTRAINED_RESIZE_POLICY
    stylesheets.add("/biz/ganttproject/lib/fx/TreeTable.css")
    styleClass.add("gp-tree-table-view")
    focusModel.focusedCellProperty().addListener { _, _, newValue ->
      if (newValue.column == -1) {
        focusModel.focus(newValue.row, columns[0])
      }
      refresh()
    }
    addEventHandler(MouseEvent.MOUSE_CLICKED) { event ->
      if (event.button == MouseButton.SECONDARY) {
        val contextMenu = ContextMenu()
        contextMenuActions(MenuBuilder(contextMenu))
        contextMenu.show(this, event.screenX, event.screenY)
      }
    }

  }
  override fun createDefaultSkin(): Skin<*> {
    return GPTreeTableViewSkin(this).also {
      it.scrollValue.addListener { _, _, newValue -> this.scrollListener(newValue.toDouble()) }
      it.headerHeight.addListener { _, _, _ ->
        headerHeight.value = it.fullHeaderHeight
      }
    }
  }

  val headerHeight = SimpleDoubleProperty(0.0)

  var scrollListener: (Double)->Unit = {}
  fun addScrollListener(listener: (Double)->Unit) {
    this.scrollListener = listener
  }

  fun scrollBy(value: Double) {
    skin?.let { (it as GPTreeTableViewSkin<T>).scrollBy(value) }
  }

  override fun requestFocus() {
    super.requestFocus()
    val focusedCell = this.focusModel.focusedCell
    if (focusedCell.tableColumn == null) {
      this.focusModel.focus(focusedCell.row, columns[0])
    }
  }
}

class GPTreeTableViewSkin<T>(control: GPTreeTableView<T>) : TreeTableViewSkin<T>(control) {

  val scrollValue = SimpleDoubleProperty()
  val headerHeight: ReadOnlyDoubleProperty
  get() = tableHeaderRow.heightProperty()
  val fullHeaderHeight: Double get() = headerHeight.value + tableHeaderRow.boundsInParent.minX
  private val contentWidthListener = mutableMapOf<Double, ()->Unit>()

  init {
    this.virtualFlow.positionProperty().addListener { _, _, _ ->
      var totalCellHeight = 0.0
      for (idx in 0 until virtualFlow.cellCount) {
        totalCellHeight += virtualFlow.getCell(idx).height
      }
      val result = (totalCellHeight - virtualFlow.height) * virtualFlow.position
      scrollValue.value = result
    }
    val cornerRegion = this.tableHeaderRow.lookup(".show-hide-columns-button") as Region
    cornerRegion.onMousePressed = EventHandler { me: MouseEvent ->
      control.tableMenu.items.clear()
      control.tableMenuActions(MenuBuilder(control.tableMenu))
      control.tableMenu.show(cornerRegion, Side.BOTTOM, 0.0, 0.0)
      me.consume()
    }
    skinnable.properties.addListener(MapChangeListener { change ->
      if (change.key == "TableView.contentWidth" && change.wasAdded()) {
        var value = change.valueAdded as Double
        println("contentWidth=$value vbar width=${(virtualFlow as MyVirtualFlow).vbarWidth()}")
        value += (virtualFlow as MyVirtualFlow).vbarWidth()
        contentWidthListener.remove(value)?.invoke()
      }
    })
  }

  override fun createVirtualFlow(): VirtualFlow<TreeTableRow<T>> {
    return MyVirtualFlow();
  }
  fun onContentWidthChange(expected: Double, code: () -> Unit) {
    contentWidthListener[expected] = code
  }

  fun scrollBy(value: Double) {
    this.virtualFlow.scrollPixels(value)
  }
}

interface TreeCollapseView<T> {
  fun isExpanded(node: T): Boolean
  fun setExpanded(node: T, value: Boolean)
}

class SimpleTreeCollapseView<T> : TreeCollapseView<T> {
  private val node2value = mutableMapOf<T, Boolean>()
  override fun isExpanded(node: T): Boolean {
    return node2value[node] ?: true
  }

  override fun setExpanded(node: T, value: Boolean) {
    node2value[node] = value
  }
}

class TextCell<T, S>(
  private val converter: StringConverter<S>,
  private val editingCellController: (TextCell<T, S>?) -> Boolean
) : TreeTableCell<T, S>() {
  private val textField: TextField = createTextField(this, converter)

  init {
    styleClass.add("gp-tree-table-cell")
  }
  override fun startEdit() {
    if (!isEditable) {
      return
    }
    super.startEdit()

    if (isEditing && editingCellController(this)) {
      treeTableView.requestFocus()
      startEdit(this, converter, null, null, textField)
    }
  }

  override fun cancelEdit() {
    super.cancelEdit()
    editingCellController(null)
    cancelEdit(this, converter, null)
    treeTableView.requestFocus()
  }

  fun commitEdit() {
    commitEdit(converter.fromString(textField.text))
  }

  override fun commitEdit(newValue: S?) {
    editingCellController(null)
    super.commitEdit(newValue)
    treeTableView.requestFocus()
  }

  override fun updateItem(item: S?, empty: Boolean) {
    super.updateItem(item, empty)
    if (treeTableView.focusModel.isFocused(treeTableRow.index, tableColumn)) {
      styleClass.add("focused")
    } else {
      styleClass.removeAll("focused")
    }
    updateItem(this, converter, null, null, textField)
  }
}

fun <T> startEdit(cell: Cell<T>, converter: StringConverter<T>, hbox: HBox?, graphic: Node?, textField: TextField) {
  textField.text = getItemText(cell, converter)
  cell.text = null
  if (graphic != null) {
    hbox?.children?.setAll(graphic, textField)
    cell.setGraphic(hbox)
  } else {
    cell.setGraphic(textField)
  }

  // requesting focus so that key input can immediately go into the
  // TextField (see RT-28132)
  Platform.runLater {
    textField.selectAll()
    textField.requestFocus()
  }
}

fun <T> cancelEdit(cell: Cell<T>, converter: StringConverter<T>, graphic: Node?) {
  cell.text = getItemText(cell, converter)
  cell.graphic = graphic
}

fun <T> createTextField(cell: Cell<T>, converter: StringConverter<T>) =
  TextField(getItemText(cell, converter)).also { textField ->
    // Use onAction here rather than onKeyReleased (with check for Enter),
    // as otherwise we encounter RT-34685
    textField.onAction = EventHandler { event: ActionEvent ->
      cell.commitEdit(converter.fromString(textField.text))
      event.consume()
    }
    textField.onKeyReleased = EventHandler { t: KeyEvent ->
      if (t.code == KeyCode.ESCAPE) {
        cell.cancelEdit()
        t.consume()
      }
    }
  }

private fun <T> getItemText(cell: Cell<T>?, converter: StringConverter<T>?) =
  converter?.toString(cell?.item) ?: cell?.item?.toString() ?: ""


private fun <T> updateItem(cell: Cell<T>, converter: StringConverter<T>, hbox: HBox?, graphic: Node?, textField: TextField?) {
  if (cell.isEmpty) {
    cell.text = null
    cell.setGraphic(null)
  } else {
    if (cell.isEditing) {
      if (textField != null) {
        textField.text = getItemText(cell, converter)
      }
      cell.text = null
      if (graphic != null) {
        hbox!!.children.setAll(graphic, textField)
        cell.setGraphic(hbox)
      } else {
        cell.setGraphic(textField)
      }
    } else {
      cell.text = getItemText(cell, converter)
      cell.setGraphic(graphic)
    }
  }
}

class MyVirtualFlow<T: IndexedCell<*>> : VirtualFlow<T>() {
  fun vbarWidth() = vbar.width
}
