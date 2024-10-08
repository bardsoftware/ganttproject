/*
Copyright 2017-2021 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.storage

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.storage.cloud.GPCloudDocument
import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.scene.control.Button
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor
import org.controlsfx.validation.ValidationResult
import java.io.File
import java.net.URL
import java.util.*
import java.util.function.Consumer

/**
 * This is a storage provider which shows a list of recently opened projects.
 *
 * @author dbarashev@bardsoftware.com
 */
class RecentProjects(
    private val currentDocument: Document,
    private val mode: StorageDialogBuilder.Mode,
    private val documentManager: DocumentManager,
    private val documentReceiver: (Document) -> Unit) : StorageUi {

  private lateinit var paneElements: BrowserPaneElements<RecentDocAsFolderItem>
  override val name = i18n.formatText("listLabel")
  override val category = "desktop"
  override val id = "recent"

  private val state = RecentProjectState(currentDocument, mode)
  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }

  override fun createUi(): Pane {
    val progressLabel = i18n.create("progress.label")
    val builder = BrowserPaneBuilder(mode, { ex -> GPLogger.log(ex) }) { _, success, busyIndicator ->
      val consumer = Consumer<List<RecentDocAsFolderItem>> {
        success.accept(FXCollections.observableArrayList(it))
      }
      documentManager.loadRecentDocs(consumer, busyIndicator, progressLabel)
    }

    val actionButtonHandler = object {
      var selectedItem: RecentDocAsFolderItem? = null
      var button: Button? = null
        set(btn) {
          btn?.disableProperty()?.bind(state.canWrite.not())
          field = btn
        }

      fun onSelectionChange(item: FolderItem) {
        if (item is RecentDocAsFolderItem) {
          selectedItem = item
          state.currentItem = item
        }
      }

      fun onAction() {
        if (mode == StorageDialogBuilder.Mode.OPEN || state.confirmationReceived.value) {
          selectedItem?.let {
            it.asDocument()?.let(documentReceiver) ?: run {
              LOG.error("File {} seems to be not existing", it)
              paneElements.setValidationResult(
                ValidationResult.fromError(
                  paneElements.filenameInput, RootLocalizer.formatText("document.storage.error.read.notExists")
                )
              )
            }
          }
        }
      }
    }

    this.paneElements = builder.apply {
      withI18N(i18n)
      withActionButton { btn ->
        btn.addEventHandler(ActionEvent.ACTION) { actionButtonHandler.onAction() }
        actionButtonHandler.button = btn
      }
      withListView(
        onSelectionChange = actionButtonHandler::onSelectionChange,
        onLaunch = { actionButtonHandler.onAction() },
        itemActionFactory = {
          Collections.emptyMap()
        },
        cellFactory = { CellWithBasePath() },
        onNameTyped = { _, matchedItems, _, _ ->
          state.onNameTyped(matchedItems)
        }
      )
      withListViewHint(progressLabel)
      withValidator { _, _ -> ValidationResult() }
      if (mode == StorageDialogBuilder.Mode.SAVE) {
        withConfirmation(RootLocalizer.create("document.overwrite"), state.confirmationRequired)
      }
    }.build()
    paneElements.breadcrumbView?.show()
    if (this.mode == StorageDialogBuilder.Mode.SAVE) {
      this.paneElements.filenameInput.text = currentDocument.fileName
      this.paneElements.filenameInput.isDisable = true
      this.state.currentItemProperty.addListener { _, _, _ ->
        Platform.runLater { paneElements.confirmationCheckBox?.isSelected = false }
      }
      this.paneElements.confirmationCheckBox?.selectedProperty()?.addListener { _, _, newValue ->
        state.confirmationReceived.value = newValue
      }
    }

    return paneElements.browserPane.also {
      it.stylesheets.addAll(
        "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
        "/biz/ganttproject/storage/FolderViewCells.css"
      )
      val consumer = Consumer<List<RecentDocAsFolderItem>> { list ->
        builder.resultConsumer.accept(FXCollections.observableArrayList(list))
      }
      documentManager.loadRecentDocs(consumer, paneElements.busyIndicator, progressLabel)
    }

  }

  override fun focus() {
    this.paneElements.filenameInput.sceneProperty().addListener { _, oldValue, newValue ->
      if (newValue != null && oldValue == null) {
        this.paneElements.filenameInput.requestFocus()
      }
    }
  }
}

/**
 * Plugs a recent document string into the folder view.
 */
