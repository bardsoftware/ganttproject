package biz.ganttproject.ganttview

import biz.ganttproject.app.dialog
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.util.Callback
import net.sourceforge.ganttproject.CustomPropertyDefinition
import net.sourceforge.ganttproject.CustomPropertyManager
import org.controlsfx.control.PropertySheet
import org.controlsfx.property.BeanProperty
import java.beans.PropertyDescriptor

/**
 * @author dbarashev@bardsoftware.com
 */
class ColumnManager(private val columnList: ColumnList) {
  private val listView: ListView<ColumnItem> = ListView()
  private val propertySheet: PropertySheet
  private val propertySheetAction: Button
  private val propertyDescriptors = listOf(PropertyDescriptor("title", CustomPropertyEditable::class.java))
  private val customPropertyEditable = CustomPropertyEditable()
  internal val content: Node
  init {
    listView.items = FXCollections.observableArrayList(columnList.exportData().map { ColumnItem(it, it.isVisible) })
    listView.cellFactory = Callback { CellImpl() }
    propertySheet = PropertySheet(FXCollections.observableArrayList(
      propertyDescriptors.map { BeanProperty(customPropertyEditable, it) }.toList()
    )).also {
      it.isModeSwitcherVisible = false
      it.isSearchBoxVisible = false
    }
    propertySheetAction = Button("Add")
    val propertySheetBox = vbox {
      this.add(propertySheet, Pos.CENTER_LEFT, Priority.NEVER)
      this.add(propertySheetAction, Pos.CENTER_RIGHT, Priority.NEVER)
      this.vbox
    }
    content = HBox().also {
      it.children.addAll(listView, propertySheetBox)
    }
  }
}

internal data class CustomPropertyEditable(var title: String = "")
internal data class ColumnItem(val column: ColumnList.Column, var isVisible: Boolean)

private class CellImpl : ListCell<ColumnItem>() {
  override fun updateItem(item: ColumnItem?, empty: Boolean) {
    super.updateItem(item, empty)
    if (item == null || empty) {
      text = ""
      graphic = null
      return
    }
    text = item.column.name
    if (item.isVisible) {
      graphic = FontAwesomeIconView(FontAwesomeIcon.EYE)
      styleClass.remove("is-hidden")
    } else {
      graphic = FontAwesomeIconView(FontAwesomeIcon.EYE_SLASH)
      styleClass.add("is-hidden")
    }
  }
}

fun show(columnList: ColumnList, customColumnsManager: CustomPropertyManager) {
  dialog { dlg ->
    val columnManager = ColumnManager(columnList)
    dlg.setContent(columnManager.content)
  }
}
