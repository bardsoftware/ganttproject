/*
Copyright 2016-2020 BarD Software s.r.o

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
package biz.ganttproject.storage.webdav

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.storage.*
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.webdav.HttpDocument
import net.sourceforge.ganttproject.document.webdav.WebDavResource
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor
import java.util.function.Consumer

/**
 * This is a user interface showing the contents of WebDAV storage.
 *
 * @author dbarashev@bardsoftware.com
 */
class WebdavBrowserPane(private val myServer: WebDavServerDescriptor,
                        private val myMode: StorageDialogBuilder.Mode,
                        private val myOpenDocument: (Document) -> Unit,
                        private val myDialogUi: StorageDialogBuilder.DialogUi) {
  private lateinit var path: Path
  private val myLoadService: WebdavLoadService = WebdavLoadService(myServer)
  private val myState = State(server = myServer, resource = null, filename = null, folder = null)
  private lateinit var paneElements: BrowserPaneElements<WebDavResourceAsFolderItem>

  private fun createResource(state: State): WebDavResource {
    return state.resource ?: myLoadService.createResource(state.folder, state.filename)
  }

  fun createStorageUi(): Pane {
    val builder = BrowserPaneBuilder<WebDavResourceAsFolderItem>(this.myMode, this.myDialogUi::error) { path, success, loading ->
      this.path = path
      refresh()
    }
    val isLockingSupported = SimpleBooleanProperty()
    isLockingSupported.addListener { _, _, newValue ->
      System.err.println("is locking supported=" + newValue!!)
    }
    fun onAction() {
      if (myMode == StorageDialogBuilder.Mode.SAVE) {
        myState.filename = myState.filename!!.withGanExtension()
      }
      myOpenDocument(createDocument(myState.server, createResource(myState)))
    }
    builder.apply {
      withI18N(RootLocalizer.createWithRootKey("storageService.webdav", BROWSE_PANE_LOCALIZER))
      withBreadcrumbs(DocumentUri(listOf(), true, myServer.name))
      withListView(
          onSelectionChange = { item ->
            if (item.isDirectory) {
              myState.folder = item.myResource
              myState.filename = null
              myState.resource = null
            } else {
              myState.resource = item.myResource
            }
          },
          onLaunch = {
            myOpenDocument(createDocument(myState.server, createResource(myState)))
          },
          onNameTyped = { filename, _, withEnter, withControl ->
            myState.filename = filename
            if (withEnter && withControl) {
              onAction()
            }
          },
          onDelete = { item ->
            deleteResource(item)
          },
          onLock = { item ->
            toggleLockResource(item)
          },
          canLock = isLockingSupported,
          canDelete = SimpleBooleanProperty(true)
      )


      withActionButton { btn ->
        btn.addEventHandler(ActionEvent.ACTION) {
          onAction()
        }
      }
    }
    paneElements = builder.build()
    paneElements.breadcrumbView?.show()
    return paneElements.browserPane
  }

  private fun refresh() {
    val success = Consumer<ObservableList<WebDavResourceAsFolderItem>> { Platform.runLater { paneElements.listView.setResources(it) } }
    val wrappers = FXCollections.observableArrayList<WebDavResourceAsFolderItem>()
    val selectedName = paneElements.listView.selectedResource.orElse(null)?.name
    val consumer = Consumer { webDavResources: ObservableList<WebDavResource> ->
      webDavResources.forEach { resource -> wrappers.add(WebDavResourceAsFolderItem(resource)) }
      success.accept(wrappers)
      if (selectedName != null) {
        val selectedIdx = webDavResources.indexOfFirst { it.name == selectedName }
        if (selectedIdx >= 0) {
          Platform.runLater { paneElements.listView.listView.selectionModel.select(selectedIdx) }
        }
      }
    }
    loadFolder(path, paneElements.busyIndicator, consumer, myDialogUi)
  }

  private fun deleteResource(folderItem: WebDavResourceAsFolderItem) {
    val resource = folderItem.myResource
    try {
      resource.delete()
      refresh()
    } catch (e: WebDavResource.WebDavException) {
      myDialogUi.error(e)
    }
  }

  private fun toggleLockResource(folderItem: WebDavResourceAsFolderItem) {
    try {
      val resource = folderItem.myResource
      if (resource.isLocked) {
        resource.unlock()
      } else {
        resource.lock(-1)
      }
      refresh()
    } catch (e: WebDavResource.WebDavException) {
      myDialogUi.error(e)
    }
  }

  private fun loadFolder(selectedPath: Path,
                         showMaskPane: Consumer<Boolean>,
                         setResult: Consumer<ObservableList<WebDavResource>>,
                         dialogUi: StorageDialogBuilder.DialogUi) {
    myLoadService.setPath(selectedPath.toString())
    myState.folder = myLoadService.createRootResource()
    myLoadService.apply {
      onSucceeded = EventHandler {
        setResult.accept(value)
        showMaskPane.accept(false)
      }
      onFailed = EventHandler {
        showMaskPane.accept(false)
        dialogUi.error("WebdavService failed!")
      }
      onCancelled = EventHandler {
        showMaskPane.accept(false)
          GPLogger.log("WebdavService cancelled!")
      }
      restart()
    }
    showMaskPane.accept(true)
  }
}

/**
 * This is an adapter class for plugging WebDavResource into FolderView.
 */
class WebDavResourceAsFolderItem(val myResource: WebDavResource) : FolderItem {
  override val isLocked: Boolean
    get() {
      try {
        return myResource.isLocked
      } catch (e: WebDavResource.WebDavException) {
        throw RuntimeException(e)
      }

    }

  override val isLockable: Boolean
    get() = myResource.isLockSupported(true)

  override val canChangeLock: Boolean
    get() = isLockable

  override val name: String
    get() = myResource.name

  override val basePath: String
    get() = myResource.parent.absolutePath

  override val isDirectory: Boolean
    get() {
      try {
        return myResource.isCollection
      } catch (e: WebDavResource.WebDavException) {
        throw RuntimeException(e)
      }

    }
  override val tags = listOf<String>()
}

private data class State(
        val server: WebDavServerDescriptor,
        var resource: WebDavResource?,
        var filename: String?,
        var folder: WebDavResource?
)

private fun createDocument(server: WebDavServerDescriptor, resource: WebDavResource): Document {
  return HttpDocument(resource, server.username, server.password, HttpDocument.NO_LOCK)
}
