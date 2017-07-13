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
import biz.ganttproject.storage.*
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Control
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
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
import java.util.stream.Collectors
import java.util.stream.Stream

class FileAsFolderItem(val file: File) : FolderItem, Comparable<FileAsFolderItem> {
  override fun compareTo(other: FileAsFolderItem): Int {
    val result = this.isDirectory.compareTo(other.isDirectory)
    return  if (result != 0) { -1 * result } else { this.name.compareTo(other.name) }
  }

  override val isLocked: Boolean
    get() = false
  override val isLockable: Boolean
    get() = false
  override val name: String
    get() = file.name
  override val isDirectory: Boolean
    get() = file.isDirectory
}
/**
 * @author dbarashev@bardsoftware.com
 */
class LocalStorage(
    private val myDialogUi: StorageDialogBuilder.DialogUi,
    private val myMode: StorageMode,
    private val currentDocument: Document,
    private val myDocumentReceiver: Consumer<Document>) : StorageDialogBuilder.Ui {
  private val i18n = GanttLanguage.getInstance()
  private val myUtil = StorageUtil(myMode)

  override fun getName(): String {
    return "This Computer"
  }

  override fun getCategory(): String {
    return "desktop"
  }

  override fun createUi(): Pane {
    val filePath = Paths.get(currentDocument.filePath) ?: Paths.get("/")
    val filenameControl = CustomTextField()
    val state = State(
        SimpleObjectProperty(myUtil.absolutePrefix(filePath, filePath.nameCount - 1).toFile()),
        SimpleObjectProperty(myUtil.absolutePrefix(filePath).toFile()))
    val validationHelper = ValidationHelper(filenameControl, state, myMode)

    val rootPane = VBox()
    rootPane.styleClass.addAll("pane-service-contents", "local-storage")
    rootPane.prefWidth = 400.0

    val titleBox = HBox()
    titleBox.styleClass.add("title")
    val title = Label(i18n.getText(myUtil.i18nKey("storageService.local.%s.title")))
    titleBox.children.add(title)

    filenameControl.text = when (myMode) {
      is StorageMode.Open -> ""
      is StorageMode.Save -> currentDocument.fileName
    }
    filenameControl.styleClass.add("filename")

    val listView = FolderView<FileAsFolderItem>(
        myDialogUi,
        Consumer { item: FileAsFolderItem -> this.deleteResource() },
        Consumer { item: FileAsFolderItem ->  },
        SimpleBooleanProperty())
    val onSelectCrumb = Consumer { path: Path ->
      val dir = path.toFile()
      val result = FXCollections.observableArrayList<FileAsFolderItem>()
      dir.listFiles().map { f -> FileAsFolderItem(f) }.sorted().forEach { result.add(it) }
      listView.setResources(result)
      state.currentDir.set(dir)
    }

    val breadcrumbView = BreadcrumbView(
        if (filePath.toFile().isDirectory) filePath else filePath.parent, onSelectCrumb)
    state.currentDir.addListener({
      _, _, newValue -> breadcrumbView.path = newValue.toPath().toAbsolutePath()
    })

    fun selectItem(withEnter: Boolean, withControl: Boolean) {
      listView.selectedResource.ifPresent { item ->
        if (item.isDirectory && withEnter) {
          breadcrumbView.append(item.name)
          state.currentDir.set(item.file)
          state.currentFile.set(null)
          filenameControl.text = ""
        } else {
          state.currentDir.set(item.file.parentFile)
          state.currentFile.set(item.file)
          filenameControl.text = item.name
          if (withControl) {
            myDocumentReceiver.accept(FileDocument(state.currentFile.get()))
          }
        }
      }
    }
    fun onFilenameEnter() {
      var path = Paths.get(filenameControl.text)
      if (!path.isAbsolute) {
        path = breadcrumbView.path.resolve(path)
      }
      if (path.toFile().exists() && path.toFile().isDirectory) {
        breadcrumbView.path = path.normalize()
      }
    }
    connect(filenameControl, listView, breadcrumbView, ::selectItem, ::onFilenameEnter)
    fun onBrowse() {
      val fileChooser = FileChooser()
      var initialDir: File? = state.resolveFile(filenameControl.text)
      while (initialDir != null && (!initialDir.exists() || !initialDir.isDirectory)) {
        initialDir = initialDir.parentFile
      }
      if (initialDir != null) {
        fileChooser.initialDirectory = initialDir
      }
      fileChooser.title = myUtil.i18nKey("storageService.local.%s.fileChooser.title")
      fileChooser.extensionFilters.addAll(
          FileChooser.ExtensionFilter("GanttProject Files", "*.gan"))
      val chosenFile = fileChooser.showOpenDialog(null)
      if (chosenFile != null) {
        state.currentFile.set(chosenFile)
        state.currentDir.set(chosenFile.parentFile)
        filenameControl.text = chosenFile.name
      }
    }

    val btnBrowse = buildFontAwesomeButton(FontAwesomeIcon.SEARCH.name, "Browse...", {onBrowse()}, "doclist-browse")
    filenameControl.right = btnBrowse

    val errorLabel = Label("", FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE))
    setupErrorLabel(errorLabel, validationHelper)

    val btnSave = Button(i18n.getText(myUtil.i18nKey("storageService.local.%s.actionLabel")))
    val btnSaveBox = setupSaveButton(btnSave, state, myDocumentReceiver, validationHelper)
    validationHelper.validationSupport.validationResultProperty().addListener({ _, _, validationResult ->
      if (validationResult.errors.size + validationResult.warnings.size == 0) {
        state.currentFile.set(state.resolveFile(filenameControl.text))
      }
    })
    validationHelper.validationSupport.invalidProperty().addListener({ _, _, newValue ->
      btnSave.disableProperty().set(newValue)
    })

    rootPane.stylesheets.add("biz/ganttproject/storage/StorageDialog.css")
    rootPane.stylesheets.add("biz/ganttproject/storage/local/LocalStorage.css")

    errorLabel.styleClass.add("errorLabel")
    rootPane.children.addAll(titleBox, breadcrumbView.breadcrumbs, filenameControl, errorLabel, listView.listView, btnSaveBox)
    return rootPane
  }

  fun deleteResource() {

  }
  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty<Pane>()
  }
}

