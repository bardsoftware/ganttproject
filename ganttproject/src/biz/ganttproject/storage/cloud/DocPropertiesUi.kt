/*
Copyright 2019 BarD Software s.r.o

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

import biz.ganttproject.app.OptionElementData
import biz.ganttproject.app.OptionPaneBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.*
import com.fasterxml.jackson.databind.JsonNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase
import net.sourceforge.ganttproject.language.GanttLanguage
import java.awt.BorderLayout
import java.awt.Component
import java.time.Duration
import java.util.*
import javax.swing.JPanel

typealias OnLockDone = (JsonNode?) -> Unit
typealias BusyUi = (Boolean) -> Unit
typealias LockDurationHandler = (Duration) -> Unit
typealias MirrorOptionHandler = (OnlineDocumentMode) -> Unit

/**
 * @author dbarashev@bardsoftware.com
 */
class DocPropertiesUi(val errorUi: ErrorUi, val busyUi: BusyUi) {

  // ------------------------------------------------------------------------------
  // Locking stuff
  private fun createLockWarningPage(document: GPCloudDocument): Pane {
    val notify = CheckBox("Show notification when lock is released").also {
      it.styleClass.add("mt-5")
      it.isSelected = true
    }

    val vboxBuilder = VBoxBuilder().also {
      it.i18n.rootKey = "cloud.lockOptionPane"
      it.addTitle("title")
      it.add(Label("Locked by ${document.status.value.lockOwnerName}").apply {
        this.styleClass.add("help")
      })
      it.add(notify)
    }
    return vboxBuilder.vbox
  }


//  fun createLockSuggestionPane(document: LockableDocument, onLockDone: OnLockDone): Pane {
//    if (document.status.value.lockedBySomeone) {
//      return Pane(Label("Locked by ${document.status.value.lockOwnerName}"))
//    } else {
//
//      return lockPaneBuilder(document.status.value).run {
//        buildDialogPane(lockDurationHandler(document, onLockDone)).also {
//          it.styleClass.add("dlg-lock")
//          it.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
//        }
//      }
//    }
//  }
//
  private fun lockPaneBuilder(lockStatus: LockStatus): OptionPaneBuilder<Duration> {
    return OptionPaneBuilder<Duration>().apply {
      i18n.rootKey = "cloud.lockOptionPane"
      if (lockStatus.lockExpiration >= 0) {
        titleHelpString = i18n.create("titleHelp.locked").update(
            GanttLanguage.getInstance().formatDateTime(Date(lockStatus.lockExpiration)))
      }
      graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
      elements = listOf(
          OptionElementData(if (lockStatus.locked) "lockRelease" else "lock0h", Duration.ZERO, isSelected = !lockStatus.locked),
          OptionElementData("lockKeep", Duration.ofHours(-1), isSelected = lockStatus.locked),
          OptionElementData("lock1h", Duration.ofHours(1)),
          OptionElementData("lock2h", Duration.ofHours(2)),
          OptionElementData("lock24h", Duration.ofHours(24))
      )
    }
  }

  private fun lockDurationHandler(document: LockableDocument, onLockDone: OnLockDone): LockDurationHandler {
    return { duration ->
      if (!duration.isNegative) {
        toggleProjectLock(
            document = document,
            done = onLockDone,
            busyIndicator = busyUi,
            lockDuration = duration
        )
      }
    }
  }

  private fun toggleProjectLock(document: LockableDocument,
                                done: OnLockDone,
                                busyIndicator: BusyUi,
                                lockDuration: Duration = Duration.ofMinutes(10)) {
    busyIndicator(true)
    document.toggleLocked(lockDuration)
        .thenAccept { status ->
          done(status?.raw)
          busyIndicator(false)
        }
        .exceptionally { ex ->
          errorUi("Failed to lock document")
          GPLogger.log(ex)
          busyIndicator(false)
          return@exceptionally null
        }
  }

