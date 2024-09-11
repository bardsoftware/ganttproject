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

import biz.ganttproject.app.Localizer
import biz.ganttproject.app.PropertySheetBuilder
import biz.ganttproject.core.option.GPObservable
import biz.ganttproject.core.option.ObservableProperty
import biz.ganttproject.lib.fx.createToggleSwitch
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.BooleanProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane

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

open internal class ItemEditorPane(
  private val allOptions: List<ObservableProperty<*>>,
  private val escCloseEnabled: BooleanProperty,
  private val localizer: Localizer) {

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
  internal val propertySheet = PropertySheetBuilder(localizer).createPropertySheet(allOptions).also {
    escCloseEnabled.bind(it.isEscCloseEnabled)
  }
  private val errorLabel = Label().also {
    it.styleClass.addAll("hint", "hint-validation")
  }
  private val errorPane = HBox().also {
    it.styleClass.addAll("hint-validation-pane", "noerror")
    it.children.add(errorLabel)
  }

}
