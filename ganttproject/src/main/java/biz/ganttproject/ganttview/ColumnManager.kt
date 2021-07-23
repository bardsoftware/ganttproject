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
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.util.Callback
import net.sourceforge.ganttproject.CustomPropertyManager
import org.controlsfx.control.PropertySheet
import org.controlsfx.property.BeanProperty
import java.beans.PropertyDescriptor

/**
 * @author dbarashev@bardsoftware.com
 */
class ColumnManager(private val currentTableColumns: ColumnList, private val customColumnsManager: CustomPropertyManager) {
  fun applyChanges() {
    listItems.forEach { columnItem ->
      columnItem.column.isVisible = columnItem.isVisible
    }
    currentTableColumns.importData(ColumnList.Immutable.fromList(mergedColumns), false)
  }

  private val listItems: ObservableList<ColumnItem>
  private val listView: ListView<ColumnItem> = ListView()
  private val propertySheet: PropertySheet
  private val propertySheetAction: Button
  private val propertyDescriptors = listOf(
    PropertyDescriptor("title", CustomPropertyEditable::class.java)
  )
  private val customPropertyEditable = CustomPropertyEditable()
  internal val content: Node
  private val mergedColumns: MutableList<ColumnList.Column> = currentTableColumns.exportData()
  init {
    listItems = FXCollections.observableArrayList(mergedColumns.map { ColumnItem(it, it.isVisible, false) })
    customColumnsManager.definitions.forEach { def ->
      if (mergedColumns.find { it.id == def.id } == null) {
        val columnStub = ColumnList.ColumnStub(def.id, def.name, false, -1, -1)
        mergedColumns.add(columnStub)
        listItems.add(ColumnItem(columnStub, columnStub.isVisible, true))
      }
    }
    listView.items = listItems
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
      it.styleClass.add("content-pane")
      it.children.addAll(listView, propertySheetBox)
    }

    listView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
      if (newValue.isCustom) {
        propertySheet.isDisable = false
      } else {
        propertySheet.isDisable = true
      }
    }

  }
}

internal data class CustomPropertyEditable(var title: String = "")
internal data class ColumnItem(val column: ColumnList.Column, var isVisible: Boolean, val isCustom: Boolean)

private class CellImpl : ListCell<ColumnItem>() {
  private val iconVisible = MaterialIconView(MaterialIcon.VISIBILITY)
  private val iconHidden = MaterialIconView(MaterialIcon.VISIBILITY_OFF)
  private val iconPane = StackPane().also {
    it.onMouseClicked = EventHandler { evt ->
      println("icon clicked!!! item=$item")
      item.isVisible = !item.isVisible
      updateItem(item, false)
    }
    //it.children.addAll(iconVisible, iconHidden)
  }

  init {
    styleClass.add("column-item-cell")
  }

  override fun updateItem(item: ColumnItem?, empty: Boolean) {
    super.updateItem(item, empty)
    if (item == null || empty) {
      text = ""
      graphic = null
      return
    }
    text = item.column.name
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
      }

    }
  }
}
