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
import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.option.ObservableProperty
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.createToggleSwitch
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.MapChangeListener
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
import java.util.WeakHashMap

/**
 * This is an object that is rendered in a list view.
 */
data class ShowHideListItem(val text: ()->String, val isVisible: ()->Boolean, val toggleVisible: ()->Unit)

private val ourCells = WeakHashMap<ShowHideListCell<*>, Boolean>()
/**
 * A list cell is a UI component that renders items in a list view. It adds a show/hide icon to the item title.
 */
internal class ShowHideListCell<T>(private val converter: (T)-> ShowHideListItem) : ListCell<T>() {
  private val iconVisible = MaterialIconView(MaterialIcon.VISIBILITY)
  private val iconHidden = MaterialIconView(MaterialIcon.VISIBILITY_OFF)
  private val iconPane = StackPane().also {
    it.onMouseClicked = EventHandler { _ ->
      theItem.toggleVisible()
      updateTheItem(theItem)
    }
  }

  val theItem by lazy { converter(item) }
  init {
    styleClass.add("column-item-cell")
    alignment = Pos.CENTER_LEFT
    ourCells.put(this, true)
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
    text = item.text()
    if (text.isEmpty()) {
      text = " "
    }
    if (graphic == null) {
      graphic = iconPane
    }
    if (item.isVisible()) {
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

  fun refresh() {
    if (item != null) {
      updateTheItem(converter(item))
    }
  }
}

/**
 * UI for editing properties of the selected list item.
 */
internal open class ItemEditorPane<T: Item<T>>(
  // Fields that need to be shown in the UI.
  fields: List<ObservableProperty<*>>,
  protected val editItem: ObservableProperty<T?>,
  // The whole dialog model.
  private val dialogModel: ItemListDialogModel<T>,
  // i18n
  localizer: Localizer) {

  private var isEditIgnored = false

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
    it.children.add(Label(localizer.formatText("visibility.label")))
  }
  internal val propertySheetLabel = Label().also {
    it.styleClass.add("title")
  }
  internal val propertySheet = PropertySheetBuilder(localizer).createPropertySheet(fields)
  private val errorLabel = Label().also {
    it.styleClass.addAll("hint", "hint-validation")
    it.isWrapText = true
  }
  private val errorPane = HBox().also {
    it.styleClass.addAll("hint-validation-pane", "noerror")
    it.children.add(errorLabel)
  }

  init {
    fields.forEach { it.addWatcher { onEdit() } }
    visibilityToggle.selectedProperty().subscribe { oldValue, newValue ->
      onEdit()
    }
    editItem.addWatcher {evt ->
      if (evt.trigger != this) {
        isEditIgnored = true
        evt.newValue?.isEnabledProperty?.addListener { source, oldValue, newValue ->
          if (oldValue != newValue && source == editItem.value?.isEnabledProperty) {
            visibilityToggle.isSelected = newValue
          }
        }
        loadData(evt.newValue)
        isEditIgnored = false
      }
    }
    propertySheet.validationErrors.addListener(MapChangeListener {
      if (propertySheet.validationErrors.isEmpty()) {
        onError(null)
      } else {
        onError(propertySheet.validationErrors.values.joinToString(separator = "\n"))
      }
    })
  }

  /**
   * This is called when we need to save data from the UI controls into the model item.
   */
  protected open fun saveData(item: T) {}

  /**
   * This is called when we need to load data from the model item into the UI controls.
   */
  protected open fun loadData(item: T?) {}

  internal fun onEdit() {
    if (isEditIgnored) return
    editItem.value?.let {
      saveData(it)
      //editItem.set(it, trigger = this)
    }
    dialogModel.requireRefresh.set(true)
  }

  internal fun focus() {
    propertySheet.requestFocus()
    onEdit()
  }

  private fun onError(it: String?) {
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

interface Item<T> {
  var title: String
  val isEnabledProperty: BooleanProperty
}

data class BtnController<T>(
  val isDisabled: SimpleBooleanProperty = SimpleBooleanProperty(false),
  var onAction: () -> T? = { null }
)

/**
 * The dialog model object connects the list view with the actions to add/delete items and the validation logic.
 */
internal class ItemListDialogModel<T: Item<T>>(
  private val listItems: ObservableList<T>,
  private val newItemFactory: ()->T,
  private val i18n: Localizer,
) {
  val btnAddController = BtnController(onAction = this::onAddColumn)
  val btnDeleteController = BtnController(onAction = this::onDeleteColumn)
  val btnApplyController = BtnController(onAction = {})
  internal var selection: ()-> Collection<T> = { emptyList() }
  val requireRefresh = SimpleBooleanProperty(false)

  fun onAddColumn(): T {
    val item = newItemFactory()
    val title = i18n.create("addItem").also {
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

/**
 * The whole user interface.
 */
internal class ItemListDialogPane<T: Item<T>>(
  // The list of items being shown.
  val listItems: ObservableList<T>,
  // The item that is currently selected and being edited.
  val selectedItem: ObservableProperty<T?>,
  // Converter from the application model objects to the list view objects.
  val listItemConverter: (T) -> ShowHideListItem,
  // The dialog model.
  private val dialogModel: ItemListDialogModel<T>,
  // The editor pane UI.
  val editor: ItemEditorPane<T>,
  // i18n.
  private val localizer: Localizer) {

  internal val listView: ListView<T> = ListView()

  init {
    listView.apply {
      this@ItemListDialogPane.dialogModel.selection = { selectionModel.selectedItems }
      items = listItems
      cellFactory = Callback { ShowHideListCell(listItemConverter)}
      selectionModel.selectedItemProperty().addListener { _, _, newValue ->
        if (newValue != null) {
          selectedItem.set(newValue, trigger = this)
        }
      }
      selectionModel.select(0)
    }
    dialogModel.requireRefresh.subscribe { oldValue, newValue ->
      if (newValue == true && oldValue == false) {
//        listView.refresh()
        ourCells.keys.forEach {
          it.refresh()
        }
        dialogModel.requireRefresh.set(false)
      }
    }
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
}