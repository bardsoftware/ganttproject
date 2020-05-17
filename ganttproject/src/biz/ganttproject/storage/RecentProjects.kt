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
package biz.ganttproject.storage

//import biz.ganttproject.storage.local.setupErrorLabel
import biz.ganttproject.app.RootLocalizer
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.FileDocument
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer

/**
 * This is a storage provider which shows a list of recently opened projects.
 *
 * @author dbarashev@bardsoftware.com
 */
class RecentProjects(
    private val mode: StorageDialogBuilder.Mode,
    private val documentManager: DocumentManager,
    private val currentDocument: Document,
    private val documentReceiver: (Document) -> Unit) : StorageDialogBuilder.Ui {

  override val name = i18n.formatText("listLabel")
  override val category = "desktop"
  override val id = "recent"

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }

  override fun createUi(): Pane {
    val builder = BrowserPaneBuilder<RecentDocAsFolderItem>(mode, { ex -> GPLogger.log(ex) }) { _, success, _ ->
      loadRecentDocs(success)
    }

    val actionButtonHandler = object {
      var selectedItem: RecentDocAsFolderItem? = null

      fun onSelectionChange(item: FolderItem) {
        if (item is RecentDocAsFolderItem) {
            selectedItem = item
          }
        }


      fun onAction() {
        // TODO: This currently works for local docs only. Make it working for GP Cloud/WebDAV docs too.
        selectedItem?.let {
          val file = it.docPath.toFile()
          if (file.exists()) {
            documentReceiver(FileDocument(file))
          }
        }
      }
    }

    val paneElements = builder.apply {
      withI18N(i18n)
      withActionButton { btn -> btn.addEventHandler(ActionEvent.ACTION) { actionButtonHandler.onAction() }}
      withListView(
          onSelectionChange = actionButtonHandler::onSelectionChange,
          onLaunch = { actionButtonHandler.onAction() },
          itemActionFactory = java.util.function.Function {
            Collections.emptyMap()
          },
          cellFactory = { createListCell() }
      )
    }.build()

    return paneElements.browserPane.also {
      it.stylesheets.addAll(
        "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
        "/biz/ganttproject/storage/RecentProjects.css"
      )
      loadRecentDocs(builder.resultConsumer)
    }

  }

  private fun createListCell(): ListCell<ListViewItem<RecentDocAsFolderItem>> {
    return object : ListCell<ListViewItem<RecentDocAsFolderItem>>() {
      override fun updateItem(item: ListViewItem<RecentDocAsFolderItem>?, empty: Boolean) {
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
        val pane = StackPane()
        pane.minWidth = 0.0
        pane.prefWidth = 1.0

        pane.children.add(VBox().also { vbox ->
          vbox.isFillWidth = true
          vbox.children.add(
              Label(item.resource.get().docPath.parent?.normalize()?.toString() ?: "").apply {
                styleClass.add("list-item-path")
              }
          )
          vbox.children.add(
              Label(item.resource.get().docPath.fileName.toString()).apply {
                styleClass.add("list-item-filename")
              }
          )
          item.resource.value.tags.let {
            if (it.isNotEmpty()) {
              vbox.children.add(
                  HBox(Label(it.joinToString(", "))).apply {
                    styleClass.add("list-item-tags")
                  }
              )
            }
          }
          StackPane.setAlignment(vbox, Pos.BOTTOM_LEFT)
        })

        graphic = pane
      }
    }
  }
  private fun loadRecentDocs(consumer: Consumer<ObservableList<RecentDocAsFolderItem>>) {
    val result = FXCollections.observableArrayList<RecentDocAsFolderItem>()

    runBlocking {
      result.addAll(documentManager.recentDocuments.map { path ->
        GlobalScope.async {
          // TODO: This currently works for local docs only. Make it working for GP Cloud/WebDAV docs too.
          documentManager.newDocument(path)?.let {doc ->
            when {
              doc.isLocal && doc.canWrite().isOK -> RecentDocAsFolderItem(Paths.get(path), listOf(i18n.formatText("tag.local")))
              doc.isLocal && doc.canRead() -> RecentDocAsFolderItem(Paths.get(path), listOf(
                  i18n.formatText("tag.local"), i18n.formatText("tag.readonly")
              ))
              else -> null
            }
          }
        }
      }.awaitAll().filterNotNull())
    }
    consumer.accept(result)
  }
}

class RecentDocAsFolderItem(val docPath: Path, override val tags: List<String>) : FolderItem, Comparable<RecentDocAsFolderItem> {
  override fun compareTo(other: RecentDocAsFolderItem): Int {
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
  override val name: String = this.docPath.toString()
  override val isDirectory: Boolean = false
}

private val i18n = RootLocalizer.createWithRootKey("storageService.recent", BROWSE_PANE_LOCALIZER)
