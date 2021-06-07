package biz.ganttproject.app

import javafx.application.Platform
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.skin.TreeTableViewSkin
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Region
import javafx.util.StringConverter

/**
 * @author dbarashev@bardsoftware.com
 */
class GPTreeTableView<T>(rootItem: TreeItem<T>) : TreeTableView<T>(rootItem) {
  internal val tableMenu = ContextMenu()

  init {
    columnResizePolicy = CONSTRAINED_RESIZE_POLICY;
    stylesheets.add("/biz/ganttproject/lib/fx/TreeTable.css")
    styleClass.add("gp-tree-table-view")
    tableMenu.items.add(MenuItem(RootLocalizer.formatText("columns.manage.label")))
  }
  override fun createDefaultSkin(): Skin<*>? {
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
}

class GPTreeTableViewSkin<T>(control: GPTreeTableView<T>) : TreeTableViewSkin<T>(control) {

  val scrollValue = SimpleDoubleProperty()
  val headerHeight: ReadOnlyDoubleProperty
  get() = tableHeaderRow.heightProperty()
  val fullHeaderHeight: Double get() = headerHeight.value + tableHeaderRow.boundsInParent.minX

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

class TextCell<T>(private val converter: StringConverter<String>) : TreeTableCell<T, String>() {
  private val textField: TextField = createTextField(this, converter)

  override fun startEdit() {
    if (!isEditable) {
      return
    }
    super.startEdit()

    if (isEditing) {
      Platform.runLater {
        treeTableView.requestFocus()
        startEdit(this, converter, null, null, textField)
      }
    }
  }

  override fun cancelEdit() {
    super.cancelEdit()
    cancelEdit(this, converter, null)
    treeTableView.requestFocus()
  }

  override fun commitEdit(newValue: String?) {
    super.commitEdit(newValue)
    treeTableView.requestFocus()
  }

  override fun updateItem(item: String?, empty: Boolean) {
    super.updateItem(item, empty)
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
