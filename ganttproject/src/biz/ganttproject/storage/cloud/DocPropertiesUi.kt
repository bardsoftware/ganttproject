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
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.*
import com.fasterxml.jackson.databind.JsonNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import java.time.Duration

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
  fun createLockSuggestionPane(document: LockableDocument, onLockDone: OnLockDone): Pane {
    return lockPaneBuilder().run {
      buildDialogPane(lockDurationHandler(document, onLockDone)).also {
        it.styleClass.add("dlg-lock")
        it.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      }
    }
  }

  private fun lockPaneBuilder(): OptionPaneBuilder<Duration> {
    return OptionPaneBuilder<Duration>().apply {
      i18n.rootKey = "cloud.lockOptionPane"
      graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
      elements = listOf(
          OptionElementData("lock0h", Duration.ZERO),
          OptionElementData("lock1h", Duration.ofHours(1), isSelected = true),
          OptionElementData("lock2h", Duration.ofHours(2)),
          OptionElementData("lock24h", Duration.ofHours(24))
      )
    }
  }

  private fun lockDurationHandler(document: LockableDocument, onLockDone: OnLockDone): LockDurationHandler {
    return { duration ->
      if (!duration.isZero || document.status.get().locked) {
        toggleProjectLock(
            document = document,
            done = onLockDone,
            busyIndicator = busyUi,
            lockDuration = duration
        )
      } else {
        onLockDone(null)
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
  fun mirrorPaneBuilder(document: OnlineDocument): OptionPaneBuilder<OnlineDocumentMode> {
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
      when (mode) {
        OnlineDocumentMode.MIRROR -> document.setMirrored(true)
        OnlineDocumentMode.ONLINE_ONLY -> document.setMirrored(false)
        OnlineDocumentMode.OFFLINE_ONLY -> error("Unexpected mode value=$mode at this place")
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
        cellFactory = this@DocPropertiesUi::createHistoryCell
    )
    val vboxBuilder = VBoxBuilder("tab-contents", "history-pane").apply {
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

  fun showDialog(document: GPCloudDocument, onLockDone: OnLockDone) {
    Platform.runLater {
      val lockToggleGroup = ToggleGroup()
      val lockDurationHandler = lockDurationHandler(document, onLockDone)

      val mirrorToggleGroup = ToggleGroup()
      val mirrorOptionHandler = mirrorOptionHandler(document)

      val vboxBuilder = VBoxBuilder("tab-contents").apply {
        add(node = mirrorPaneBuilder(document).let {
          it.toggleGroup = mirrorToggleGroup
          it.styleClass = "section"
          it.buildPane()
        })
        val btnChangeMode = Button().also {
          it.textProperty().bind(this.i18n.create("cloud.offlineMirrorOptionPane.btnApply"))
          it.styleClass.add("btn-small-attention")
          it.addEventHandler(ActionEvent.ACTION) {
            val selectedMode = mirrorToggleGroup.selectedToggle.userData as OnlineDocumentMode
            mirrorOptionHandler(selectedMode)
          }
        }
        add(btnChangeMode, alignment = Pos.CENTER_RIGHT, growth = Priority.NEVER).also {
          it.styleClass.add("pane-buttons")
        }

        add(node = lockPaneBuilder().let {
          it.toggleGroup = lockToggleGroup
          it.styleClass = "section"
          it.buildPane()
        })
        val btnChangeLock = Button().also {
          it.textProperty().bind(this.i18n.create("cloud.lockOptionPane.btnApply"))
          it.styleClass.add("btn-small-attention")
          it.addEventHandler(ActionEvent.ACTION) {
            val selectedDuration = lockToggleGroup.selectedToggle.userData as Duration
            lockDurationHandler(selectedDuration)
          }
        }
        add(btnChangeLock, alignment = Pos.CENTER_RIGHT, growth = Priority.NEVER).also {
          it.styleClass.add("pane-buttons")
        }

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
          else -> {}
        }
      }
      Dialog<Unit>().also {
        it.dialogPane.apply {

          content = tabPane
          styleClass.addAll("dlg-lock", "dlg-cloud-file-options")
          stylesheets.addAll("/biz/ganttproject/storage/cloud/GPCloudStorage.css", "/biz/ganttproject/storage/StorageDialog.css")

          val window = scene.window
          window.onCloseRequest = EventHandler {
            window.hide()
          }
          scene.accelerators[KeyCombination.keyCombination("ESC")] = Runnable{ window.hide() }
//          buttonTypes.add(ButtonType.OK)
//          lookupButton(ButtonType.OK).apply {
//            this.isVisible = false
//            styleClass.add("btn-attention")
//            addEventHandler(ActionEvent.ACTION) {
//
//            }
//          }
        }
        it.show()
      }
    }

  }
}
