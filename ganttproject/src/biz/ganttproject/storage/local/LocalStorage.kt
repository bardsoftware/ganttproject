/*
Copyright 2017 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

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
package biz.ganttproject.storage.local

import biz.ganttproject.lib.fx.buildFontAwesomeButton
import biz.ganttproject.storage.StorageDialogBuilder
import biz.ganttproject.storage.StorageMode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleObjectProperty
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.util.Callback
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.validation.ValidationResult
import org.controlsfx.validation.ValidationSupport
import org.controlsfx.validation.Validator
import org.controlsfx.validation.decoration.StyleClassValidationDecoration
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
class LocalStorage(
    private val myMode: StorageMode,
    private val currentDocument: Document,
    private val myDocumentReceiver: Consumer<Document>) : StorageDialogBuilder.Ui {
  private val i18n = GanttLanguage.getInstance()

  override fun getName(): String {
    return "This Computer"
  }

  override fun getCategory(): String {
    return "desktop"
  }

  private fun i18nKey(pattern: String): String {
    return String.format(pattern, myMode.name.toLowerCase())
  }

  fun absolutePrefix(path: Path, end: Int = path.nameCount): Path {
    return path.root.resolve(path.subpath(0, end))
  }

  override fun createUi(): Pane {
    val filePath = Paths.get(currentDocument.filePath)
    val state = State(
        CustomTextField(),
        SimpleObjectProperty(absolutePrefix(filePath, filePath.nameCount - 1).toFile()),
        SimpleObjectProperty(absolutePrefix(filePath).toFile()))

    state.validator = Validator<String> { control, value ->
      if (value == null) {
        return@Validator ValidationResult()
      }
      val file = File(state.currentDir.get(), value)
      try {
        myMode.tryFile(file)
        return@Validator ValidationResult()
      } catch (e: StorageMode.FileException) {
        println(e.message)
        return@Validator ValidationResult.fromError(control, i18n.formatText(e.message, e.args))
      }
    }
    val validationSupport = ValidationSupport()
    validationSupport.registerValidator(state.filename, state.validator)
    validationSupport.validationDecorator = StyleClassValidationDecoration("error", "warning")

    val rootPane = VBox()
    rootPane.styleClass.addAll("pane-service-contents", "local-storage")
    rootPane.prefWidth = 400.0

    val titleBox = HBox()
    titleBox.styleClass.add("title")
    val title = Label(i18n.getText(i18nKey("storageService.local.%s.title")))
    titleBox.children.add(title)

    val docList = VBox()
    docList.styleClass.add("doclist")

    val currentDocDir = createDirPane(state, state.currentDir::set)
    state.filename.text = currentDocument.fileName
    state.filename.styleClass.add("filename")
    docList.children.addAll(currentDocDir, state.filename)

    fun onBrowse() {
      val fileChooser = FileChooser()
      fileChooser.initialDirectory = state.currentDir.get()
      fileChooser.title = i18nKey("storageService.local.%s.fileChooser.title")
      fileChooser.extensionFilters.addAll(
          FileChooser.ExtensionFilter("GanttProject Files", "*.gan"))
      val chosenFile = fileChooser.showOpenDialog(null)
      if (chosenFile != null) {
        state.currentFile.set(chosenFile)
        state.filename.text = chosenFile.name
      }
    }

    val btnBrowse = buildFontAwesomeButton(FontAwesomeIcon.SEARCH.name, "Browse...", {onBrowse()}, "doclist-browse")
    state.filename.right = btnBrowse

    val btnSave = Button(i18n.getText(i18nKey("storageService.local.%s.actionLabel")))
    btnSave.addEventHandler(ActionEvent.ACTION, {myDocumentReceiver.accept(FileDocument(state.currentFile.get()))})
    btnSave.styleClass.add("doclist-save")
    val btnSaveBox = HBox()
    btnSaveBox.alignment = Pos.CENTER
    btnSaveBox.maxWidth = Double.MAX_VALUE
    btnSaveBox.children.addAll(btnSave)
    validationSupport.invalidProperty().addListener({ _, _, newValue -> btnSave.disableProperty().set(newValue) })

    rootPane.stylesheets.add("biz/ganttproject/storage/StorageDialog.css")
    rootPane.stylesheets.add("biz/ganttproject/storage/local/LocalStorage.css")
    rootPane.children.addAll(titleBox, docList, btnSaveBox)
    return rootPane
  }

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty<Pane>()
  }

  fun createDirPane(state: State, onClick: ((File) -> Unit)): Pane {
    val path = state.currentFile.get().toPath()
    val dropDown = ComboBox<Path>()

    dropDown.cellFactory = Callback<ListView<Path>, ListCell<Path>> { _ ->
      object : ListCell<Path>() {
        override fun updateItem(item: Path?, empty: Boolean) {
          super.updateItem(item, empty)
          if (item == null || empty) {
            graphic = null
          } else {
            val label = Label(if(item.nameCount >= 1) item.getName(item.nameCount - 1).toString() else item.toString())
            label.style = "-fx-padding: 0.5ex 0 0.5ex ${item.nameCount}em"
            graphic = label
          }
        }
      }
    }
    dropDown.buttonCell = object : ListCell<Path>() {
      override fun updateItem(item: Path?, empty: Boolean) {
        super.updateItem(item, empty)
        if (item == null || empty) {
          text = ""
        } else {
          text = if (item.nameCount >= 1) item.getName(item.nameCount - 1).toString() else item.toString()
        }
      }
    }
    dropDown.valueProperty().addListener { _, _, newValue ->
      if (newValue != null) {
        onClick(newValue.toFile())
      }
    }
    dropDown.styleClass.add("path-dropdown")
    dropDown.maxWidth = Double.MAX_VALUE

    // Re-populates dropdown given path to the selected file
    fun resetDropDown(filePath: Path) {
      dropDown.items.clear()
      dropDown.items.add(filePath.root)
      for (i in 0..(filePath.nameCount - 2)) {
        dropDown.items.add(absolutePrefix(filePath, i + 1))
      }
      dropDown.value = absolutePrefix(filePath, filePath.nameCount - 1)
    }
    resetDropDown(path)

    state.currentFile.addListener { _, _, newValue -> resetDropDown(newValue.toPath()) }
    val dropDownBox = HBox()
    dropDownBox.children.addAll(dropDown)
    HBox.setHgrow(dropDown, Priority.ALWAYS)
    return dropDownBox
  }
}

class State(
  val filename: CustomTextField,
  var currentDir: SimpleObjectProperty<File>,
  var currentFile: SimpleObjectProperty<File>
) {
  lateinit var validator: Validator<String>
}
