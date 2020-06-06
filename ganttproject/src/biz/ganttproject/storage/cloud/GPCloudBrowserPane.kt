/*
Copyright 2018 BarD Software s.r.o

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
package biz.ganttproject.storage.cloud

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.storage.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.language.GanttLanguage
import org.apache.http.HttpStatus
import java.time.Instant
import java.util.function.Consumer
import java.util.logging.Level

abstract class CloudJsonAsFolderItem : FolderItem {
  override val tags = listOf<String>()
}
/**
 * Wraps JSON node matching a team to FolderItem
 */
class TeamJsonAsFolderItem(val node: JsonNode) : CloudJsonAsFolderItem() {
  override val isLocked = false
  override val isLockable = false
  override val canChangeLock = false
  override val name: String
    get() = this.node["name"].asText()
  override val isDirectory = true
}

class ProjectJsonAsFolderItem(val node: JsonNode) : CloudJsonAsFolderItem() {
  override val canChangeLock: Boolean
    get() {
      return if (!isLocked) isLockable else {
        val lockNode = this.node["lock"]
        lockNode["uid"].textValue() == GPCloudOptions.userId.value
      }
    }
  override val isLocked: Boolean
    get() {
      val lockNode = this.node["lock"]
      return if (lockNode is ObjectNode) {
        lockNode["expirationEpochTs"].asLong(0) > Instant.now().toEpochMilli()
      } else {
        false
      }
    }
  val lockOwner: String?
    get() {
      val lockNode = this.node["lock"]
      return if (lockNode is ObjectNode) {
        lockNode["name"]?.textValue()
      } else {
        null
      }
    }
  val lockOwnerEmail: String?
    get() {
      val lockNode = this.node["lock"]
      return if (lockNode is ObjectNode) {
        lockNode["email"]?.textValue()
      } else {
        null
      }
    }
  val lockOwnerId: String?
    get() {
      val lockNode = this.node["lock"]
      return if (lockNode is ObjectNode) {
        lockNode["uid"]?.textValue()
      } else {
        null
      }
    }

  override val isLockable = true
  override val name: String
    get() = this.node["name"].asText()
  override val isDirectory = false
  val refid: String = this.node["refid"].asText()

}

class VersionJsonAsFolderItem(val node: JsonNode) : FolderItem {
  override val isLocked = false
  override val isLockable = false
  override val name: String
    get() = node["author"].toString().removeSurrounding("\"")
  override val isDirectory = false
  override val canChangeLock = false
  override val tags = listOf<String>()

  val generation: Long
    get() = node["number"].asLong(-1)

  fun formatTimestamp(): String {
    return GanttLanguage.getInstance().formatDateTime(CalendarFactory.newCalendar().let {
      it.timeInMillis = node["timestamp"].asLong()
      it.time
    })
  }
}

val ROOT_URI = DocumentUri(listOf(), true, "GanttProject Cloud")
/**
 * This pane shows the contents of GanttProject Cloud storage
 * for a signed in user.
 *
 * @author dbarashev@bardsoftware.com
 */