class RecentDocAsFolderItem(
    private val urlString: String,
    webdabServers: GPCloudStorageOptions,
    private val documentManager: DocumentManager
  ) : FolderItem, Comparable<RecentDocAsFolderItem> {
  private val url: URL
  private val scheme: String
  private val webdavServer: WebDavServerDescriptor?

  init {
    var (url, scheme) = urlString.asDocumentUrl()

    if ("http" == scheme || "https" == scheme) {
      webdavServer = webdabServers.list.firstOrNull { urlString.startsWith(it.rootUrl) }
      if (webdavServer != null) {
        scheme = "webdav"
      }
    } else {
      webdavServer = null
    }

    this.url = url
    this.scheme = scheme
  }

  override fun compareTo(other: RecentDocAsFolderItem): Int {
    val result = this.isDirectory.compareTo(other.isDirectory)
    return if (result != 0) {
      -1 * result
    } else {
      this.fullPath.compareTo(other.fullPath)
    }
  }

  fun updateMetadata() {
    when (scheme) {
      "file" -> {
        File(this.url.path).let {
          when {
            it.isFile && it.canWrite() -> this.tags[FolderItemTag.TYPE] = i18n.formatText("tag.local")
            it.isFile && it.canRead() -> this.tags.putAll(mapOf(
                FolderItemTag.TYPE to i18n.formatText("tag.local"),
                FolderItemTag.READONLY to i18n.formatText("tag.readonly")
            ))
            !it.isFile || !it.exists() -> this.tags[FolderItemTag.UNAVAILABLE] = i18n.formatText("tag.unavailable")
            else -> {}
          }
        }
      }
      "cloud" -> {
        this.tags[FolderItemTag.TYPE] = i18n.formatText("tag.cloud")
      }
      "webdav" -> {
        this.tags[FolderItemTag.TYPE] = i18n.formatText("tag.webdav")
      }
      else -> {
      }
    }
  }

  fun asDocument(): Document? =
    when (scheme) {
      "file" -> File(this.url.path).let { if (it.exists()) FileDocument(it) else null }
      "cloud" -> GPCloudDocument(
          teamRefid = null,
          teamName = DocumentUri.createPath(this.fullPath).getParent().getFileName(),
          projectRefid = this.url.host,
          projectName = this.name,
          projectJson = null
      )
      "webdav" -> {
          documentManager.getDocument(this.urlString)
      }
      else -> null
    }

  override fun toString(): String {
    return "RecentDocAsFolderItem(url=$url, scheme='$scheme', stored urlstring=$urlString)"
  }


  override val isLocked: Boolean = false
  override val isLockable: Boolean = false
  override val canChangeLock: Boolean = false
  override val tags: MutableMap<FolderItemTag, String> = mutableMapOf()
  override val name: String
    get() = DocumentUri.createPath(this.fullPath).getFileName()
  override val basePath: String
    get() = when (scheme) {
      "file" -> DocumentUri.createPath(this.fullPath).getParent().normalize().toString()
      "cloud" -> DocumentUri.createPath(this.fullPath).getParent().getFileName()
      "webdav" -> "${this.webdavServer!!.name} ${DocumentUri.createPath(this.fullPath).getParent().normalize()}"
      else -> ""
    }

  private val fullPath: String = this.url.path
  override val isDirectory: Boolean = false

}

private class RecentProjectState(
  val currentDocument: Document,
  val mode: StorageDialogBuilder.Mode) {

  var currentItem: RecentDocAsFolderItem? = null
    set(value) {
      field = value
      currentItemProperty.value = value
      validate()
    }
  val currentItemProperty = SimpleObjectProperty<RecentDocAsFolderItem?>(null)

  // This property indicates if the user must confirm the action on the current file. This may be needed if
  // we're saving a document and currentFile exists.
  val confirmationRequired: SimpleBooleanProperty = SimpleBooleanProperty(false)

  // This property indicates if the user has confirmed his decision to take the action on the current file.
  var confirmationReceived: SimpleBooleanProperty = SimpleBooleanProperty(false).also {
    it.addListener { _, _, _ -> validate() }
  }

  // This property indicates if the action is possible. E.g. if the action is SAVE and currentFile is read-only,
  // this property value will be false.
  val canWrite = SimpleBooleanProperty(false)

  fun onNameTyped(matchedItems: List<RecentDocAsFolderItem>) {
    currentItem = if (mode == StorageDialogBuilder.Mode.SAVE && matchedItems.size == 1) {
      matchedItems[0]
    } else {
      null
    }
  }

  private fun validate() {
    if (mode == StorageDialogBuilder.Mode.SAVE) {
      confirmationRequired.value = this.currentItem?.asDocument()?.uri != this.currentDocument.uri
      canWrite.value = this.currentItem != null && (confirmationReceived.value || !confirmationRequired.value)
    } else {
      canWrite.value = true
      confirmationRequired.value = false
    }
  }
}

private val i18n = RootLocalizer.createWithRootKey("storageService.recent", BROWSE_PANE_LOCALIZER)
private val LOG = GPLogger.create("RecentProjects")
