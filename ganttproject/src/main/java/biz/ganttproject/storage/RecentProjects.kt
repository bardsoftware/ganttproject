/*
Copyright 2017-2020 Dmitry Barashev, BarD Software s.r.o

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
import biz.ganttproject.storage.cloud.GPCloudDocument
import biz.ganttproject.storage.cloud.onboard
import biz.ganttproject.storage.cloud.webSocket
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.scene.layout.Pane
import kotlinx.coroutines.*
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.FileDocument
import java.io.File
import java.net.MalformedURLException
import java.net.URL
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
    private val documentReceiver: (Document) -> Unit) : StorageUi {

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
        // TODO: This currently works for local and Cloud docs only. Make it working for WebDAV docs too.
        selectedItem?.let {
          it.asDocument()?.let(documentReceiver)
        }
      }
    }

    val paneElements = builder.apply {
      withI18N(i18n)
      withActionButton { btn -> btn.addEventHandler(ActionEvent.ACTION) { actionButtonHandler.onAction() }}
      withListView(
          onSelectionChange = actionButtonHandler::onSelectionChange,
          onLaunch = { actionButtonHandler.onAction() },
          itemActionFactory = {
            Collections.emptyMap()
          },
          cellFactory = { CellWithBasePath() }
      )
    }.build()
    paneElements.breadcrumbView?.show()
    return paneElements.browserPane.also {
      it.stylesheets.addAll(
        "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
        "/biz/ganttproject/storage/FolderViewCells.css"
      )
      loadRecentDocs(builder.resultConsumer)
    }

  }

  private fun loadRecentDocs(consumer: Consumer<ObservableList<RecentDocAsFolderItem>>) {
    val result = FXCollections.observableArrayList<RecentDocAsFolderItem>()

    runBlocking {
      documentManager.recentDocuments.map { path ->
        try {
          val doc = RecentDocAsFolderItem(path, documentManager)
          GlobalScope.async(Dispatchers.IO) {
            doc.updateMetadata()
            doc
          }
        } catch (ex: MalformedURLException) {
          LOG.error("Can't parse this recent document record: {}", path)
          CompletableDeferred(value = null)
        }
      }.awaitAll()
    }.filterNotNullTo(result)
    consumer.accept(result)
  }
}

class RecentDocAsFolderItem(urlString: String, private val documentManager: DocumentManager) : FolderItem, Comparable<RecentDocAsFolderItem> {
  private val url: URL
  private val scheme: String
  init {
    val (url, scheme) = try {
      URL(urlString).let {it to it.protocol }
    } catch (ex: MalformedURLException) {
      if (File(urlString).exists()) {
        URL("file:$urlString") to "file"
      } else {
        val indexColon = urlString.indexOf(':')
        val indexSlash = urlString.indexOf('/')
        if (indexColon > 0 && indexSlash == indexColon + 1) {
          URL("http" + urlString.drop(indexColon)) to urlString.take(indexColon)
        } else if (indexSlash == 0) {
          URL("file:$urlString") to "file"
        } else {
          throw ex
        }
      }
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
            it.isFile && it.canWrite() -> this.tags.add(i18n.formatText("tag.local"))
            it.isFile && it.canRead() -> this.tags.addAll(listOf(
                i18n.formatText("tag.local"),
                i18n.formatText("tag.readonly")
            ))
            else -> {}
          }
        }
      }
      "cloud" -> {
        this.tags.add(i18n.formatText("tag.cloud"))
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
      ).also {
        it.onboard(documentManager, webSocket)
      }
      else -> null
    }


  override val isLocked: Boolean = false
  override val isLockable: Boolean = false
  override val canChangeLock: Boolean = false
  override val tags: MutableList<String> = mutableListOf()
  override val name: String
    get() = DocumentUri.createPath(this.fullPath).getFileName()
  override val basePath: String
    get() = when (scheme) {
      "file", "webdav" -> DocumentUri.createPath(this.fullPath).getParent().normalize().toString()
      "cloud" -> DocumentUri.createPath(this.fullPath).getParent().getFileName()
      else -> ""
    }

  val fullPath: String = this.url.path
  override val isDirectory: Boolean = false
}

private val i18n = RootLocalizer.createWithRootKey("storageService.recent", BROWSE_PANE_LOCALIZER)
private val LOG = GPLogger.create("RecentProjects")