  // ----------------------------------------------------------------------------
  // Sync stuff
  private fun mirrorPaneBuilder(document: OnlineDocument): OptionPaneBuilder<OnlineDocumentMode> {
    return OptionPaneBuilder<OnlineDocumentMode>().apply {
      i18n.rootKey = "cloud.offlineMirrorOptionPane"
      elements = listOf(
          OptionElementData(OnlineDocumentMode.MIRROR.name.toLowerCase(), OnlineDocumentMode.MIRROR,
              isSelected = document.mode.value == OnlineDocumentMode.MIRROR),
          OptionElementData(OnlineDocumentMode.ONLINE_ONLY.name.toLowerCase(), OnlineDocumentMode.ONLINE_ONLY,
              isSelected = document.mode.value == OnlineDocumentMode.ONLINE_ONLY)
      )
    }
  }

  private fun mirrorOptionHandler(document: OnlineDocument): MirrorOptionHandler {
    return { mode ->
      if (mode != document.mode.value) {
        when (mode) {
          OnlineDocumentMode.MIRROR -> document.setMirrored(true)
          OnlineDocumentMode.ONLINE_ONLY -> document.setMirrored(false)
          OnlineDocumentMode.OFFLINE_ONLY -> error("Unexpected mode value=$mode at this place")
        }
      }
    }
  }

  // -------------------------------------------------------------------------------
  // History stuff
  private val historyService = HistoryService()

  private data class HistoryPaneData(val pane: Pane, val loader: (GPCloudDocument) -> Nothing?)

  private fun createHistoryPane(): HistoryPaneData {
    val folderView = FolderView(
        exceptionUi = {},
        maybeCellFactory = this@DocPropertiesUi::createHistoryCell
    )
    val vboxBuilder = VBoxBuilder("tab-contents", "section", "history-pane").apply {
      addTitle("cloud.historyPane.title")
      add(Label().also {
        it.textProperty().bind(this.i18n.create("cloud.historyPane.titleHelp"))
        it.styleClass.add("help")
      })
      val listView = folderView.listView
      add(listView, alignment = null, growth = Priority.ALWAYS)

      val btnGet = Button().also {
        it.textProperty().bind(this.i18n.create("cloud.historyPane.btnGet"))
        it.styleClass.add("btn-small-attention")
        it.isDisable = listView.selectionModel.isEmpty
      }
      listView.selectionModel.selectedItemProperty().addListener { _, _, _ -> btnGet.isDisable = listView.selectionModel.isEmpty }
      btnGet.addEventHandler(ActionEvent.ACTION) {
        val selected = listView.selectionModel.selectedItem?.resource?.get() ?: return@addEventHandler
        GlobalScope.launch {
          folderView.document?.fetchVersion(selected.generation)
        }
      }
      add(btnGet, alignment = Pos.CENTER_RIGHT, growth = Priority.NEVER).also {
        it.styleClass.add("pane-buttons")
      }
      vbox.stylesheets.add("/biz/ganttproject/storage/cloud/HistoryPane.css")
    }
    val loader = { doc: GPCloudDocument ->
      folderView.document = doc
      doc.projectJson?.also { projectJson ->
        this.historyService.apply {
          this.busyIndicator = this@DocPropertiesUi.busyUi
          this.projectNode = projectJson
          onSucceeded = EventHandler {
            Platform.runLater { folderView.setResources(this.value) }
            this.busyIndicator(false)
          }
          onFailed = EventHandler {
            busyIndicator(false)
            //dialogUi.error("History loading has failed")
          }
          onCancelled = EventHandler {
            this.busyIndicator(false)
            GPLogger.log("Loading cancelled!")
          }
          restart()
        }
      }
      null
    }
    return HistoryPaneData(vboxBuilder.vbox, loader)
  }

  private fun createHistoryCell(): ListCell<ListViewItem<VersionJsonAsFolderItem>> {
    return object : ListCell<ListViewItem<VersionJsonAsFolderItem>>() {
      override fun updateItem(item: ListViewItem<VersionJsonAsFolderItem>?, empty: Boolean) {
        if (item == null) {
          text = ""
          graphic = null
          return
        }
        super.updateItem(item, empty)
        if (empty) {
          text = ""
          graphic = null
          return
        }

        val vboxBuilder = VBoxBuilder()
        vboxBuilder.add(Label(item.resource.value.formatTimestamp()).also {
          it.styleClass.add("timestamp")
        })
        vboxBuilder.add(Label(item.resource.value.name).also {
          it.styleClass.add("author")
        })
        graphic = vboxBuilder.vbox
      }
    }
  }

