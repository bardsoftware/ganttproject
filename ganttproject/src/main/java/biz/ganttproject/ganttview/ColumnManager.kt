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
package biz.ganttproject.ganttview

import biz.ganttproject.app.LocalizedString
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.util.Callback
import net.sourceforge.ganttproject.CustomPropertyClass
import net.sourceforge.ganttproject.CustomPropertyDefinition
import net.sourceforge.ganttproject.CustomPropertyManager
import org.controlsfx.control.PropertySheet
import org.controlsfx.property.BeanProperty
import java.beans.PropertyDescriptor

/**
 * @author dbarashev@bardsoftware.com
 */
class ColumnManager(private val currentTableColumns: ColumnList, private val customColumnsManager: CustomPropertyManager) {

  internal val btnAddController = BtnController(onAction = this::onAddColumn)
  internal val btnDeleteController = BtnController(onAction = this::onDeleteColumn)

  private val listItems = FXCollections.observableArrayList<ColumnAsListItem>()
  private val listView: ListView<ColumnAsListItem> = ListView()
  private val propertySheet: PropertySheet = PropertySheet()
  private val customPropertyEditor = CustomPropertyEditor(customColumnsManager, propertySheet, btnDeleteController, listItems)
  internal val content: Node
  private val mergedColumns: MutableList<ColumnList.Column> = mutableListOf()
  init {
    mergedColumns.addAll(currentTableColumns.exportData())
    listItems.setAll(mergedColumns.map { ColumnAsListItem(it, it.isVisible, false, customColumnsManager) })
    customColumnsManager.definitions.forEach { def ->
      if (mergedColumns.find { it.id == def.id } == null) {
        val columnStub = ColumnList.ColumnStub(def.id, def.name, false, -1, -1)
        mergedColumns.add(columnStub)
        listItems.add(ColumnAsListItem(columnStub, columnStub.isVisible, true, customColumnsManager))
      }
    }
    listView.items = listItems
    listView.cellFactory = Callback { CellImpl() }
    propertySheet.items.setAll(FXCollections.observableArrayList(customPropertyEditor.props))
    propertySheet.isModeSwitcherVisible = false
    propertySheet.isSearchBoxVisible = false

    content = HBox().also {
      it.styleClass.add("content-pane")
      it.children.addAll(listView, propertySheet)
      HBox.setHgrow(propertySheet, Priority.ALWAYS)
    }

    listView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
      customPropertyEditor.item = newValue
    }

  }

  private fun onAddColumn() {
    val item = ColumnAsListItem(
      null,
      isVisible = true, isCustom = true, customColumnsManager
    ).also {
      it.title = "Untitled Custom Column"
    }
    listItems.add(item)
    listView.scrollTo(item)
    listView.selectionModel.select(item)
  }
  private fun onDeleteColumn() {
    listItems.removeAll(listView.selectionModel.selectedItems)
  }
  fun applyChanges() {
    mergedColumns.forEach { existing ->
      listItems.find { it.column?.id == existing.id } ?: run {
        customColumnsManager.definitions.find { def -> def.id == existing.id }?.let(customColumnsManager::deleteDefinition)
      }
    }

    listItems.forEach { columnItem ->
      columnItem.column?.let {
        it.isVisible = columnItem.isVisible
        if (columnItem.isCustom) {
          customColumnsManager.definitions.find { def -> def.id == it.id }?.fromColumnItem(columnItem)
        }
      } ?: run {
        val def = customColumnsManager.createDefinition(columnItem.type.getCustomPropertyClass(), columnItem.title, columnItem.defaultValue)
        if (columnItem.isVisible) {
          mergedColumns.add(ColumnList.ColumnStub(def.id, def.name, true, mergedColumns.size, 50))
        }
      }
    }

    currentTableColumns.importData(ColumnList.Immutable.fromList(mergedColumns), false)
  }
}

