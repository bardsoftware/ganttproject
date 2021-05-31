package biz.ganttproject.app

import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.event.EventHandler
import javafx.geometry.Side
import javafx.scene.control.*
import javafx.scene.control.skin.TreeTableViewSkin
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region

/**
 * @author dbarashev@bardsoftware.com
 */
class GPTreeTableView<T>(rootItem: TreeItem<T>) : TreeTableView<T>(rootItem) {
  internal val tableMenu = ContextMenu()

  init {
    columnResizePolicy = CONSTRAINED_RESIZE_POLICY;
    stylesheets.add("/biz/ganttproject/lib/fx/TreeTable.css")
    styleClass.add("gp-tree-table-view")
    tableMenu.items.add(MenuItem("Manage columns"))
  }
  override fun createDefaultSkin(): Skin<*>? {
    return GPTreeTableViewSkin(this).also {
      it.scrollValue.addListener { _, _, newValue -> this.scrollListener(newValue.toDouble()) }
      it.headerHeight.addListener { _, _, newValue -> headerHeight.value = newValue.toDouble() }
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
}

class GPTreeTableViewSkin<T>(control: GPTreeTableView<T>) : TreeTableViewSkin<T>(control) {

  val scrollValue = SimpleDoubleProperty()
  val headerHeight: ReadOnlyDoubleProperty
  get() = tableHeaderRow.heightProperty()

  init {
    this.virtualFlow.positionProperty().addListener { _, _, newValue ->
      var totalCellHeight = 0.0
      for (idx in 0 until virtualFlow.cellCount) {
        totalCellHeight += virtualFlow.getCell(idx).height
      }
      val result = (totalCellHeight - virtualFlow.height) * virtualFlow.position
      scrollValue.value = result
    }
    val cornerRegion = this.tableHeaderRow.lookup(".show-hide-columns-button") as Region
    cornerRegion.onMousePressed = EventHandler { me: MouseEvent ->
      // show a popupMenu which lists all columns
      control.tableMenu.show(cornerRegion, Side.BOTTOM, 0.0, 0.0)
      me.consume()
    }

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
