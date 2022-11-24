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

import biz.ganttproject.FXUtil
import biz.ganttproject.app.MenuBuilder
import biz.ganttproject.app.MenuBuilderFx
import biz.ganttproject.lib.fx.treetable.TreeTableRowSkin
import biz.ganttproject.lib.fx.treetable.TreeTableViewSkin
import biz.ganttproject.lib.fx.treetable.VirtualFlow
import biz.ganttproject.walkTree
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.util.Callback
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.round

/**
 * @author dbarashev@bardsoftware.com
 */
class GPTreeTableView<T>(rootItem: TreeItem<T>) : TreeTableView<T>(rootItem) {
  internal val tableMenu = ContextMenu()
  var contextMenuActions: (MenuBuilder) -> Unit = { }
  var onProperties: () -> Unit = {}
  var onColumnResize: () -> Unit = {}
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
      it.bindHeaderFont()

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

  fun scrollTo(item: TreeItem<T>) {
    skin?.let { (it as GPTreeTableViewSkin<T>).scrollTo(getRow(item)) }
  }

  override fun requestFocus() {
    super.requestFocus()
    val focusedCell = this.focusModel.focusedCell
    if (focusedCell.tableColumn == null && columns.size > 0) {
      this.focusModel.focus(focusedCell.row, columns[0])
    }
  }

  override fun resizeColumn(column: TreeTableColumn<T, *>?, delta: Double) =
    super.resizeColumn(column, delta).also {
      onColumnResize()
    }

  fun vbarWidth(): Double = skin?.let { (it as GPTreeTableViewSkin<T>).vbarWidth() } ?: 0.0
  fun setColumns(tableColumns: List<TreeTableColumn<T, out Any>>) {
    val totalPrefWidth = tableColumns.filter { it.isVisible }.sumOf { it.prefWidth }
    prefWidth = totalPrefWidth + vbarWidth()
    columns.setAll(tableColumns)
    (skin as? GPTreeTableViewSkin<*>)?.let { it.applyHeaderFont() }
  }

  fun autosizeColumns() {
    val totalWidth = columns.sumOf { autosizeColumn(it as TreeTableColumn<T, Any>) }
    this.prefWidth = totalWidth
  }

  private fun autosizeColumn(tc: TreeTableColumn<T, Any>): Double {
    val cellFactory = tc.cellFactory ?: return 0.0
    val cell = cellFactory.call(tc) ?: return 0.0

    // set this property to tell the TableCell we want to know its actual
    // preferred width, not the width of the associated TableColumnBase
    cell.properties["deferToParentPrefWidth"] = true

    // determine cell padding
    var padding = 0.0
    val node = cell.skin?.node
    if (node is Region) {
      padding = node.snappedLeftInset() + node.snappedRightInset()
    }

    val treeTableRow = MyTreeTableRow<T>()
    treeTableRow.updateTreeTableView(this)

    val rows = expandedItemCount
    var maxWidth = 0.0
    for (row in 0 until rows) {
      treeTableRow.updateIndex(row)
      treeTableRow.updateTreeItem(getTreeItem(row))

      cell.updateTreeTableColumn(tc)
      cell.updateTreeTableView(this)
      cell.updateTreeTableRow(treeTableRow)
      cell.updateIndex(row)

      if (cell.text?.isNotEmpty() == true || cell.graphic != null) {
        children.add(cell)
        cell.applyCss()
        maxWidth = max(maxWidth, cell.prefWidth(-1.0))
        children.remove(cell)
        //println("text=${cell.text} maxWidth=$maxWidth")
      }
    }

    // dispose of the cell to prevent it retaining listeners (see RT-31015)
    cell.updateIndex(-1)
    tc.prefWidth = maxWidth + padding
    return maxWidth + padding
  }

  fun registerTreeItem(treeItem: TreeItem<T>) {
    treeItem.expandedProperty().addListener { _, _, _ ->
      Platform.runLater {
        skin?.let { (it as GPTreeTableViewSkin<T>).updateScrollValue() }
      }
    }
  }

  private val refreshCommand = AtomicReference<Runnable?>(null)

  fun coalescingRefresh() {
    if (refreshCommand.get() == null) {
      val runnable = Runnable {
        // perhaps we can live without refresh call
        // refresh()
        val focusedCell = focusModel.focusedCell
        Platform.runLater {
          focusModel.focus(-1)
          focusModel.focus(focusedCell)
        }
        refreshCommand.set(null)
      }
      if (refreshCommand.compareAndSet(null, runnable)) {
        Platform.runLater(runnable)
      }
    }

  }
}

class GPTreeTableViewSkin<T>(private val table: GPTreeTableView<T>) : TreeTableViewSkin<T>(table) {

  val scrollValue = SimpleDoubleProperty()
  val headerHeight: ReadOnlyDoubleProperty
    get() = tableHeaderRow.heightProperty()
  val fullHeaderHeight: Double get() = headerHeight.value + tableHeaderRow.boundsInParent.minX

