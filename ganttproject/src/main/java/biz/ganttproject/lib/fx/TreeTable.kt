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

//import javafx.scene.control.skin.TreeTableRowSkin
//import javafx.scene.control.skin.TreeTableViewSkin
//import javafx.scene.control.skin.VirtualFlow
import biz.ganttproject.app.MenuBuilder
import biz.ganttproject.app.MenuBuilderFx
import biz.ganttproject.ganttview.NewTaskActor
import biz.ganttproject.lib.fx.treetable.TreeTableRowSkin
import biz.ganttproject.lib.fx.treetable.TreeTableViewSkin
import biz.ganttproject.lib.fx.treetable.VirtualFlow
import com.sun.javafx.scene.control.behavior.TreeTableViewBehavior
import com.sun.javafx.scene.control.inputmap.InputMap
import com.sun.javafx.scene.control.inputmap.KeyBinding
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.util.Callback
import org.apache.commons.lang3.reflect.FieldUtils

/**
 * @author dbarashev@bardsoftware.com
 */
class GPTreeTableView<T>(rootItem: TreeItem<T>) : TreeTableView<T>(rootItem) {
  internal val tableMenu = ContextMenu()
  var contextMenuActions: (MenuBuilder) -> Unit = { }
  var tableMenuActions: (MenuBuilder) -> Unit = {}
  private val resizePolicy = MyColumnResizePolicy<Any>(this, widthProperty())
  init {
    rowFactory = Callback {
      MyTreeTableRow()
    }
    stylesheets.add("/biz/ganttproject/lib/fx/TreeTable.css")
    styleClass.add("gp-tree-table-view")
    focusModel.focusedCellProperty().addListener { _, _, newValue ->
      if (newValue.column == -1 && columns.isNotEmpty()) {
        focusModel.focus(newValue.row, columns[0])
      }
      refresh()
    }
    addEventHandler(MouseEvent.MOUSE_CLICKED) { event ->
      if (event.button == MouseButton.SECONDARY) {
        val contextMenu = ContextMenu()
        contextMenuActions(MenuBuilderFx(contextMenu))
        contextMenu.show(this, event.screenX, event.screenY)
      }
    }
    columnResizePolicy = resizePolicy
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

  fun vbarWidth(): Double = skin?.let { (it as GPTreeTableViewSkin<T>).vbarWidth() } ?: 0.0
  fun setColumns(tableColumns: List<TreeTableColumn<T, out Any>>) {
    val totalPrefWidth = tableColumns.filter { it.isVisible }.sumOf { it.prefWidth }
    prefWidth = totalPrefWidth + vbarWidth()
    columns.setAll(tableColumns)
  }
}

class GPTreeTableViewSkin<T>(control: GPTreeTableView<T>) : TreeTableViewSkin<T>(control) {

  val scrollValue = SimpleDoubleProperty()
  val headerHeight: ReadOnlyDoubleProperty
  get() = tableHeaderRow.heightProperty()
  val fullHeaderHeight: Double get() = headerHeight.value + tableHeaderRow.boundsInParent.minX

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
      control.tableMenuActions(MenuBuilderFx(control.tableMenu))
      control.tableMenu.show(cornerRegion, Side.BOTTOM, 0.0, 0.0)
      me.consume()
    }
    val behavior = FieldUtils.readField(this, "behavior", true) as TreeTableViewBehavior<T>
    behavior.inputMap.removeKey {
      (it.code == KeyCode.LEFT || it.code == KeyCode.RIGHT)
        && it.alt != KeyBinding.OptionalBoolean.TRUE
        && it.shift != KeyBinding.OptionalBoolean.TRUE
        && it.meta != KeyBinding.OptionalBoolean.TRUE
        && it.ctrl != KeyBinding.OptionalBoolean.TRUE
    }
  }

  private fun (InputMap<*>).removeKey(predicate: (KeyBinding) -> Boolean) {
    this.mappings.filter { mapping ->
      mapping.mappingKey.let { it is KeyBinding && predicate(it) }
    }.forEach { it.isDisabled = true }
    this.childInputMaps.forEach { it.removeKey(predicate) }
  }
  override fun createVirtualFlow(): VirtualFlow<TreeTableRow<T>> {
    return MyVirtualFlow()
  }

  fun scrollBy(value: Double) {
    this.virtualFlow.scrollPixels(value)
  }

  fun vbarWidth() = (this.virtualFlow as MyVirtualFlow).vbarWidth()
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


class MyVirtualFlow<T: IndexedCell<*>> : VirtualFlow<T>() {
  fun vbarWidth() = if (this.width > 0.0 && vbar.isVisible) vbar.width else 0.0
}

class MyTreeTableRow<T> : TreeTableRow<T>() {
  override fun createDefaultSkin() = TreeTableRowSkin(this)

  init {
    disclosureNode = HBox().also { hbox ->
      hbox.styleClass.setAll("tree-disclosure-node")
      hbox.isMouseTransparent = true
      hbox.alignment = Pos.CENTER
      FontAwesomeIconView(FontAwesomeIcon.CHEVRON_RIGHT).also {
        it.styleClass.add("arrow")
        hbox.children.add(it)
      }
      hbox.prefHeightProperty().bind(heightProperty())
    }
  }
}

class MyColumnResizePolicy<S>(private val table: GPTreeTableView<*>, tableWidth: ReadOnlyDoubleProperty)
  : Callback<TreeTableView.ResizeFeatures<S>, Boolean> {
  init {
    tableWidth.addListener { _, _, newValue -> resizeTable(newValue.toDouble())}
  }
  override fun call(param: TreeTableView.ResizeFeatures<S>): Boolean {
    param.column?.let { thisCol ->
      val visibleColumns = param.table.columns.filter { it.isVisible }
      val idxThis = visibleColumns.indexOfFirst { it == thisCol }
      if (idxThis < visibleColumns.size - 1) {
        val nextCol = visibleColumns[idxThis + 1]
        thisCol.prefWidth += param.delta
        nextCol.prefWidth -= param.delta
      }
    }
    return true
  }

  private fun resizeTable(newValue: Double) {
    val visibleColumns = table.columns.filter { it.isVisible }
    if (visibleColumns.isEmpty()) {
      return
    }
    val totalWidth = visibleColumns.sumOf { it.width }
    var delta = newValue - totalWidth - table.vbarWidth()
    if (delta > 0) {
      visibleColumns.last().prefWidth += delta
    } else {
      delta = -delta
      for (col in visibleColumns.reversed()) {
        val decrement = kotlin.math.min(col.width - 30, delta)
        col.prefWidth -=  decrement
        delta -= decrement
        if (delta <= 0) {
          break
        }
      }
    }
  }
}
