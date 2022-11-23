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
import biz.ganttproject.lib.fx.AutoCompletionTextFieldBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.effect.InnerShadow
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.util.StringConverter
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.util.BrowserControl
import java.awt.event.ActionEvent

private data class OptionItem(val option: ObservableProperty<*>, val editor: Node, val label: String?)
private val MIN_COLUMN_WIDTH = 100.0

class PropertySheet(
  val node: Node,
  val validationErrors: ObservableMap<ObservableProperty<*>, String>,
  val isEscCloseEnabled: BooleanProperty
) {
  fun requestFocus() {
    node.requestFocus()
  }

  var isDisable: Boolean
    get() = node.isDisable
    set(value) { node.isDisable = value }
}

class PropertySheetBuilder(private val localizer: Localizer) {
  private val validationErrors = FXCollections.observableMap(mutableMapOf<ObservableProperty<*>, String>())
  private val isEscCloseEnabled = SimpleBooleanProperty(true)
  fun createPropertySheet(options: List<ObservableProperty<*>>): PropertySheet {
    val gridPane = PropertyPane().also {
      it.styleClass.add("property-pane")
      it.stylesheets.add("/biz/ganttproject/app/PropertySheet.css")
    }
    options.map { createOptionEditorAndLabel(it) }.forEachIndexed { idx, item ->
      if (item.label != null) {
        val label = createLabel(item)
        gridPane.add(label, 0, idx)

        item.editor.also {editor ->
          if (editor is Region) {
            editor.minWidth = MIN_COLUMN_WIDTH
            editor.maxWidth = Double.MAX_VALUE
          }
          label.labelFor = editor
          HBox(editor).also {hbox ->
            HBox.setHgrow(item.editor, Priority.ALWAYS)

            getOptionHelpUrl(item.option)?.let { url ->
              hbox.children.add(createButton(OpenUrlAction(url), onlyIcon = true)?.also {
                it.styleClass.add("btn-help-url")
              })
            }
            gridPane.add(hbox, 1, idx)
            GridPane.setHgrow(hbox, Priority.ALWAYS)
          }
        }

      }
      if (idx == 0) {
        gridPane.focusedProperty().addListener { _, oldValue, newValue ->
          if (!oldValue && newValue) {
            item.editor.requestFocus()
          }
        }
      }
    }
    return PropertySheet(gridPane, validationErrors, isEscCloseEnabled)
  }

  private fun createLabel(item: OptionItem): Label {
    return Label(item.label)
  }

  private fun createOptionEditorAndLabel(option: ObservableProperty<*>): OptionItem {
    val editor = when (option) {
      is ObservableBoolean -> createBooleanOptionEditor(option)
      is ObservableString -> createStringOptionEditor(option)
      is ObservableEnum -> createEnumerationOptionEditor(option)
      is ObservableObject<*> -> error("Can't create editor for ObservableObject=${option.id}")
    }
    option.isWritable.addWatcher { evt -> editor.isDisable = !evt.newValue }

    return OptionItem(option, editor, getOptionLabel(option))
  }

  private fun createBooleanOptionEditor(option: ObservableBoolean): Node {
    return CheckBox().also {checkBox ->
      checkBox.onAction = EventHandler {
        option.set(checkBox.isSelected, checkBox)
      }
      option.addWatcher { evt ->
        if (evt.trigger != checkBox) {
          checkBox.isSelected = option.value
        }
      }
    }

  }

  private fun <E: Enum<E>> createEnumerationOptionEditor(option: ObservableEnum<E>): Node {
    val key2i18n: List<Pair<E, String>> = option.allValues.map { it to localizer.formatText("$it.label") }.toList()
    return ComboBox(FXCollections.observableArrayList(key2i18n)).also { comboBox ->
      comboBox.onAction = EventHandler{
        option.set(comboBox.value.first, comboBox)
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
    }
  }

  private fun createNoEditor(option: GPOption<*>) = Label(option.value?.toString())


  private fun createStringOptionEditor(option: ObservableString): Node =
    (if (option.isScreened) { PasswordField() } else { TextField() }).also { textField ->
      AutoCompletionTextFieldBinding(textField = textField, suggestionProvider ={req ->
          option.completions(req.userText, textField.caretPosition)
      }, converter = {it.text}).also {
        isEscCloseEnabled.bind(it.autoCompletionPopup.showingProperty().not())
      }
      val validatedText = textField.textProperty().validated(option.validator)
      validatedText.addWatcher { evt ->
        option.set(evt.newValue, textField)
      }
      validatedText.validationMessage.addWatcher {
        if (it.newValue == null) {
          textField.markValid()
          validationErrors.remove(option)
        } else {
          textField.markInvalid()
          validationErrors[option] = it.newValue
        }
      }
      option.addWatcher {
        if (it.trigger != textField) {
          textField.text = option.value
        }
      }
    }

  private fun getOptionLabel(option: ObservableProperty<*>) = localizer.formatTextOrNull("${option.id}.label")
  private fun getOptionHelpUrl(option: ObservableProperty<*>) = localizer.formatTextOrNull("${option.id}.helpUrl")
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

private class OpenUrlAction(private val url: String): GPAction("help.openUrl") {
  override fun actionPerformed(e: ActionEvent?) {
    BrowserControl.displayURL(url)
  }
}