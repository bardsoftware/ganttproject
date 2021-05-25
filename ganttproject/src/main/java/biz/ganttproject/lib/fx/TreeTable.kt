package biz.ganttproject.app

import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.Skin
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableView
import javafx.scene.control.skin.TreeTableViewSkin

/**
 * @author dbarashev@bardsoftware.com
 */
class GPTreeTableView<T>(rootItem: TreeItem<T>) : TreeTableView<T>(rootItem) {
  init {
    columnResizePolicy = CONSTRAINED_RESIZE_POLICY;
    stylesheets.add("/biz/ganttproject/lib/fx/TreeTable.css")
    styleClass.add("gp-tree-table-view")
  }
  override fun createDefaultSkin(): Skin<*>? {
    return GPTreeTableViewSkin(this).also {
      it.scrollValue.addListener { _, _, newValue -> this.scrollListener(newValue.toDouble()) }
    }
  }

  val headerHeight: Double
  get() = (skin as GPTreeTableViewSkin<T>).headerHeight

  var scrollListener: (Double)->Unit = {}
  fun addScrollListener(listener: (Double)->Unit) {
    this.scrollListener = listener
  }

  fun scrollBy(value: Double) {
    (skin as GPTreeTableViewSkin<T>).scrollBy(value)
  }
}

class GPTreeTableViewSkin<T>(control: GPTreeTableView<T>) : TreeTableViewSkin<T>(control) {

  val scrollValue = SimpleDoubleProperty()
  val headerHeight: Double
  get() = tableHeaderRow.height

  init {
    this.virtualFlow.positionProperty().addListener { _, _, newValue ->
      var totalCellHeight = 0.0
      for (idx in 0 until virtualFlow.cellCount) {
        totalCellHeight += virtualFlow.getCell(idx).height
      }
      val result = (totalCellHeight - virtualFlow.height) * virtualFlow.position
      scrollValue.value = result
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
