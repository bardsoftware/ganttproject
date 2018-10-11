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

import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.storage.BrowserPaneBuilder
import biz.ganttproject.storage.BrowserPaneElements
import biz.ganttproject.storage.FolderItem
import biz.ganttproject.storage.StorageDialogBuilder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.language.GanttLanguage
import java.time.Instant

import java.nio.file.Path
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

/**
 * Wraps JSON node matching a team to FolderItem
 */
class TeamJsonAsFolderItem(val node: JsonNode) : FolderItem {
  override val isLocked = false
  override val isLockable = false
  override val canChangeLock = false
  override val name: String
    get() = this.node["name"].asText()
  override val isDirectory = true
}

class ProjectJsonAsFolderItem(val node: JsonNode) : FolderItem {
  override val canChangeLock: Boolean
    get() {
      return if (!isLocked) isLockable else {
        val lockNode = this.node["lock"]
        lockNode["uid"].asText().substringAfterLast(':') == GPCloudOptions.userId.value
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
  override val isLockable = true
  override val name: String
    get() = this.node["name"].asText()
  override val isDirectory = false
  val refid: String = this.node["refid"].asText()
}

class VersionJsonAsFolderItem(val node: JsonNode): FolderItem {
  override val isLocked = false
  override val isLockable = false
  override val name: String
    get() = """${node["author"]} [${this.formatTimestamp()}]"""
  override val isDirectory = false
  override val canChangeLock = false

  private fun formatTimestamp(): String {
    return GanttLanguage.getInstance().formatDateTime(CalendarFactory.newCalendar().let {
      it.timeInMillis = node["timestamp"].asLong()
      it.time
    })
  }
}
/**
 * This pane shows the contents of GanttProject Cloud storage
 * for a signed in user.
 *
 * @author dbarashev@bardsoftware.com
 */
class GPCloudBrowserPane(
    private val mode: StorageDialogBuilder.Mode,
    private val dialogUi: StorageDialogBuilder.DialogUi,
    private val documentConsumer: Consumer<Document>) {
  private val loaderService = LoaderService(dialogUi)
  private val lockService = LockService(dialogUi)
  private val webSocketClient = WebSocketClient(Consumer { this.reload() })
  private val historyService = HistoryService(dialogUi)

  fun createStorageUi(): Pane {
    val builder = BrowserPaneBuilder(this.mode, this.dialogUi) { path, success, loading ->
      loadTeams(path, success, loading)
    }
    var paneElements: BrowserPaneElements? = null

    val actionButtonHandler = object {
      var selectedProject: ProjectJsonAsFolderItem? = null
      var selectedTeam: TeamJsonAsFolderItem? = null

      fun onOpenItem(item: FolderItem) {
        when (item) {
          is ProjectJsonAsFolderItem -> selectedProject = item
          is TeamJsonAsFolderItem -> selectedTeam = item
          else -> {
          }
        }

      }

      fun onAction() {
        selectedProject?.let { this@GPCloudBrowserPane.openDocument(it) }
            ?: this@GPCloudBrowserPane.createDocument(selectedTeam, paneElements!!.filenameInput.text)

      }
    }

    paneElements = builder.apply {
      withBreadcrumbs()
      withActionButton(EventHandler { actionButtonHandler.onAction() })
      withListView(
          onOpenItem = Consumer { actionButtonHandler.onOpenItem(it) },
          onLaunch = Consumer {
            if (it is ProjectJsonAsFolderItem) {
              this@GPCloudBrowserPane.openDocument(it)
            }
          },
          onLock = Consumer {
            if (it is ProjectJsonAsFolderItem) {
              this@GPCloudBrowserPane.toggleProjectLock(it,
                  Consumer { this@GPCloudBrowserPane.reload() },
                  builder.busyIndicatorToggler)
            }
          },
          itemActionFactory = Function { it ->
            if (it is ProjectJsonAsFolderItem) {
              mapOf(
                  "history" to Consumer { item ->
                    this@GPCloudBrowserPane.loadHistory(it, builder.resultConsumer, builder.busyIndicatorToggler)
                  }
              )
            } else {
              Collections.emptyMap<String, Consumer<FolderItem>>()
            }
          }
      )
    }.build()
    return paneElements.pane
  }

  private fun createDocument(selectedTeam: TeamJsonAsFolderItem?, text: String) {
    if (selectedTeam == null) {
      return
    }
    this.documentConsumer.accept(GPCloudDocument(selectedTeam, text))
  }

  private fun openDocument(item: ProjectJsonAsFolderItem) {
    if (item.node is ObjectNode) {
      this.documentConsumer.accept(GPCloudDocument(item))
    }
  }

  private fun loadTeams(path: Path, setResult: Consumer<ObservableList<FolderItem>>, showMaskPane: Consumer<Boolean>) {
    loaderService.apply {
      busyIndicator = showMaskPane
      this.path = path
      onSucceeded = EventHandler { _ ->
        setResult.accept(value)
        showMaskPane.accept(false)
        this@GPCloudBrowserPane.webSocketClient.start()
      }
      onFailed = EventHandler { _ ->
        showMaskPane.accept(false)
        dialogUi.error("Loading failed!")
      }
      onCancelled = EventHandler { _ ->
        showMaskPane.accept(false)
        GPLogger.log("Loading cancelled!")
      }
      restart()
    }
  }

  private fun reload() {
    this.loaderService.jsonResult.set(null)
    this.loaderService.restart()
  }


  private fun toggleProjectLock(item: ProjectJsonAsFolderItem,
                                setResult: Consumer<Boolean>,
                                showMaskPane: Consumer<Boolean>) {
    lockService.apply {
      this.busyIndicator = showMaskPane
      this.project = item
      onSucceeded = EventHandler { _ ->
        setResult.accept(value)
        showMaskPane.accept(false)
      }
      onFailed = EventHandler { _ ->
        showMaskPane.accept(false)
        dialogUi.error("Loading failed!")
      }
      onCancelled = EventHandler { _ ->
        showMaskPane.accept(false)
        GPLogger.log("Loading cancelled!")
      }
      restart()
    }
  }

  fun openWebSocket() {
    this.webSocketClient.start()
  }

  private fun loadHistory(item: ProjectJsonAsFolderItem,
                          resultConsumer: Consumer<ObservableList<FolderItem>>,
                          busyIndicator: Consumer<Boolean>) {
    this.historyService.apply {
      this.busyIndicator = busyIndicator
      this.projectNode = item
      onSucceeded = EventHandler { _ ->
        resultConsumer.accept(this.value)
        this.busyIndicator.accept(false)
      }
      onFailed = EventHandler { _ ->
        busyIndicator.accept(false)
        dialogUi.error("History loading has failed")
      }
      onCancelled = EventHandler { _ ->
        this.busyIndicator.accept(false)
        GPLogger.log("Loading cancelled!")
      }
      restart()
    }
  }
}

