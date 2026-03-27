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

import biz.ganttproject.core.option.*
import biz.ganttproject.lib.fx.buildFontAwesomeButton
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.collections.FXCollections
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.stage.FileChooser
import javafx.stage.Window
import javafx.util.Callback
import javafx.util.StringConverter
import org.controlsfx.control.textfield.CustomTextField
import java.io.File
import java.util.*

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

private val ourTimer = Timer()

abstract class FileOptionEditorBase<T>(val option: ObservableProperty<T>, protected val displayOptions: FileDisplayOptions = FileDisplayOptions()) {
  protected val textField = CustomTextField()
  protected var myTimerTask: TimerTask? = null

  val node: Node = textField

  init {
    textField.right = buildFontAwesomeButton(
      iconName = FontAwesomeIcon.SEARCH.name,
      label = displayOptions.browseButtonText,
      onClick = { onBrowse() },
      styleClass = "btn"
    )
    textField.text = getTextFieldValue(option)
    textField.id = option.id
    textField.textProperty().addListener {
      onTextChange()
    }
    displayOptions.editorStyles.let(textField.styleClass::addAll)
    option.addWatcher {
      if (it.trigger != textField) {
        textField.text = getTextFieldValue(option)
      }
    }
  }

  private fun onBrowse() {
    val fileChooser = FileChooser()
    var initialFile: File? = getInitialFileForChooser()
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
    fileChooser.title = displayOptions.chooserTitle.ifBlank { "Choose a file" }
    displayOptions.let {
      it.extensionFilters.forEach {filter ->
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter(filter.description, filter.extensions))
      }
    }

    showFileChooser(fileChooser, topWindow())
  }

  protected abstract fun getInitialFileForChooser(): File?
  protected abstract fun getTextFieldValue(option: ObservableProperty<T>): String?
  protected abstract fun processTextFieldValue()
  protected abstract fun setOptionValue(value: T)
  protected abstract fun showFileChooser(fileChooser: FileChooser, owner: Window?)

  private fun onTextChange() {
    if (myTimerTask == null) {
      myTimerTask = object : TimerTask() {
        override fun run() {
          processTextFieldValue()
          myTimerTask = null
        }
      }
      ourTimer.schedule(myTimerTask, 1000)
    }
  }
}

class SingleFileOptionEditor(private val fileOption: ObservableFile, displayOptions: FileDisplayOptions = FileDisplayOptions())
  : FileOptionEditorBase<File?>(fileOption, displayOptions) {
  override fun getInitialFileForChooser(): File? =
    File(textField.text)


  override fun getTextFieldValue(option: ObservableProperty<File?>): String? =
    option.value?.absolutePath ?: ""

  override fun processTextFieldValue() {
    val file = File(textField.text)
    setOptionValue(file)
  }

  override fun setOptionValue(value: File?) {
    fileOption.set(value, textField)
  }

  override fun showFileChooser(fileChooser: FileChooser, ownerWindow: Window?) {
    val resultFile =
      if (displayOptions.isSaveNotOpen) fileChooser.showSaveDialog(ownerWindow)
      else fileChooser.showOpenDialog(ownerWindow)
    resultFile?.let {
      setOptionValue(resultFile)
      textField.text = getTextFieldValue(option)
    }
  }
}

class MultipleFilesOptionEditor(private val fileOption: ObservableFiles, displayOptions: FileDisplayOptions = FileDisplayOptions())
  : FileOptionEditorBase<List<File>>(fileOption, displayOptions) {
  override fun getInitialFileForChooser(): File? =
    textField.text.split(File.pathSeparator).filter { it.isNotBlank() }.firstOrNull()?.let { File(it) }


  override fun getTextFieldValue(option: ObservableProperty<List<File>>): String? =
    option.value.joinToString(File.pathSeparator) { it.absolutePath }

  override fun processTextFieldValue() {
    val paths = textField.text.split(File.pathSeparator).filter { it.isNotBlank() }
    val files = paths.map { File(it) }
    option.set(files, textField)
  }

  override fun setOptionValue(value: List<File>) {
    option.set(value, textField)
  }

  override fun showFileChooser(fileChooser: FileChooser, owner: Window?) {
    setOptionValue(fileChooser.showOpenMultipleDialog(owner))
    textField.text = getTextFieldValue(option)
  }

}
