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

import biz.ganttproject.app.RootLocalizer
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
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier

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
  override val isDirectory: Boolean = file.isDirectory
  override val tags = listOf<String>()
}

fun absolutePrefix(path: Path, end: Int = path.getNameCount()): Path {
  return path.getRoot().resolve(path.subpath(0, end))
}

/**
 * This class build user interface of local file storage (aka "This Computer") in the projects pane.
 *
 * @author dbarashev@bardsoftware.com
 */
class LocalStorage(
    private val myDialogUi: StorageDialogBuilder.DialogUi,
    private val mode: StorageDialogBuilder.Mode,
    private val currentDocument: Document,
    private val myDocumentReceiver: (Document) -> Unit) : StorageDialogBuilder.Ui {
  private val myMode = if (mode == StorageDialogBuilder.Mode.OPEN) StorageMode.Open() else StorageMode.Save()
  private lateinit var paneElements: BrowserPaneElements<FileAsFolderItem>
  private lateinit var state: LocalStorageState


  override val name = i18n.formatText("listLabel")
  override val category = "desktop"

  private fun loadFiles(path: Path, success: Consumer<ObservableList<FileAsFolderItem>>, state: LocalStorageState) {
    val dir = DocumentUri.toFile(path)
    val result = FXCollections.observableArrayList<FileAsFolderItem>()
    dir.listFiles()
        ?.filter { !it.name.startsWith(".") }
        ?.map { f -> FileAsFolderItem(f) }
        ?.sorted()
        ?.forEach { result.add(it) }
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
    fileChooser.title = i18n.formatText("storageService.local.${myMode.name.toLowerCase()}.fileChooser.title")
    fileChooser.extensionFilters.addAll(
        FileChooser.ExtensionFilter(RootLocalizer.formatText("ganttprojectFiles"), "*.gan"))
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

    val builder = BrowserPaneBuilder<FileAsFolderItem>(this.mode, myDialogUi::error) { path, success, loading ->
      loadFiles(path, success, state)
    }
    val actionButtonHandler = object {
      var selectedProject: FileAsFolderItem? = null
      var selectedDir: FileAsFolderItem? = null
      var button: Button? = null

      fun onSelectionChange(item: FolderItem) {
        if (item is FileAsFolderItem) {
          when {
            item.isDirectory -> {
              selectedDir = item
              state.currentDir.set(item.file)
              state.setCurrentFile(null)
              button?.isDisable = true
            }
            else -> {
              selectedProject = item
              state.currentDir.set(item.file.parentFile)
              state.setCurrentFile(item.file)
              button?.isDisable = false
            }
          }
        }
      }

      fun onAction() {
        val doc = selectedProject?.let { FileDocument(it.file) }
            ?: state.currentDir.get()?.resolve(paneElements.filenameInput.text)?.let { FileDocument(it) }
            ?: return
        myDocumentReceiver.invoke(doc)
      }

      fun onNameTyped(filename: String, itemsMatched: List<FolderItem>, withEnter: Boolean, withControl: Boolean) {
        this.button?.isDisable =
            when (mode) {
              StorageDialogBuilder.Mode.OPEN -> itemsMatched.isEmpty()
              StorageDialogBuilder.Mode.SAVE -> filename.isBlank()
            }
        if (withEnter && withControl && mode == StorageDialogBuilder.Mode.SAVE) {
          this.onAction()
        }
      }

    }

    val listViewHint = SimpleStringProperty(i18n.formatText("${myMode.name.toLowerCase()}.listViewHint"))
    this.paneElements = builder.apply {
      withI18N(i18n)
      withBreadcrumbs(
          if (filePath.toFile().isDirectory) createPath(filePath.toFile())
          else createPath(filePath.parent.toFile())
      )
      withActionButton { btn ->
        actionButtonHandler.button = btn
        btn.addEventHandler(ActionEvent.ACTION) {
          actionButtonHandler.onAction()
        }
      }
      withListView(
          onSelectionChange = actionButtonHandler::onSelectionChange,
          onLaunch = {
            if (it is FileAsFolderItem) {
              myDocumentReceiver(FileDocument(it.file))
            }
          },
          onNameTyped = actionButtonHandler::onNameTyped
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

    val btnBrowse = buildFontAwesomeButton(
        iconName = FontAwesomeIcon.SEARCH.name,
        label = i18n.formatText("btn"),
        onClick = { onBrowse() },
        styleClass = "local-storage-browse"
    )
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

private val i18n = RootLocalizer.createWithRootKey("storageService.local", BROWSE_PANE_LOCALIZER)