internal data class BtnController(
  val isDisabled: SimpleBooleanProperty = SimpleBooleanProperty(false),
  val onAction: () -> Unit
)
internal enum class PropertyType(private val displayName: String) {
  STRING("Text"), INTEGER("Integer value"), DATE("Date"), DECIMAL("Numeric/decimal"), COLOR("Color"), BOOLEAN("True or false");

  override fun toString() = this.displayName
}

internal fun CustomPropertyDefinition.getPropertyType(): PropertyType = when (this.propertyClass) {
  CustomPropertyClass.TEXT -> PropertyType.STRING
  CustomPropertyClass.DATE -> PropertyType.DATE
  CustomPropertyClass.INTEGER -> PropertyType.INTEGER
  CustomPropertyClass.DOUBLE -> PropertyType.DECIMAL
  CustomPropertyClass.BOOLEAN -> PropertyType.BOOLEAN
}

internal fun PropertyType.getCustomPropertyClass(): CustomPropertyClass = when (this) {
  PropertyType.STRING -> CustomPropertyClass.TEXT
  PropertyType.INTEGER -> CustomPropertyClass.INTEGER
  PropertyType.DATE -> CustomPropertyClass.DATE
  PropertyType.BOOLEAN -> CustomPropertyClass.BOOLEAN
  PropertyType.DECIMAL -> CustomPropertyClass.DOUBLE
  else -> CustomPropertyClass.TEXT
}

internal fun CustomPropertyDefinition.fromColumnItem(item: ColumnAsListItem) {
  this.name = item.title
  if (item.defaultValue.trim().isNotBlank()) {
    this.defaultValueAsString = item.defaultValue
  }
  this.propertyClass = item.type.getCustomPropertyClass()
}

internal fun TaskDefaultColumn.getPropertyType(): PropertyType = when (this) {
  TaskDefaultColumn.ID, TaskDefaultColumn.DURATION, TaskDefaultColumn.COMPLETION -> PropertyType.INTEGER
  TaskDefaultColumn.BEGIN_DATE, TaskDefaultColumn.END_DATE -> PropertyType.DATE
  TaskDefaultColumn.COST -> PropertyType.DECIMAL
  TaskDefaultColumn.COLOR -> PropertyType.COLOR
  else -> PropertyType.STRING
}

internal class CustomPropertyEditor(
  private val customColumnsManager: CustomPropertyManager,
  private val propertySheet: PropertySheet,
  private val btnDeleteController: BtnController,
  private val listItems: ObservableList<ColumnAsListItem>
) {
  var isPropertyChangeIgnored = false
  var item: ColumnAsListItem? = null
  set(newValue) {
    isPropertyChangeIgnored = true
    field = newValue
    if (newValue != null) {
      value.title = newValue.title
      value.type = newValue.type
      if (newValue.isCustom) {
        propertySheet.isDisable = false
        btnDeleteController.isDisabled.value = false
        value.defaultValue = newValue.defaultValue
      } else {
        btnDeleteController.isDisabled.value = true
        //propertySheet.isDisable = true
      }
    }
    isPropertyChangeIgnored = false
  }
  val value = ColumnAsListItem(column = null, isVisible = true, isCustom = true, customColumnsManager = customColumnsManager)
  val title = BeanProperty(value, PropertyDescriptor("title", ColumnAsListItem::class.java))
  val type = BeanProperty(value, PropertyDescriptor("type", ColumnAsListItem::class.java))
  val defaultValue = BeanProperty(value, PropertyDescriptor("defaultValue", ColumnAsListItem::class.java))
  val props = listOf(title, type, defaultValue)
  init {
    props.forEach { it.observableValue.get().addListener { _, _, _ -> onPropertyChange() } }
  }
  private fun onPropertyChange() {
    if (!isPropertyChangeIgnored) {
      item?.title = value.title
      item?.type = value.type
      item?.defaultValue = value.defaultValue
      listItems.set(listItems.indexOf(item), item)
    }
  }
}