class ValidationHelper(val filename: Control, val state: State, val mode: StorageMode) {
  val validator: Validator<String> = Validator { control, value ->
    if (value == null) {
      return@Validator ValidationResult()
    }
    try {
      if (value == "") {
        return@Validator ValidationResult.fromWarning(control, "Type file name")
      }
      mode.tryFile(state.resolveFile(value))
      return@Validator ValidationResult()
    } catch (e: StorageMode.FileException) {
      println(e.message)
      return@Validator ValidationResult.fromError(control, GanttLanguage.getInstance().formatText(e.message, e.args))
    }
  }
  val validationSupport = ValidationSupport().apply {
    registerValidator(filename, validator)
    validationDecorator = StyleClassValidationDecoration("error", "warning")
  }
}

class State(
    var currentDir: SimpleObjectProperty<File>,
    var currentFile: SimpleObjectProperty<File>
) {

  fun resolveFile(typedString: String): File {
    val typedPath = Paths.get(typedString)
    return if (typedPath.isAbsolute) {
      typedPath.toFile()
    } else {
      File(this.currentDir.get(), typedString)
    }
  }
}

fun setupSaveButton(btnSave: Button, state: State, receiver: Consumer<Document>, validationHelper: ValidationHelper): Node {
  btnSave.addEventHandler(ActionEvent.ACTION, {receiver.accept(FileDocument(state.currentFile.get()))})
  btnSave.styleClass.add("doclist-save")
  val btnSaveBox = HBox()
  btnSaveBox.styleClass.add("doclist-save-box")
  btnSaveBox.maxWidth = Double.MAX_VALUE
  btnSaveBox.children.addAll(btnSave)
  validationHelper.validationSupport.validationResultProperty().addListener({ _, _, validationResult ->
    if (validationResult.errors.size + validationResult.warnings.size > 0) {
      btnSave.disableProperty().set(true)
    } else {
      btnSave.disableProperty().set(false)
    }
  })
  validationHelper.validationSupport.invalidProperty().addListener({ _, _, newValue ->
    btnSave.disableProperty().set(newValue)
  })
  return btnSaveBox
}

private fun formatError(validation: ValidationResult): String {
  return Stream.concat(validation.errors.stream(), validation.warnings.stream())
      .map { error -> error.text }
      .collect(Collectors.joining("\n"))
}

fun setupErrorLabel(errorLabel: Label, validationHelper: ValidationHelper) {
  errorLabel.styleClass.addAll("hint", "noerror")
  validationHelper.validationSupport.validationResultProperty().addListener({_, _, validationResult ->
    if (validationResult.errors.size + validationResult.warnings.size > 0) {
      errorLabel.text = formatError(validationResult)
      errorLabel.styleClass.remove("noerror")
      if (validationResult.errors.isNotEmpty()) {
        errorLabel.graphic = FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE)
        errorLabel.styleClass.remove("warning")
        errorLabel.styleClass.add("error")
      } else if (validationResult.warnings.isNotEmpty()) {
        errorLabel.graphic = null
        errorLabel.styleClass.remove("error")
        errorLabel.styleClass.add("warning")
      }
    } else {
      errorLabel.text = ""
      errorLabel.styleClass.removeAll("error", "warning")
      errorLabel.styleClass.add("noerror")
    }
  })
}
