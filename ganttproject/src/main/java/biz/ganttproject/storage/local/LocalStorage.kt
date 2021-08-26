/*
Copyright 2017-2020 Dmitry Barashev, BarD Software s.r.o

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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.lib.fx.buildFontAwesomeButton
import biz.ganttproject.storage.*
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.scene.control.Button
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer

/**
 * Adapter representing file in the browser UI
 */
class FileAsFolderItem(val file: File) : FolderItem, Comparable<FileAsFolderItem> {
  override fun compareTo(other: FileAsFolderItem): Int {
    val result = this.isDirectory.compareTo(other.isDirectory)
    return if (result != 0) {
      -1 * result
    } else {
      this.name.compareTo(other.name)
    }
  }

  override val isLocked: Boolean = false
  override val isLockable: Boolean = false
  override val canChangeLock: Boolean = false
  override val name: String = file.name
  override val basePath = file.parentFile.absolutePath
  override val isDirectory: Boolean = file.isDirectory
  override val tags = listOf<String>()
}

/**
 * This class builds the user interface of the local file storage (aka "This Computer") in the projects pane.
 */
class LocalStorage(
    private val myDialogUi: StorageDialogBuilder.DialogUi,
    private val mode: StorageDialogBuilder.Mode,
    private val currentDocument: Document,
    private val myDocumentReceiver: (Document) -> Unit) : StorageUi {
  private val myMode = if (mode == StorageDialogBuilder.Mode.OPEN) StorageMode.Open() else StorageMode.Save()
  private lateinit var paneElements: BrowserPaneElements<FileAsFolderItem>
  private val state: LocalStorageState = LocalStorageState(currentDocument, myMode, getDefaultLocalFolder())
  private val validator = createLocalStorageValidator(
      { this@LocalStorage.paneElements.listView.listView.items.isEmpty() },
      state
  )


  override val name = i18n.formatText("listLabel")
  override val category = "desktop"

  private fun loadFiles(
    path: Path,
    success: Consumer<ObservableList<FileAsFolderItem>>,
    state: LocalStorageState,
    busyIndicator: Consumer<Boolean>
  ) {
    busyIndicator.accept(true)
    val dir = DocumentUri.toFile(path)
    val result = FXCollections.observableArrayList<FileAsFolderItem>()
    dir.listFiles()
        ?.filter { !it.name.startsWith(".") }
        ?.map { f -> FileAsFolderItem(f) }
        ?.sorted()
        ?.toCollection(result)
    success.accept(result)
    val currentFilename = paneElements.filenameInput.text
    state.currentDir.set(dir)
    state.setCurrentFile(currentFilename)
    busyIndicator.accept(false)
  }

  private fun onBrowse() {
    var initialDir: File? = this.state.resolveFile(this.paneElements.filenameInput.text)
    while (initialDir != null && (!initialDir.exists() || !initialDir.isDirectory)) {
      initialDir = initialDir.parentFile
    }
    val chosenFile = myMode.openFileChooser(initialDir)
    if (chosenFile != null) {
      when (myMode) {
        is StorageMode.Save -> {
          state.currentDir.set(chosenFile)
          paneElements.breadcrumbView?.path = createPath(chosenFile)
        }
        is StorageMode.Open -> {
          state.setCurrentFile(chosenFile)
          paneElements.breadcrumbView?.path = createPath(chosenFile.parentFile)
          this.paneElements.filenameInput.text = chosenFile.name
        }
      }
    }
  }

  override fun createUi(): Pane {
    val builder = BrowserPaneBuilder<FileAsFolderItem>(this.mode, myDialogUi::error) { path, success, busyIndicator ->
      loadFiles(path, success, state, busyIndicator)
    }

    val actionButtonHandler = object {
      var selectedProject: FileAsFolderItem? = null
      var button: Button? = null
      set(btn) {
        field = btn
        btn?.let {
          it.addEventHandler(ActionEvent.ACTION) {
            onAction()
          }
          it.disableProperty().bind(state.canWrite.not())
        }
      }

      fun onOpenDirectory(item: FolderItem) {
        if (item is FileAsFolderItem) {
          if (item.isDirectory) {
            val currentFilename = paneElements.filenameInput.text
            state.currentDir.set(item.file)
            state.setCurrentFile(currentFilename)
          }
        }
      }

      fun onSelectionChange(item: FolderItem) {
        if (item is FileAsFolderItem) {
          when {
            item.isDirectory -> {
            }
            else -> {
              selectedProject = item
              state.currentDir.set(item.file.parentFile)
              state.setCurrentFile(item.file)
            }
          }
        }
      }

      fun onAction() {
        val doc = selectedProject?.let { FileDocument(it.file) }
            ?: state.currentDir.get()?.let {
              val file = if (it.resolve(paneElements.filenameInput.text).exists()) {
                it.resolve(paneElements.filenameInput.text)
              } else {
                it.resolve(paneElements.filenameWithExtension)
              }
              FileDocument(file)
            } ?: return
        myDocumentReceiver.invoke(doc)
      }

      fun onNameTyped(filename: String, itemsMatched: List<FolderItem>, withEnter: Boolean, withControl: Boolean) {
        state.setCurrentFile(filename)
        if (withEnter && withControl && mode == StorageDialogBuilder.Mode.SAVE) {
          this.onAction()
        }
      }
    }

    val listViewHint = SimpleStringProperty(i18n.formatText("${myMode.name.toLowerCase()}.listViewHint"))

    val filePath = Paths.get(currentDocument.filePath) ?: Paths.get("/")
    this.paneElements = builder.apply {
      withI18N(i18n)
      withBreadcrumbs(
          if (filePath.toFile().isDirectory) createPath(filePath.toFile())
          else createPath(filePath.parent.toFile())
      )
      withActionButton { btn ->
        actionButtonHandler.button = btn
      }
      withListView(
          onSelectionChange = actionButtonHandler::onSelectionChange,
          onOpenDirectory = actionButtonHandler::onOpenDirectory,
          onLaunch = {
            myDocumentReceiver(FileDocument(it.file))
          },
          onNameTyped = actionButtonHandler::onNameTyped
      )
      withValidator(validator)
      withListViewHint(listViewHint)
      withConfirmation(RootLocalizer.create("document.overwrite"), state.confirmationRequired)
    }.build()
    paneElements.browserPane.stylesheets.addAll(
        "biz/ganttproject/storage/StorageDialog.css",
        "biz/ganttproject/storage/local/LocalStorage.css"
    )
    paneElements.breadcrumbView?.show()

    val btnBrowse = buildFontAwesomeButton(
        iconName = FontAwesomeIcon.SEARCH.name,
        label = i18n.formatText("btn"),
        onClick = { onBrowse() },
        styleClass = "local-storage-browse"
    )
    this.paneElements.filenameInput.right = btnBrowse
    if (this.mode == StorageDialogBuilder.Mode.SAVE) {
      this.paneElements.filenameInput.text = currentDocument.fileName
      this.state.setCurrentFile(currentDocument.fileName)
      this.state.currentFile.addListener { _, _, newValue ->
        Platform.runLater { paneElements.confirmationCheckBox?.isSelected = false }
      }
      this.paneElements.confirmationCheckBox?.selectedProperty()?.addListener { _, _, newValue ->
        state.confirmationReceived.value = newValue
      }
    }
    this.state.validation.addListener { _, _, newValue ->
      paneElements.setValidationResult(newValue)
    }
    return paneElements.browserPane
  }

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }

  override fun focus() {
    this.paneElements.filenameInput.requestFocus()
  }
}

private val i18n = RootLocalizer.createWithRootKey("storageService.local", BROWSE_PANE_LOCALIZER)
