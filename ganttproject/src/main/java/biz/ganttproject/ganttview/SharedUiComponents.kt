/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.ganttview

import biz.ganttproject.app.DialogController
import biz.ganttproject.app.Localizer
import biz.ganttproject.app.PropertySheetBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.GPObservable
import biz.ganttproject.core.option.ObservableProperty
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.createToggleSwitch
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.util.Callback

data class ShowHideListItem(val text: String, val isVisible: GPObservable<Boolean>)

/**
 * UI components that render columns in the list view.
 */
internal class ShowHideListCell<T>(private val converter: (T)-> ShowHideListItem) : ListCell<T>() {
  private val iconVisible = MaterialIconView(MaterialIcon.VISIBILITY)
  private val iconHidden = MaterialIconView(MaterialIcon.VISIBILITY_OFF)
  private val iconPane = StackPane().also {
    it.onMouseClicked = EventHandler { _ ->
      theItem.isVisible.value = !theItem.isVisible.value
      updateTheItem(theItem)
    }
  }

  val theItem by lazy { converter(item) }
  init {
    styleClass.add("column-item-cell")
    alignment = Pos.CENTER_LEFT
  }

  override fun updateItem(item: T?, empty: Boolean) {
    super.updateItem(item, empty)
    if (item == null || empty) {
      text = ""
      graphic = null
      return
    }
    updateTheItem(converter(item))
  }

  private fun updateTheItem(item: ShowHideListItem) {
    text = item.text
    if (text.isEmpty()) {
      text = " "
    }
    if (graphic == null) {
      graphic = iconPane
    }
    if (item.isVisible.value) {
      styleClass.add("is-visible")
      styleClass.remove("is-hidden")
      iconPane.children.setAll(iconVisible)
    } else {
      if (!styleClass.contains("is-hidden")) {
        styleClass.remove("is-visible")
        styleClass.add("is-hidden")
        iconPane.children.setAll(iconHidden)
      }
    }
  }
}

/**
 * UI for editing properties of the selected list item.
 */
open internal class ItemEditorPane<T>(
  private val editItem: ObservableProperty<T>,
  fields: List<ObservableProperty<*>>,
  localizer: Localizer) {

  fun focus() {
    propertySheet.requestFocus()
    //editors["title"]?.editor?.requestFocus()
    onEdit()
  }

  protected open fun onEdit() {}

  val node: Node by lazy {
    vbox {
      addClasses("property-sheet-box")
      add(visibilityTogglePane)
      add(propertySheetLabel, Pos.CENTER_LEFT, Priority.NEVER)
      add(propertySheet.node, Pos.CENTER, Priority.ALWAYS)
      add(errorPane)
    }
  }

  internal val visibilityToggle = createToggleSwitch()
  internal val visibilityTogglePane = HBox().also {
    it.styleClass.add("visibility-pane")
    it.children.add(visibilityToggle)
    it.children.add(Label(localizer.formatText("customPropertyDialog.visibility.label")))
  }
  internal val propertySheetLabel = Label().also {
    it.styleClass.add("title")
  }
  internal val propertySheet = PropertySheetBuilder(localizer).createPropertySheet(fields)
  private val errorLabel = Label().also {
    it.styleClass.addAll("hint", "hint-validation")
  }
  private val errorPane = HBox().also {
    it.styleClass.addAll("hint-validation-pane", "noerror")
    it.children.add(errorLabel)
  }
}

interface Item<T> {
  var title: String
  val cloneOf: T?
  fun clone(): T
}

data class BtnController<T>(
  val isDisabled: SimpleBooleanProperty = SimpleBooleanProperty(false),
  var onAction: () -> T? = { null }
)

internal class ItemListDialogModel<T: Item<T>>(
  private val listItems: ObservableList<T>,
  private val newItemFactory: ()->T,
  private val selection: ()->Collection<T>
) {
  val btnAddController = BtnController(onAction = this::onAddColumn)
  val btnDeleteController = BtnController(onAction = this::onDeleteColumn)
  val btnApplyController = BtnController(onAction = {})

  fun onAddColumn(): T {
    val item = newItemFactory()
    val title = RootLocalizer.create("addCustomColumn").also {
      it.update("")
    }
    var count = 1
    while (listItems.any { it.title == title.value.trim() }) {
      val prevValue = title.value
      title.update(count.toString())
      if (prevValue == title.value) {
        break
      }
      count++
    }
    item.title = title.value.trim()
    listItems.add(item)
    return item
  }

  fun onDeleteColumn() {
    listItems.removeAll(selection())
  }

}

