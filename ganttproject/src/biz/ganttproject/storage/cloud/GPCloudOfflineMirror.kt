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
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.*
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import java.time.Instant
import java.util.*
import java.util.function.Consumer

/**
 * Represents local offline mirror document in the document browser pane.
 */
class OfflineMirrorOptionsAsFolderItem(val options: GPCloudFileOptions) : CloudJsonAsFolderItem() {
  override val isLockable: Boolean = false
  override val name: String = options.name.ifBlank { options.offlineMirror ?: "Untitled" }
  override val isDirectory: Boolean = false
  override val canChangeLock: Boolean = false
  override val isLocked: Boolean
    get() = Instant.ofEpochMilli(options.lockExpiration.toLongOrNull() ?: 0).isAfter(Instant.now())

}

/**
 * Builds offline notification pane and offline browser pane.
 */
class GPCloudOfflinePane(
    val mode: StorageDialogBuilder.Mode,
    private val dialogUi: StorageDialogBuilder.DialogUi,
    private val documentManager: DocumentManager,
    private val documentConsumer: (Document) -> Unit) {
  var controller: GPCloudStorage.Controller? = null

  fun createPane(): Pane {
    val rootPane = VBoxBuilder("pane-service-contents", "cloud-storage")
    rootPane.add(Pane(), Pos.CENTER, Priority.ALWAYS)
    rootPane.add(buildContentPane(), Pos.CENTER, Priority.NEVER)
    rootPane.add(Pane(), Pos.CENTER, Priority.ALWAYS)
    return rootPane.vbox
  }

  private fun buildContentPane(): Pane {
    val vbox = VBoxBuilder("content-pane")
    vbox.addTitle("You seem to be offline")
    vbox.add(Label("We couldn't contact $GPCLOUD_HOST by name nor by IP address").apply { styleClass.add("help") })

    val toggleGroup = ToggleGroup()

    val btnOffline = RadioButton("Open offline project mirror").also {
      it.styleClass.add("mt-5")
      it.isSelected = true
      it.styleClass.add("btn-lock-expire")
      it.toggleGroup = toggleGroup
      vbox.add(it)
    }
    val btnTryAgain = RadioButton("Try contacting GanttProject Cloud again").also {
      it.styleClass.add("btn-lock-expire")
      it.toggleGroup = toggleGroup
      vbox.add(it)
    }

    return DialogPane().apply {
      styleClass.add("dlg-lock")
      stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      graphic = FontAwesomeIconView(FontAwesomeIcon.SIGNAL)

      content = vbox.vbox

      buttonTypes.addAll(ButtonType.OK)
      lookupButton(ButtonType.OK).apply {
        if (this is Button) {
          text = "Continue"
          styleClass.add("btn-attention")
          addEventHandler(ActionEvent.ACTION) { evt ->
            when {
              btnTryAgain.isSelected -> {
                controller?.start()
              }
              btnOffline.isSelected -> {
                controller?.sceneChanger?.invoke(this@GPCloudOfflinePane.browser)
              }
              else -> {

              }
            }
          }
        }
      }
    }
  }

  val browser: Pane by lazy(this::createBrowserPane)

  private val actionButtonHandler = object {
    private var selectedProject: FolderItem? = null

    fun onSelectionChange(item: FolderItem) {
      this.selectedProject = item
    }

    fun onAction() {
      selectedProject?.let {
        if (it is OfflineMirrorOptionsAsFolderItem) {
          it.options.offlineMirror?.let { path ->
            documentConsumer(documentManager.newDocument(path))
          }
        }
      }
    }
  }

  private fun createBrowserPane(): Pane {
    val builder = BrowserPaneBuilder<OfflineMirrorOptionsAsFolderItem>(this.mode, this.dialogUi::error) { path, success, loading ->
      loadOfflineMirrors(success)
    }

    val paneElements = builder.apply {
      withI18N(RootLocalizer.createWithRootKey("storageService.cloudOffline", BROWSE_PANE_LOCALIZER))
      withBreadcrumbs(DocumentUri(listOf(), true, "Offline Cloud Documents"))
      withActionButton {}
      withListView(
          onSelectionChange = actionButtonHandler::onSelectionChange,
          itemActionFactory = java.util.function.Function { Collections.emptyMap() }
      )
      withActionButton { btn ->
        btn.addEventHandler(ActionEvent.ACTION) {
          actionButtonHandler.onAction()
        }
      }

    }.build()
    paneElements.browserPane.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
    return paneElements.browserPane
  }
}

fun <T: CloudJsonAsFolderItem> loadOfflineMirrors(consumer: Consumer<ObservableList<T>>) {
  val mirrors = GPCloudOptions.cloudFiles.files.entries.mapNotNull { (fp, options) ->
    options.offlineMirror?.let {
      OfflineMirrorOptionsAsFolderItem(options)
    }
  }
  consumer.accept(FXCollections.observableArrayList(mirrors) as ObservableList<T>)
}

