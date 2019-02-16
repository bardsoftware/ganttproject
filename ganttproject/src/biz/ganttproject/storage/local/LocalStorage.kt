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

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.lib.fx.buildFontAwesomeButton
import biz.ganttproject.storage.*
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.layout.Pane
import javafx.stage.FileChooser
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.language.GanttLanguage
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier

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
  override val isDirectory: Boolean = file.isDirectory
}

fun absolutePrefix(path: Path, end: Int = path.getNameCount()): Path {
  return path.getRoot().resolve(path.subpath(0, end))
}

/**
 * @author dbarashev@bardsoftware.com
 */
class LocalStorage(
    private val myDialogUi: StorageDialogBuilder.DialogUi,
    private val mode: StorageDialogBuilder.Mode,
    private val currentDocument: Document,
    private val myDocumentReceiver: Consumer<Document>) : StorageDialogBuilder.Ui {
  private val myMode = if (mode == StorageDialogBuilder.Mode.OPEN) StorageMode.Open() else StorageMode.Save()
  private val i18n = GanttLanguage.getInstance()
  private val myUtil = StorageUtil(myMode)
  private lateinit var paneElements: BrowserPaneElements
  private lateinit var state: LocalStorageState


  override val name = "This Computer"
  override val category = "desktop"

  private fun loadFiles(path: Path, success: Consumer<ObservableList<FolderItem>>, state: LocalStorageState) {
    val dir = DocumentUri.toFile(path)
    val result = FXCollections.observableArrayList<FolderItem>()
    dir.listFiles().filter { !it.name.startsWith(".") }.map { f -> FileAsFolderItem(f) }.sorted().forEach { result.add(it) }
    success.accept(result)
    state.currentDir.set(dir)
  }

  private fun onBrowse() {
    val fileChooser = FileChooser()
    var initialDir: File? = this.state.resolveFile(this.paneElements.filenameInput.text)
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
      state.setCurrentFile(chosenFile)
      state.currentDir.set(chosenFile.parentFile)
      this.paneElements.filenameInput.text = chosenFile.name
    }
  }

  override fun createUi(): Pane {
    val filePath = Paths.get(currentDocument.filePath) ?: Paths.get("/")
    this.state = LocalStorageState(currentDocument, myMode)

    val builder = BrowserPaneBuilder(this.mode, myDialogUi) { path, success, loading ->
      loadFiles(path, success, state)
    }
    val actionButtonHandler = object {
      var selectedProject: FileAsFolderItem? = null
      var selectedDir: FileAsFolderItem? = null

      fun onOpenItem(item: FolderItem) {
        if (item is FileAsFolderItem) {
          when {
            item.isDirectory -> {
              selectedDir = item
              state.currentDir.set(item.file)
              state.setCurrentFile(null)
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
        selectedProject?.let {
          myDocumentReceiver.accept(FileDocument(it.file))
        }
      }
    }

    val listViewHint = SimpleStringProperty(i18n.getText(myUtil.i18nKey("storageService.local.%s.listViewHint")))
    this.paneElements = builder.apply {
      withI18N(DefaultLocalizer("storageService.local", BROWSE_PANE_LOCALIZER))
      withBreadcrumbs(if (filePath.toFile().isDirectory) createPath(filePath.toFile()) else createPath(filePath.parent.toFile()))
      withActionButton(EventHandler { actionButtonHandler.onAction() })
      withListView(
          onOpenItem = Consumer { actionButtonHandler.onOpenItem(it) },
          onLaunch = Consumer {
            if (it is FileAsFolderItem) {
              myDocumentReceiver.accept(FileDocument(it.file))
            }
          }
      )
      withValidator(createLocalStorageValidator(
          Supplier { this@LocalStorage.paneElements.listView.listView.items.isEmpty() },
          state
      ))
      withListViewHint(listViewHint)
    }.build()
    paneElements.browserPane.stylesheets.addAll(
        "biz/ganttproject/storage/StorageDialog.css",
        "biz/ganttproject/storage/local/LocalStorage.css"
    )

    val btnBrowse = buildFontAwesomeButton(FontAwesomeIcon.SEARCH.name, "Browse...", { onBrowse() }, "local-storage-browse")
    this.paneElements.filenameInput.right = btnBrowse

    state.validationSupport = paneElements.validationSupport

// TODO: restore overwrite confirmation?
//    if (myMode is StorageMode.Save) {
//      val confirmation = CheckBox("Overwrite")
//      confirmation.visibleProperty().set(false)
//      fun updateConfirmation() {
//        if (state.confirmationRequired.get()) {
//          confirmation.visibleProperty().set(true)
//          confirmation.text = "Overwrite file " + state.currentFile.get().name
//          confirmation.selectedProperty().set(false)
//        } else {
//          confirmation.visibleProperty().set(false)
//        }
//      }
//      state.confirmationRequired.addListener({ _, _, _ -> updateConfirmation() })
//      state.currentFile.addListener({ _, _, _ -> updateConfirmation() })
//
//      confirmation.selectedProperty().addListener({ _, _, newValue -> state.confirmationReceived.set(newValue) })
//      rootPane.add(confirmation)
//    }
    return paneElements.browserPane
  }

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }
}

fun setupSaveButton(
    btnSave: Button,
    state: LocalStorageState,
    receiver: Consumer<Document>) {
  btnSave.addEventHandler(ActionEvent.ACTION) { receiver.accept(FileDocument(state.currentFile.get())) }
  btnSave.styleClass.add("btn-attention")
  state.submitOk.addListener { _, _, newValue -> btnSave.disableProperty().set(!newValue) }
}