internal class ItemListDialogPane<T: Item<T>>(
  val listItems: ObservableList<T>,
  val selectedItem: ObservableProperty<T?>,
  val listItemConverter: (T) -> ShowHideListItem,
  private val dialogModel: ItemListDialogModel<T>,
  val editor: ItemEditorPane<T?>,
  private val localizer: Localizer) {

  val listView: ListView<T> = ListView()
  private val errorLabel = Label().also {
    it.styleClass.addAll("hint", "hint-validation")
  }
  private val errorPane = HBox().also {
    it.styleClass.addAll("hint-validation-pane", "noerror")
    it.children.add(errorLabel)
  }


  init {
    selectedItem.addWatcher { evt ->
      if (evt.trigger != listView && evt.newValue != null) {
        listItems.replaceAll { if (it.title == evt.newValue?.cloneOf?.title) { evt.newValue } else { it } }
      }
    }
    listView.items = listItems
    listView.cellFactory = Callback { ShowHideListCell(listItemConverter)}
//
    listView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
      if (newValue != null) {
        selectedItem.set(newValue.clone(), trigger = listView)
      }
    }
    listView.selectionModel.select(0)

  }

  fun build(dlg: DialogController) {
    dlg.addStyleClass("dlg-list-view-editor")
    dlg.addStyleSheet("/biz/ganttproject/ganttview/ListViewEditorDialog.css")
    dlg.setHeader(
      VBoxBuilder("header").apply {
        addTitle(localizer.create("title")).also { hbox ->
          hbox.alignment = Pos.CENTER_LEFT
          hbox.isFillHeight = true
        }
      }.vbox
    )

    dlg.setContent(HBox().also {
      it.children.addAll(listView, editor.node)
      HBox.setHgrow(editor.node, Priority.ALWAYS)
    })

    dlg.setupButton(ButtonType(localizer.formatText("add"))) { btn ->
      btn.text = localizer.formatText("add")
      ButtonBar.setButtonData(btn, ButtonBar.ButtonData.HELP)
      btn.disableProperty().bind(dialogModel.btnAddController.isDisabled)
      btn.styleClass.addAll("btn-attention", "secondary")
      btn.addEventFilter(ActionEvent.ACTION) {
        it.consume()
        dialogModel.btnAddController.onAction().also { item ->
          listView.scrollTo(item)
          listView.selectionModel.select(item)
          editor.focus()
        }
      }
    }

    dlg.setupButton(ButtonType(localizer.formatText("delete"))) { btn ->
      btn.text = localizer.formatText("delete")
      ButtonBar.setButtonData(btn, ButtonBar.ButtonData.HELP_2)
      btn.disableProperty().bind(dialogModel.btnDeleteController.isDisabled)
      btn.addEventFilter(ActionEvent.ACTION) {
        it.consume()
        dialogModel.btnDeleteController.onAction()
      }
      btn.styleClass.addAll("btn-regular", "secondary")
    }

    dlg.setupButton(ButtonType.APPLY) { btn ->
      btn.text = localizer.formatText("apply")
      btn.styleClass.add("btn-attention")
      btn.disableProperty().bind(dialogModel.btnApplyController.isDisabled)
      btn.setOnAction {
        dialogModel.btnApplyController.onAction()
        dlg.hide()
      }
    }

    dlg.setEscCloseEnabled(true)
  }

  fun onError(it: String?) {
    if (it == null) {
      errorPane.isVisible = false
      if (!errorPane.styleClass.contains("noerror")) {
        errorPane.styleClass.add("noerror")
      }
      errorLabel.text = ""
      dialogModel.btnApplyController.isDisabled.value = false
    }
    else {
      errorLabel.text = it
      errorPane.isVisible = true
      errorPane.styleClass.remove("noerror")
      dialogModel.btnApplyController.isDisabled.value = true
    }
  }
}