class GPCloudBrowserPane(
    private val mode: StorageDialogBuilder.Mode,
    private val dialogUi: StorageDialogBuilder.DialogUi,
    private val documentManager: DocumentManager,
    private val documentConsumer: (Document) -> Unit) {
  private val loaderService = LoaderService<CloudJsonAsFolderItem>()

  private lateinit var paneElements: BrowserPaneElements<CloudJsonAsFolderItem>
  var controller: GPCloudStorage.Controller? = null

  fun createStorageUi(): Pane {
    val builder = BrowserPaneBuilder<CloudJsonAsFolderItem>(this.mode, this.dialogUi::error) { path, success, loading ->
      loadTeams(path, success, loading)
    }

    val actionButtonHandler = object {
      var button: Button? = null
      var selectedProject: ProjectJsonAsFolderItem? = null
      var selectedTeam: TeamJsonAsFolderItem? = null

      fun onSelectionChange(item: FolderItem) {
        // We just remember the selection and update action button state.
        // We can "open" files but we can't "open" directories.
        selectedProject = null
        selectedTeam = null
        when (item) {
          is ProjectJsonAsFolderItem -> selectedProject = item
          is TeamJsonAsFolderItem -> selectedTeam = item
          else -> {
          }
        }
        button?.isDisable = selectedTeam != null;
      }

      fun onAction() {
        selectedProject?.let { this@GPCloudBrowserPane.openDocument(it) }
            ?: this@GPCloudBrowserPane.createDocument(selectedTeam, paneElements.filenameInput.text)

      }

      fun onNameTyped(filename: String, itemsMatched: List<FolderItem>, withEnter: Boolean, withControl: Boolean) {
        this.button?.isDisable =
            when (this@GPCloudBrowserPane.mode) {
              StorageDialogBuilder.Mode.OPEN -> itemsMatched.isEmpty()
              StorageDialogBuilder.Mode.SAVE -> filename.isBlank()
            }
        if (withEnter && withControl && this@GPCloudBrowserPane.mode == StorageDialogBuilder.Mode.SAVE) {
          this.onAction()
        }
      }
    }

    val listViewHint = SimpleStringProperty(i18n.formatText("open.listViewHint"))

    this.paneElements = builder.apply {
      withI18N(RootLocalizer.createWithRootKey("storageService.cloud", BROWSE_PANE_LOCALIZER))
      withBreadcrumbs(ROOT_URI)
      withActionButton { btn ->
        actionButtonHandler.button = btn
        btn.addEventHandler(ActionEvent.ACTION) {
          actionButtonHandler.onAction()
        }
      }
      withListView(
          onSelectionChange = actionButtonHandler::onSelectionChange,
          onOpenDirectory = {},
          onLaunch = {
            if (it is ProjectJsonAsFolderItem) {
              this@GPCloudBrowserPane.openDocument(it)
            }
          },
          onNameTyped = actionButtonHandler::onNameTyped
      )
      withListViewHint(listViewHint)

    }.build()
    paneElements.browserPane.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")

    webSocket.onStructureChange { Platform.runLater { this.reload() } }
    return paneElements.browserPane
  }

  private fun createDocument(selectedTeam: TeamJsonAsFolderItem?, text: String) {
    if (selectedTeam == null) {
      return
    }
    this.documentConsumer(GPCloudDocument(selectedTeam, text).also {
      it.offlineDocumentFactory = { path -> this.documentManager.newDocument(path) }
      it.proxyDocumentFactory = this.documentManager::getProxyDocument
    })
  }

  private fun openDocument(item: ProjectJsonAsFolderItem) {
    if (item.node is ObjectNode) {
      val document = GPCloudDocument(item)
      document.offlineDocumentFactory = { path -> this.documentManager.newDocument(path) }
      document.proxyDocumentFactory = this.documentManager::getProxyDocument

      this.documentConsumer(document)
      document.listenEvents(webSocket)
    }
  }

  enum class ActionOnLocked { OPEN, CANCEL }

  private fun openDocumentWithLock(document: GPCloudDocument, jsonLock: JsonNode?) {
    println("Lock node=$jsonLock")
    if (jsonLock != null) {
      document.lock = jsonLock
    }
    this@GPCloudBrowserPane.documentConsumer(document)
  }

  private fun <T: CloudJsonAsFolderItem> loadTeams(path: Path, setResult: Consumer<ObservableList<T>>, showMaskPane: Consumer<Boolean>) {
    loaderService.apply {
      busyIndicator = showMaskPane
      this.path = path
      onSucceeded = EventHandler {
        setResult.accept(value as ObservableList<T>)
        showMaskPane.accept(false)
      }
      onFailed = EventHandler {
        showMaskPane.accept(false)
        val ex = this.exception
        when (ex) {
          is GPCloudException -> {
            when (ex.status) {
              HttpStatus.SC_SERVICE_UNAVAILABLE -> loadOfflineMirrors(setResult)
              HttpStatus.SC_FORBIDDEN, HttpStatus.SC_UNAUTHORIZED -> {
                this@GPCloudBrowserPane.controller!!.start()
              }
              else -> dialogUi.error(ex.message ?: "")
            }

          }
          null -> dialogUi.error("Loading failed!")
          else -> {

            GPLogger.getLogger("GPCloud").log(Level.WARNING, "", ex)
            val errorDetails = ex.message
            dialogUi.error("Failed to load data from GanttProject Cloud $errorDetails")
          }

        }
      }
      onCancelled = EventHandler {
        showMaskPane.accept(false)
        GPLogger.log("Loading cancelled!")
      }
      restart()
    }
  }

  private fun reload() {
    reset()
  }

  fun reset() {
    this.loaderService.jsonResult.set(null)
    this.loaderService.restart()
    this.paneElements.breadcrumbView?.path = ROOT_URI
  }
}

private val i18n = RootLocalizer.createWithRootKey("storageService.local", BROWSE_PANE_LOCALIZER)