  data class LockOfflinePaneElements(val pane: Parent, val lockToggleGroup: ToggleGroup, val mirrorToggleGroup: ToggleGroup)
  fun buildPane(document: GPCloudDocument, onLockDone: OnLockDone): LockOfflinePaneElements {
    val lockToggleGroup = ToggleGroup()

    val mirrorToggleGroup = ToggleGroup()

    val vboxBuilder = VBoxBuilder("tab-contents").apply {
      add(node = mirrorPaneBuilder(document).let {
        it.toggleGroup = mirrorToggleGroup
        it.styleClass = "section"
        it.buildPane()
      })

      val lockNode = if (document.status.value.lockedBySomeone) {
        createLockWarningPage(document)
      } else {
        lockPaneBuilder(document.status.value).let {
          it.toggleGroup = lockToggleGroup
          it.styleClass = "section"
          it.buildPane()
        }
      }
      add(node = lockNode)

    }

    val lockingOffline = Tab("Locking and Offline", vboxBuilder.vbox)
    val historyPane = createHistoryPane()
    val versions = Tab("History", historyPane.pane)
    val tabPane = TabPane(lockingOffline, versions).also {
      it.tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE

    }
    tabPane.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
      when (newValue) {
        versions -> historyPane.loader(document)
        else -> {
        }
      }
    }
    return LockOfflinePaneElements(tabPane, lockToggleGroup, mirrorToggleGroup)
  }

  fun showDialog(document: GPCloudDocument) {
    val lockDurationHandler = lockDurationHandler(document, {})
    val mirrorOptionHandler = mirrorOptionHandler(document)
    dialog { dialogController ->
      val paneElements = buildPane(document, {})
      dialogController.addStyleClass("dlg-lock")
      dialogController.addStyleClass("dlg-cloud-file-options")
      dialogController.addStyleSheet(
          "/biz/ganttproject/app/TabPane.css",
          "/biz/ganttproject/storage/cloud/DocPropertiesUi.css",
          "/biz/ganttproject/storage/StorageDialog.css"
      )
      dialogController.setContent(paneElements.pane)
      dialogController.setupButton(ButtonType.APPLY) {btn ->
        btn.textProperty().bind(RootLocalizer.create("cloud.offlineMirrorOptionPane.btnApply"))
        btn.styleClass.add("btn-attention")
        btn.addEventHandler(ActionEvent.ACTION) {
          val selectedMode = paneElements.mirrorToggleGroup.selectedToggle.userData as OnlineDocumentMode
          mirrorOptionHandler(selectedMode)

          val selectedDuration = paneElements.lockToggleGroup.selectedToggle.userData as Duration
          lockDurationHandler(selectedDuration)
        }

      }

    }
  }
}

class ProjectPropertiesPageProvider : OptionPageProviderBase("project.cloud") {
  override fun getOptionGroups() = emptyArray<GPOptionGroup>()
  override fun hasCustomComponent() = true

  override fun buildPageComponent(): Component {
    val jfxPanel = JFXPanel()
    val wrapper = JPanel(BorderLayout())
    wrapper.add(jfxPanel, BorderLayout.CENTER)
    GlobalScope.launch(Dispatchers.Main) {
      jfxPanel.scene = buildScene()
    }
    return wrapper
  }

  private fun buildScene(): Scene {
    val onlineDocument = this.project.document.asOnlineDocument() ?: return buildNotOnlineDocumentScene()
    if (onlineDocument is GPCloudDocument) {
      val docPropertiesUi = DocPropertiesUi(errorUi = {}, busyUi = {})
      val vboxBuilder = VBoxBuilder("dlg-lock").also {
        it.add(docPropertiesUi.buildPane(onlineDocument, {}).pane, Pos.CENTER, Priority.ALWAYS)
        it.vbox.stylesheets.addAll("/biz/ganttproject/storage/cloud/GPCloudStorage.css", "/biz/ganttproject/storage/StorageDialog.css")
      }
      return Scene(vboxBuilder.vbox)
    } else {
      return buildNotOnlineDocumentScene()
    }
  }

  private fun buildNotOnlineDocumentScene(): Scene {
    val signupPane = GPCloudSignupPane(onTokenCallback = { _, _, _, _ -> }, pageSwitcher = {})
    return Scene(signupPane.createPane())
  }
}
