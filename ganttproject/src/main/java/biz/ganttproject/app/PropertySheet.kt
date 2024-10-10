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
import biz.ganttproject.lib.fx.buildFontAwesomeButton
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableMap
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.effect.InnerShadow
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import javafx.util.StringConverter
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.util.BrowserControl
import org.controlsfx.control.textfield.CustomTextField
import java.awt.event.ActionEvent
import java.io.File

internal data class OptionItem(val option: ObservableProperty<*>, val editor: Node, val label: String?)
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

internal typealias OptionItemBuilder = () -> OptionItem
class PropertyPaneBuilder(private val localizer: Localizer) {
  internal val validationErrors = FXCollections.observableMap(mutableMapOf<ObservableProperty<*>, String>())
  internal val isEscCloseEnabled = SimpleBooleanProperty(true)
  internal val itemBuilders = mutableListOf<OptionItemBuilder>()

  fun text(property: ObservableString, optionValues: (TextDisplayOptions.()->Unit)? = null) {
    itemBuilders.add {
      val options = optionValues?.let { TextDisplayOptions().apply(it) }
      createOptionItem(property, createStringOptionEditor(property, options))
    }
  }

  fun file(property: ObservableFile, optionValues: (FileDisplayOptions.()->Unit)? = null) {
    itemBuilders.add {
      val options = optionValues?.let { FileDisplayOptions().apply(it) }
      createOptionItem(property, createFileOptionEditor(property, options))
    }
  }

  private fun createOptionItem(property: ObservableProperty<*>, editor: Node): OptionItem {
    property.isWritable.addWatcher { evt -> editor.isDisable = !evt.newValue }
    return OptionItem(property, editor, getOptionLabel(property))
  }

  fun <T> add(property: ObservableProperty<T>, displayOptions: PropertyDisplayOptions<T>? = null) {
    itemBuilders.add {
      createOptionEditorAndLabel(property)
    }
  }

  private fun createOptionEditorAndLabel(option: ObservableProperty<*>): OptionItem {
    val editor = when (option) {
      is ObservableBoolean -> createBooleanOptionEditor(option)
      is ObservableString -> createStringOptionEditor(option)
      is ObservableEnum -> createEnumerationOptionEditor(option)
      is ObservableFile -> createFileOptionEditor(option)
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

  private fun createStringOptionEditor(property: ObservableString, displayOptions: TextDisplayOptions? = null): Node {
    val textField = when {
      property.isScreened -> PasswordField()
      displayOptions?.isMultiline == true -> TextArea().also {
        it.prefColumnCount = displayOptions.columnCount
        it.prefWidth = displayOptions.columnCount * 10.0
      }
      else -> TextField().also {
        AutoCompletionTextFieldBinding(textField = it, suggestionProvider = { req ->
          property.completions(req.userText, it.caretPosition)
        }, converter = { it.text }).also {
          isEscCloseEnabled.bind(it.autoCompletionPopup.showingProperty().not())
        }
      }
    }

    val validatedText = textField.textProperty().validated(property.validator)
    property.isWritable.addWatcher {
      if (it.newValue) {
        validatedText.validate(textField.text, null)
      }
    }
    validatedText.addWatcher { evt ->
      property.set(evt.newValue, textField)
    }

    validatedText.validationMessage.addWatcher {
      if (it.newValue == null) {
        textField.markValid()
        validationErrors.remove(property)
      } else {
        textField.markInvalid()
        validationErrors[property] = it.newValue
      }
    }
    property.addWatcher {
      if (it.trigger != textField) {
        textField.text = property.value
      }
    }

    return textField
  }

  private fun createFileOptionEditor(option: ObservableFile, displayOptions: FileDisplayOptions? = null): Node {
    val textField = CustomTextField()
    val onBrowse = {
      val fileChooser = FileChooser();
      var initialFile: File?  = File(textField.text)
      while (initialFile?.exists() == false) {
        initialFile = initialFile.parentFile
      }
      initialFile?.let {
        if (it.isDirectory) {
          fileChooser.initialDirectory = it
        } else {
          fileChooser.initialDirectory = it.parentFile
        }
      }
      fileChooser.title = "Choose a file"
      displayOptions?.let {
        it.extensionFilters.forEach {filter ->
          fileChooser.extensionFilters.add(FileChooser.ExtensionFilter(filter.description, filter.extensions))
        }
      }
      val resultFile = fileChooser.showOpenDialog(null)
      option.value = resultFile
      resultFile?.let {
        textField.text = it.absolutePath
      }
    }
    textField.right = buildFontAwesomeButton(
      iconName = FontAwesomeIcon.SEARCH.name,
      label = "Browse...",
      onClick = { onBrowse() },
      styleClass = "btn"
    )
    return textField
//    return HBox().apply {
//      HBox.setHgrow(textField, Priority.ALWAYS)
//      children.add(textField)
//      children.add(Region().also {
//        it.padding = Insets(0.0, 5.0, 0.0, 0.0)
//      })
//      children.add(btn)
//    }

  }

  private fun createNoEditor(option: GPOption<*>) = Label(option.value?.toString())


  private fun getOptionLabel(option: ObservableProperty<*>) = localizer.formatTextOrNull("${option.id}.label")
}

class PropertySheetBuilder(private val localizer: Localizer) {

  fun pane(code: PropertyPaneBuilder.()->Unit): PropertySheet {
    val gridPane = PropertyPane().also {
      it.styleClass.add("property-pane")
      it.stylesheets.add("/biz/ganttproject/app/PropertySheet.css")
    }
    val paneBuilder = PropertyPaneBuilder(localizer).apply(code)
    paneBuilder.itemBuilders.map { it.invoke() }.forEachIndexed { idx, item ->
      if (item.label != null) {
        val label = createLabel(item)
        gridPane.add(label, 0, idx)
        GridPane.setHgrow(label, Priority.SOMETIMES)
        GridPane.setHalignment(label, HPos.RIGHT)
        item.editor.also {editor ->
          if (editor is Region) {
            editor.minWidth = MIN_COLUMN_WIDTH
            editor.maxWidth = Double.MAX_VALUE
          }
          if (editor is TextArea) {
            GridPane.setValignment(label, VPos.TOP)
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
            GridPane.setHgrow(hbox, Priority.SOMETIMES)
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
    return PropertySheet(gridPane, paneBuilder.validationErrors, paneBuilder.isEscCloseEnabled)
  }


  fun createPropertySheet(options: List<ObservableProperty<*>>): PropertySheet {
    return pane {
      options.forEach {
        add(it)
      }
    }
  }

  private fun createLabel(item: OptionItem): Label {
    return Label(item.label)
  }

  private fun getOptionHelpUrl(option: ObservableProperty<*>) = localizer.formatTextOrNull("${option.id}.helpUrl")
}


internal class PropertyPane : GridPane() {
  init {
    vgap = 10.0
    hgap = 10.0
    //padding = Insets(5.0, 15.0, 5.0, 15.0)
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

sealed class PropertyDisplayOptions<P>()
data class TextDisplayOptions(
  var isMultiline: Boolean = false,
  var isScreened: Boolean = false,
  var columnCount: Int = 40
): PropertyDisplayOptions<String?>()
data class FileExtensionFilter(val description: String, val extensions: List<String>)
data class FileDisplayOptions(val extensionFilters: MutableList<FileExtensionFilter> = mutableListOf<FileExtensionFilter>()): PropertyDisplayOptions<File>()