  internal fun updateScrollValue() {
    updateItemCount()
    var totalCellHeight = 0.0
    for (idx in 0 until virtualFlow.cellCount) {
      totalCellHeight += virtualFlow.getCell(idx).height
    }
    val result = (totalCellHeight - virtualFlow.height) * virtualFlow.position
    scrollValue.value = result
  }

  init {
    this.virtualFlow.positionProperty().addListener { _, _, _ ->
      Platform.runLater { updateScrollValue() }

    }

    table.addEventFilter(KeyEvent.KEY_PRESSED) {event ->
      if ((event.code == KeyCode.LEFT || event.code == KeyCode.RIGHT)
        && event.target !is TextField
        && !event.isAltDown
        && !event.isShiftDown
        && !event.isMetaDown
        && !event.isControlDown) {

        event.consume()
        if (event.code == KeyCode.LEFT) {
          table.focusModel.focusLeftCell()
        } else {
          table.focusModel.focusRightCell()
        }
        return@addEventFilter
      }
    }
    table.addEventHandler(KeyEvent.KEY_PRESSED) {
      if (it.code == KeyCode.PAGE_DOWN) {
        pageDown()
        it.consume()
        return@addEventHandler
      }
      if (it.code == KeyCode.PAGE_UP) {
        pageUp()
        it.consume()
        return@addEventHandler
      }
    }
  }

  override fun createVirtualFlow(): VirtualFlow<TreeTableRow<T>> {
    return MyVirtualFlow()
  }

  fun scrollBy(value: Double) {
    this.virtualFlow.scrollPixels(value)
  }

  fun vbarWidth() = (this.virtualFlow as MyVirtualFlow).vbarWidth()

  fun scrollTo(row: Int) {
    this.virtualFlow.scrollTo(row)
    updateScrollValue()
  }

  private fun pageDown() {
    this.virtualFlow.lastVisibleCell?.let { lastCell ->
      this.virtualFlow.scrollToTop(lastCell)
      this.table.selectionModel.clearSelection()
      this.table.selectionModel.select(lastCell.treeItem)
    }
  }

  private fun pageUp() {
    this.virtualFlow.firstVisibleCell?.let { firstCell ->
      this.virtualFlow.scrollToBottom(firstCell)
      this.table.selectionModel.clearSelection()
      this.table.selectionModel.select(firstCell.treeItem)
    }
  }

  fun applyHeaderFont() {
    FXUtil.runLater {
      tableHeaderRow.walkTree {
        if (it is Labeled) {
          it.font = applicationFont.value
          //println("this=$it itfont=${it.font} app font=${applicationFont.value}")
          //it.style = """-fx-font-family: ${applicationFont.value.family}; -fx-font-size: ${applicationFont.value.size } """
        }
      }
    }
  }
  fun bindHeaderFont() {
    applicationFont.addListener { _, _, newValue ->
      applyHeaderFont()
    }
    applyHeaderFont()
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


class MyVirtualFlow<T: IndexedCell<*>> : VirtualFlow<T>() {
  fun vbarWidth() = if (this.width > 0.0 && vbar.isVisible) vbar.width else 0.0
  init {
    children.remove(hbar)
  }
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
    if (newValue == 0.0) {
      // This means that we're just collapsing the table pane. No action needed. When we expand it back, the column widths
      // will be kept.
      return
    }
    // We will spread the delta across all visible columns proportionally to their widths
    val columnWidths = visibleColumns.map { it.width }
    val totalWidth = visibleColumns.sumOf { it.width }
    val columnWeights = columnWidths.map { it / totalWidth }

    val delta = newValue - totalWidth - table.vbarWidth()
    val newWidths =
      if (delta > 0) {
        // If the table gets wider, we just add delta to all columns proportionally.
        columnWidths.zip(columnWeights).map { it.first + delta * it.second }
      } else {
        // If the table shrinks down, we first proportionally decrement widths...
        val newWidths = columnWidths.zip(columnWeights).map { it.first + delta * it.second }
        // .. however, we don't want to make column widths less than some minimum, so those columns
        // which reached the minimum threshold will borrow some width from the remaining ones.
        val totalOverdraft = newWidths.filter { it < 20.0 }.map { 20.0 - it }.sum()
        newWidths.zip(columnWeights).map {
          if (it.first < 20.0) {
            20.0
          } else {
            it.first - totalOverdraft * it.second
          }
        }
      }.map { round(it) }
    val newTotalWidth = newWidths.sum()
    val diff = newValue - (newTotalWidth + table.vbarWidth())
    //println("newValue=$newValue old width=$columnWidths new width=$newWidths diff=$diff vbar=${table.vbarWidth()}")
    visibleColumns.zip(newWidths).forEach { it.first.prefWidth = it.second }
    if (diff > 0) {
      visibleColumns.last().prefWidth += diff
    }
  }
}

