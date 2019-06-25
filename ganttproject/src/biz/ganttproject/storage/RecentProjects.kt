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
import biz.ganttproject.app.DefaultLocalizer
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.FileDocument
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
class RecentProjects(
    private val mode: StorageDialogBuilder.Mode,
    private val myDocumentManager: DocumentManager,
    private val myCurrentDocument: Document,
    private val myDocumentReceiver: Consumer<Document>) : StorageDialogBuilder.Ui {

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

      fun onOpenItem(item: FolderItem) {
        if (item is RecentDocAsFolderItem) {
            selectedItem = item
          }
        }


      fun onAction() {
        selectedItem?.let {
          val file = it.docPath.toFile()
          if (file.exists()) {
            myDocumentReceiver.accept(FileDocument(file))
          }
        }
      }
    }

    val paneElements = builder.apply {
      withI18N(i18n)
      withActionButton(EventHandler { actionButtonHandler.onAction() })
      withListView(
          onOpenItem = Consumer { actionButtonHandler.onOpenItem(it) },
          onLaunch = Consumer { actionButtonHandler.onAction() },
          itemActionFactory = java.util.function.Function {
            Collections.emptyMap()
          },
          cellFactory = { createListCell() }
      )
    }.build()
    paneElements.browserPane.stylesheets.addAll(
        "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
        "/biz/ganttproject/storage/RecentProjects.css")

    return paneElements.browserPane.also {
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
        val pathLabel = Label(item.resource.get().docPath.parent?.normalize()?.toString() ?: "")
        pathLabel.styleClass.add("list-item-path")
        val nameLabel = Label(item.resource.get().docPath.fileName.toString())
        nameLabel.styleClass.add("list-item-filename")
        val labelBox = VBox()
        labelBox.children.addAll(pathLabel, nameLabel)
        StackPane.setAlignment(labelBox, Pos.BOTTOM_LEFT)
        pane.children.add(labelBox)
        graphic = pane
      }
    }
  }
  private fun loadRecentDocs(consumer: Consumer<ObservableList<RecentDocAsFolderItem>>) {
    val result = FXCollections.observableArrayList<RecentDocAsFolderItem>()
    result.add(RecentDocAsFolderItem(Paths.get(myCurrentDocument.path)))
    for (doc in myDocumentManager.recentDocuments) {
      result.add(RecentDocAsFolderItem(Paths.get(doc)))
    }
    consumer.accept(result)
  }
}

class RecentDocAsFolderItem(val docPath: Path) : FolderItem, Comparable<RecentDocAsFolderItem> {
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

private val i18n = DefaultLocalizer("storageService.recent", BROWSE_PANE_LOCALIZER)