internal data class ColumnAsListItem(
  val column: ColumnList.Column?,
  var isVisible: Boolean,
  val isCustom: Boolean,
  val customColumnsManager: CustomPropertyManager
) {
  private val _title = SimpleStringProperty("")
  var title: String
    get() = _title.value
    set(value) { _title.value = value }
  fun titleProperty() = _title

  private val _type = SimpleObjectProperty(PropertyType.STRING)
  var type: PropertyType
    get() = _type.value
    set(value) { _type.value = value }
  fun typeProperty() = _type

  private val _defaultValue = SimpleStringProperty("")
  var defaultValue: String
    get() = _defaultValue.value
    set(value) { _defaultValue.value = value }
  fun defaultValueProperty() = _defaultValue

  init {
    if (column != null) {
      title = column.name
      type = run {
        if (isCustom) {
          customColumnsManager.definitions.find { it.id == column?.id }?.getPropertyType()
        } else {
          TaskDefaultColumn.find(column?.id)?.getPropertyType()
        }
      } ?: PropertyType.STRING
      defaultValue =
        if (!isCustom) ""
        else customColumnsManager.definitions.find { it.id == column.id }?.defaultValueAsString ?: ""
    }
  }

}

private class CellImpl : ListCell<ColumnAsListItem>() {
  private val iconVisible = MaterialIconView(MaterialIcon.VISIBILITY)
  private val iconHidden = MaterialIconView(MaterialIcon.VISIBILITY_OFF)
  private val iconPane = StackPane().also {
    it.onMouseClicked = EventHandler { evt ->
      item.isVisible = !item.isVisible
      updateItem(item, false)
    }
    //it.children.addAll(iconVisible, iconHidden)
  }

  init {
    styleClass.add("column-item-cell")
  }

  override fun updateItem(item: ColumnAsListItem?, empty: Boolean) {
    super.updateItem(item, empty)
    if (item == null || empty) {
      text = ""
      graphic = null
      return
    }
    text = item.title
    if (graphic == null) {
      graphic = iconPane
    }
    if (item.isVisible) {
      styleClass.remove("is-hidden")
      iconPane.children.setAll(iconVisible)
    } else {
      if (!styleClass.contains("is-hidden")) {
        styleClass.add("is-hidden")
        iconPane.children.setAll(iconHidden)
      }
    }
  }
}

fun show(columnList: ColumnList, customColumnsManager: CustomPropertyManager) {
  dialog { dlg ->
    dlg.addStyleClass("dlg-column-manager")
    dlg.addStyleSheet("/biz/ganttproject/ganttview/ColumnManager.css")
    dlg.setHeader(
      VBoxBuilder("header").apply {
        addTitle(LocalizedString("taskTable.columnManager.title", RootLocalizer)).also { hbox ->
          hbox.alignment = Pos.CENTER_LEFT
          hbox.isFillHeight = true
        }
      }.vbox
    )
    val columnManager = ColumnManager(columnList, customColumnsManager)
    dlg.setContent(columnManager.content)
    dlg.setupButton(ButtonType.APPLY) { btn ->
      btn.text = RootLocalizer.formatText("apply")
      btn.styleClass.add("btn-attention")
      btn.setOnAction {
        columnManager.applyChanges()
        dlg.hide()
      }
    }
    dlg.setupButton(ButtonType.CANCEL) { btn ->
      btn.text = "Add"
      ButtonBar.setButtonData(btn, ButtonBar.ButtonData.HELP)
      btn.disableProperty().bind(columnManager.btnAddController.isDisabled)
      btn.setOnAction {
        it.consume()
        columnManager.btnAddController.onAction()
      }
      btn.styleClass.addAll("btn-attention", "secondary")
    }
    dlg.setupButton(ButtonType.CANCEL) { btn ->
      btn.text = "Delete"
      ButtonBar.setButtonData(btn, ButtonBar.ButtonData.HELP_2)
      btn.disableProperty().bind(columnManager.btnDeleteController.isDisabled)
      btn.setOnAction {
        it.consume()
        columnManager.btnDeleteController.onAction()
      }
      btn.styleClass.addAll("btn-regular", "secondary")
    }
  }
}
