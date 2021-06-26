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
//import biz.ganttproject.lib.fx.treetable.TreeTableRowSkin
import com.sun.javafx.scene.control.behavior.TreeTableViewBehavior
//import biz.ganttproject.lib.fx.treetable.TreeTableViewSkin
//import biz.ganttproject.lib.fx.treetable.VirtualFlow
import com.sun.javafx.scene.control.inputmap.InputMap
import com.sun.javafx.scene.control.inputmap.KeyBinding
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.collections.MapChangeListener
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.control.skin.TreeTableRowSkin
import javafx.scene.control.skin.TreeTableViewSkin
import javafx.scene.control.skin.VirtualFlow
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

  init {
    rowFactory = Callback { view ->
      MyTreeTableRow()
    }
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

  fun vbarWidth(): Double = skin?.let { (it as GPTreeTableViewSkin<T>).vbarWidth() } ?: 0.0
}

class GPTreeTableViewSkin<T>(control: GPTreeTableView<T>) : TreeTableViewSkin<T>(control) {

  val scrollValue = SimpleDoubleProperty()
  val headerHeight: ReadOnlyDoubleProperty
  get() = tableHeaderRow.heightProperty()
  val fullHeaderHeight: Double get() = headerHeight.value + tableHeaderRow.boundsInParent.minX
  private val contentWidthListener = mutableMapOf<Double, ()->Unit>()
  private val contentWidth: Double get() = skinnable.properties["TableView.contentWidth"]?.toString()?.toDoubleOrNull() ?: 0.0

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
        //println("contentWidth=$value vbar width=${(virtualFlow as MyVirtualFlow).vbarWidth()} #listeners=${contentWidthListener}")
        //println("pseuidoclasses: ${skinnable.pseudoClassStates}")
        //println("insets: ${skinnable.insets} width=${skinnable.width}")
        //skinnable.childrenUnmodifiable.forEach { println("$it width=${(it as Region).width} insets=${it.insets}") }
        value += (virtualFlow as MyVirtualFlow).vbarWidth()

        // Sometimes borders or insets add a few pixels to the content width,
        // e.g. it may become 349 when we expect 350. It is difficult to track such
        // errors, so we just do a sort of "approximate match" here.
        for (key in value.toInt()-5 .. value.toInt()+5) {
          contentWidthListener.remove(key.toDouble())?.invoke()

        }
      }
    })
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
    this.mappings.filter {
      it.mappingKey.let { it is KeyBinding && predicate(it) }
    }.forEach { it.isDisabled = true }
    this.childInputMaps.forEach { it.removeKey(predicate) }
  }
  override fun createVirtualFlow(): VirtualFlow<TreeTableRow<T>> {
    return MyVirtualFlow();
  }

  fun onContentWidthChange(expected: Double, code: () -> Unit) {
    if (contentWidth == expected) {
      code()
    } else {
      contentWidthListener[expected] = code
    }
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
  fun vbarWidth() = if (this.width > 0.0) vbar.width else 0.0

}

class MyTreeTableRow<T> : TreeTableRow<T>() {
  override fun createDefaultSkin() = TreeTableRowSkin<T>(this)

  init {
    disclosureNode = HBox().also { hbox ->
      hbox.styleClass.setAll("tree-disclosure-node")
      hbox.isMouseTransparent = true
      hbox.alignment = Pos.CENTER
      FontAwesomeIconView(FontAwesomeIcon.CHEVRON_RIGHT).also {
        it.styleClass.add("arrow")
        hbox.children.add(it)
      }
      hbox.prefHeightProperty().bind(heightProperty());
    }
  }
}
