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
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.Notifications
import java.time.Duration
import java.time.Instant
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
    private val documentConsumer: Consumer<Document>,
    private val sceneChanger: SceneChanger) {
  private val loaderService = LoaderService(dialogUi)
  private val lockService = LockService(dialogUi)
  private val historyService = HistoryService(dialogUi)

  private lateinit var paneElements: BrowserPaneElements

  fun createStorageUi(): Pane {
    val builder = BrowserPaneBuilder(this.mode, this.dialogUi) { path, success, loading ->
      loadTeams(path, success, loading)
    }

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

    this.paneElements = builder.apply {
      withBreadcrumbs(DocumentUri(listOf(), true, "GanttProject Cloud"))
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
                  builder.busyIndicatorToggler
              )
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
    paneElements.browserPane.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")

    webSocket.onStructureChange { Platform.runLater { this.reload() } }
    return paneElements.browserPane
  }

  private fun createDocument(selectedTeam: TeamJsonAsFolderItem?, text: String) {
    if (selectedTeam == null) {
      return
    }
    this.documentConsumer.accept(GPCloudDocument(selectedTeam, text))
  }

  private fun openDocument(item: ProjectJsonAsFolderItem) {
    if (item.node is ObjectNode) {
      val document = GPCloudDocument(item)
      if (item.isLocked && item.canChangeLock) {
        this.documentConsumer.accept(document)
      } else {
        if (!item.isLocked) {
          this.sceneChanger(this.createLockSuggestionPane(document))
        } else {
          this.sceneChanger(this.createLockWarningPage(document))
        }
      }
      document.listenLockChange(webSocket)
    }
  }

  private fun createLockSuggestionPane(document: GPCloudDocument): Pane {
    val vbox = VBoxBuilder("lock-button-pane")
    vbox.addTitle("Lock Project")
    vbox.add(Label("You may want to lock the project to prevent concurrent modifications").apply { styleClass.add("help") })

    val lockGroup = ToggleGroup()

    val lock0h = RadioButton("Don't lock").also {
      it.styleClass.add("mt-5")
      it.userData = Duration.ofHours(0)
    }
    val lock1h = RadioButton("Lock for 1h").also {
      it.isSelected = true
      it.userData = Duration.ofHours(1)
    }

    val lock2h = RadioButton("Lock for 2h").also {
      it.userData = Duration.ofHours(2)
    }

    val lock24h = RadioButton("Lock for 24h").also {
      it.userData = Duration.ofHours(24)
    }
    listOf(lock0h, lock1h, lock2h, lock24h).forEach {
      it.styleClass.add("btn-lock-expire")
      it.toggleGroup = lockGroup
      vbox.add(it)
    }


    return DialogPane().apply {
      styleClass.add("dlg-lock")
      stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)

      content = vbox.vbox

      buttonTypes.add(ButtonType.OK)
      lookupButton(ButtonType.OK).apply {
        styleClass.add("btn-attention")
        addEventHandler(ActionEvent.ACTION) { evt ->
          val lockDuration = lockGroup.selectedToggle.userData as Duration
          if (lockDuration.isZero) {
            openDocumentWithLock(document, null)
          } else {
            toggleProjectLock(
                project = document.projectJson!!,
                done = Consumer { this@GPCloudBrowserPane.openDocumentWithLock(document, it) },
                busyIndicator = this@GPCloudBrowserPane.paneElements.busyIndicator,
                requestLockToken = true,
                lockDuration = lockDuration
            )
          }
        }
      }
    }
  }

  private fun createLockWarningPage(document: GPCloudDocument): Pane {
    val lockOwner = document.projectJson!!.lockOwner!!
    val vbox = VBoxBuilder("lock-button-pane")
    vbox.addTitle("Project Is Locked")
    vbox.add(Label("This project is locked by $lockOwner. We recommend keeping it read-only").apply { styleClass.add("help") })

    val notify = CheckBox("Show notification when lock is released").also {
      it.styleClass.add("mt-5")
    }
    notify.isSelected = true
    vbox.add(notify)

    return DialogPane().apply {
      styleClass.add("dlg-lock")
      stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      graphic = FontAwesomeIconView(FontAwesomeIcon.LOCK)

      content = vbox.vbox

      buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
      lookupButton(ButtonType.OK).apply {
        styleClass.add("btn-attention")
        addEventHandler(ActionEvent.ACTION) { evt ->
          openDocumentWithLock(document, document.projectJson.node["lock"])
        }
        if (notify.isSelected) {
          document.status.addListener { status, oldValue, newValue ->
            println("new value=$newValue")
            if (!newValue.locked) {
              Platform.runLater {
                Notifications.create().title("Project Unlocked")
                    .text("User ${newValue?.lockOwnerName ?: ""} has unlocked project ${document.fileName}")
                    .showInformation()
              }
            }
          }
        }
      }
      lookupButton(ButtonType.CANCEL).apply {
        addEventHandler(ActionEvent.ACTION) {
          this@GPCloudBrowserPane.sceneChanger(this@GPCloudBrowserPane.paneElements.browserPane)
        }
      }
    }

  }

  private fun openDocumentWithLock(document: GPCloudDocument, jsonLock: JsonNode?) {
    println("Lock node=$jsonLock")
    if (jsonLock != null) {
      document.lock = jsonLock
    }
    this@GPCloudBrowserPane.documentConsumer.accept(document)
  }

  private fun loadTeams(path: Path, setResult: Consumer<ObservableList<FolderItem>>, showMaskPane: Consumer<Boolean>) {
    loaderService.apply {
      busyIndicator = showMaskPane
      this.path = path
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

  private fun reload() {
    this.loaderService.jsonResult.set(null)
    this.loaderService.restart()
  }


  private fun toggleProjectLock(project: ProjectJsonAsFolderItem,
                                done: Consumer<JsonNode>,
                                busyIndicator: Consumer<Boolean>,
                                requestLockToken: Boolean = false,
                                lockDuration: Duration = Duration.ofMinutes(10)) {
    lockService.apply {
      this.busyIndicator = busyIndicator
      this.project = project
      this.requestLockToken = requestLockToken
      this.duration = lockDuration
      onSucceeded = EventHandler {
        done.accept(value)
        busyIndicator.accept(false)
      }
      onFailed = EventHandler {
        busyIndicator.accept(false)
        dialogUi.error("Loading failed!")
      }
      onCancelled = EventHandler {
        busyIndicator.accept(false)
        GPLogger.log("Loading cancelled!")
      }
      restart()
    }
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

