/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package biz.ganttproject.app

import biz.ganttproject.core.option.ObservableChoice
import biz.ganttproject.core.option.ObservableProperty
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.util.Callback
import javafx.util.StringConverter

/**
 * Creates a dropdown editor for the given choice option.
 */
fun <T> createChoiceOptionEditor(option: ObservableChoice<T>, displayOptions: DropdownDisplayOptions<T>? = null): Node {
  val key2i18n: List<Pair<T, String>> = option.allValues.map {
    it to option.converter.toString(it)
  }.toList()
  return createDropdownEditor(option, key2i18n, displayOptions)
}

fun <E> createDropdownEditor(option: ObservableProperty<E>, key2i18n: List<Pair<E, String>>, displayOptions: DropdownDisplayOptions<E>? = null): Node {
  return HBox(ComboBox(FXCollections.observableArrayList(key2i18n)).also { comboBox ->
    comboBox.onAction = EventHandler{
      option.set(comboBox.value.first, comboBox)
    }
    displayOptions?.cellFactory?.let { customCellFactory ->
      comboBox.cellFactory = Callback { p ->
        object: ListCell<Pair<E, String>>() {
          override fun updateItem(item: Pair<E, String>?, empty: Boolean) {
            super.updateItem(item, empty)
            if (item == null || empty) {
              setGraphic(null);
            } else {
              graphic = customCellFactory(this, item)
            }
          }
        }
      }
      comboBox.buttonCell = comboBox.cellFactory.call(null)
    }
    option.addWatcher { evt ->
      if (evt.trigger != comboBox) {
        comboBox.selectionModel.select(key2i18n.find { it.first == option.value })
      }
    }
    comboBox.converter = object : StringConverter<Pair<E, String>>() {
      override fun toString(item: Pair<E, String>?) = item?.second
      override fun fromString(string: String?) = key2i18n.find { it.second == string }
    }
    comboBox.value = key2i18n.find { it.first == option.value }
  })
}
