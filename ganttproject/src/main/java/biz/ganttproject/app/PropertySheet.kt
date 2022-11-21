/*
Copyright 2022 BarD Software s.r.o

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

import biz.ganttproject.core.option.*
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.effect.InnerShadow
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.util.StringConverter

private data class OptionItem(val option: GPOption<*>, val editor: Node, val label: String?)
private val MIN_COLUMN_WIDTH = 100.0

class PropertySheet(val node: Node, val validationErrors: ObservableMap<String, String>) {
  fun requestFocus() {
    node.requestFocus()
  }

  var isDisable: Boolean
    get() = node.isDisable
    set(value) { node.isDisable = value }
}

class PropertySheetBuilder(private val localizer: Localizer) {
  private val validationErrors = FXCollections.observableMap(mutableMapOf<String, String>())

  fun createPropertySheet(options: List<GPOption<*>>): PropertySheet {
    val gridPane = PropertyPane().also {
      it.styleClass.add("property-pane")
    }
    options.filter { it.hasUi() }.map { createOptionEditorAndLabel(it) }.forEachIndexed { idx, item ->
      if (item.label != null) {
        val label = createLabel(item)
        gridPane.add(label, 0, idx)
        gridPane.add(item.editor, 1, idx)

        if (item.editor is Region) {
          item.editor.minWidth = MIN_COLUMN_WIDTH
          item.editor.maxWidth = Double.MAX_VALUE
        }
        label.labelFor = item.editor
        GridPane.setHgrow(item.editor, Priority.ALWAYS)

      }
      if (idx == 0) {
        gridPane.focusedProperty().addListener { _, oldValue, newValue ->
          if (!oldValue && newValue) {
            item.editor.requestFocus()
          }
        }
      }
    }
    return PropertySheet(gridPane, validationErrors)
  }

  private fun createLabel(item: OptionItem): Label {
    return Label(item.label)
  }

  private fun createOptionEditorAndLabel(option: GPOption<*>): OptionItem {
    val editor = when (option) {
      is BooleanOption -> createBooleanOptionEditor(option)
      is StringOption -> createStringOptionEditor(option)
      is EnumerationOption -> createEnumerationOptionEditor(option)
      else -> createNoEditor(option)
    }
    option.isWritableProperty.addListener { evt: ChangeValueEvent ->
      (evt.newValue as Boolean).let {
        editor.isDisable = !it
      }
    }

    return OptionItem(option, editor, getOptionLabel(option))
  }

  private fun createBooleanOptionEditor(option: BooleanOption): Node {
    return CheckBox().also {checkBox ->
      checkBox.onAction = EventHandler {
        option.setValue(checkBox.isSelected, checkBox)
      }
      option.addChangeValueListener {evt ->
        if (evt.triggerID != checkBox) {
          checkBox.isSelected = option.value
        }
      }
    }

  }

  private fun createEnumerationOptionEditor(option: EnumerationOption): Node {
    val key2i18n: List<Pair<String, String>> = option.availableValues.map { it to localizer.formatText("$it.label") }.toList()
    return ComboBox(FXCollections.observableArrayList(key2i18n)).also { comboBox ->
      comboBox.onAction = EventHandler{
        option.setValue(comboBox.value.first, comboBox)
      }
      option.addChangeValueListener { evt ->
        if (evt.triggerID != comboBox) {
          comboBox.selectionModel.select(key2i18n.find { it.first == option.value })
        }
      }
      comboBox.converter = object : StringConverter<Pair<String, String>>() {
        override fun toString(item: Pair<String, String>?) = item?.second
        override fun fromString(string: String?) = key2i18n.find { it.second == string }
      }
    }
  }

  private fun createNoEditor(option: GPOption<*>) = Label(option.value?.toString())


  private fun createStringOptionEditor(option: StringOption): Node =
    (if (option.isScreened) { PasswordField() } else { TextField() }).also { textField ->
      val validatedText = textField.textProperty().validated(option.validator ?: voidValidator)
      validatedText.addWatcher { evt ->
        println("vlalidated ${option.id}: $evt")
        option.setValue(evt.newValue, textField)
      }
      validatedText.validationMessage.addWatcher {
        if (it.newValue == null) {
          textField.markValid()
          validationErrors.remove(option.id)
        } else {
          textField.markInvalid()
          validationErrors[option.id] = it.newValue
        }
      }
      option.addChangeValueListener {
        if (it.triggerID != textField) {
          textField.text = option.value
        }
      }
    }

  private fun getOptionLabel(option: GPOption<*>) = localizer.formatTextOrNull("${option.id}.label")
}


internal class PropertyPane : GridPane() {
  init {
    vgap = 5.0
    hgap = 5.0
    padding = Insets(5.0, 15.0, 5.0, 15.0)
    styleClass.add("property-pane")
  }
}

private fun Node.markValid() {
  this.styleClass.remove("validation-error")
  this.effect = null
}

private fun Node.markInvalid() {
  if (!this.styleClass.contains("validation-error")) {
    this.styleClass.add("validation-error")
    this.effect = InnerShadow(10.0, Color.RED)
  }
}
