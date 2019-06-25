// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav

import biz.ganttproject.FXUtil
import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.storage.*
import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import com.google.common.base.Strings
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.webdav.HttpDocument
import net.sourceforge.ganttproject.document.webdav.WebDavResource
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor
import java.util.*
import java.util.function.Consumer

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

  override val isDirectory: Boolean
    get() {
      try {
        return myResource.isCollection
      } catch (e: WebDavResource.WebDavException) {
        throw RuntimeException(e)
      }

    }
}

/**
 * @author dbarashev@bardsoftware.com
 */
class WebdavStorage(
    private var myServer: WebDavServerDescriptor,
    private val myMode: StorageDialogBuilder.Mode,
    private val myOpenDocument: Consumer<Document>,
    private val myDialogUi: StorageDialogBuilder.DialogUi,
    private val myOptions: GPCloudStorageOptions) : StorageDialogBuilder.Ui {

  private val myBorderPane = BorderPane()

  override val name: String
    get() {
      return myServer.getName()
    }

  override val category = "webdav"

  override val id: String
    get() {
      return myServer.rootUrl
    }

  private fun doCreateUi(): Pane = if (Strings.isNullOrEmpty(myServer.password)) createPasswordUi() else createStorageUi()

  override fun createUi(): Pane = myBorderPane.apply { center = doCreateUi() }

  private fun createStorageUi(): Pane {
    val serverUi = WebdavServerUi(myServer, myMode, myOpenDocument, myDialogUi)
    return serverUi.createStorageUi()
  }

  private fun createPasswordUi(): Pane {
    val passwordPane = WebdavPasswordPane(myServer, Consumer<WebDavServerDescriptor> { this.onPasswordEntered(it) })
    return passwordPane.createUi()
  }

  private fun onPasswordEntered(server: WebDavServerDescriptor) {
    myServer = server
    FXUtil.transitionCenterPane(myBorderPane, doCreateUi()) { myDialogUi.resize() }
  }

  override fun createSettingsUi(): Optional<Pane> {
    val updater = Consumer { server: WebDavServerDescriptor? ->
      if (server == null) {
        myOptions.removeValue(myServer)
      } else {
        myOptions.updateValue(myServer, server)
      }
    }
    return Optional.of(WebdavServerSetupPane(myServer, updater, true).createUi())
  }
}

data class State(val server: WebDavServerDescriptor,
                 var resource: WebDavResource?, var filename: String?, var folder: WebDavResource?)


class WebdavServerUi(private val myServer: WebDavServerDescriptor,
                     private val myMode: StorageDialogBuilder.Mode,
                     private val myOpenDocument: Consumer<Document>,
                     private val myDialogUi: StorageDialogBuilder.DialogUi) {
  private val myLoadService: WebdavLoadService = WebdavLoadService(myServer)
  private val myState = State(server = myServer, resource = null, filename = null, folder = null)

  private fun createResource(state: State): WebDavResource {
    return state.resource ?: myLoadService.createResource(state.folder, state.filename)
  }

  fun createStorageUi(): Pane {
    val builder = BrowserPaneBuilder<WebDavResourceAsFolderItem>(this.myMode, this.myDialogUi::error) { path, success, loading ->
      val wrappers = FXCollections.observableArrayList<WebDavResourceAsFolderItem>()
      val consumer = Consumer { webDavResources: ObservableList<WebDavResource> ->
        webDavResources.forEach { resource -> wrappers.add(WebDavResourceAsFolderItem(resource)) }
        success.accept(wrappers)
      }
      loadFolder(path, loading, consumer, myDialogUi)
    }
    val isLockingSupported = SimpleBooleanProperty()
    isLockingSupported.addListener { _, _, newValue ->
      System.err.println("is locking supported=" + newValue!!)
    }
    builder.apply {
      withI18N(DefaultLocalizer("storageService.webdav", BROWSE_PANE_LOCALIZER))
      withBreadcrumbs(DocumentUri(listOf(), true, myServer.name))
      withListView(
          onOpenItem = Consumer { item ->
            if (item is WebDavResourceAsFolderItem) {
              if (item.isDirectory) {
                myState.folder = item.myResource
                myState.filename = null
                myState.resource = null
              } else {
                myState.resource = item.myResource
              }
            }
          },
          onLaunch = Consumer {
            myOpenDocument.accept(createDocument(myState.server, createResource(myState)))
          },
          onDelete = Consumer { item ->
            if (item is WebDavResourceAsFolderItem) {
              deleteResource(item)
            }
          },
          onLock = Consumer { item ->
            if (item is WebDavResourceAsFolderItem) {
              toggleLockResource(item)
            }
          },
          canLock = isLockingSupported,
          canDelete = SimpleBooleanProperty(true)
      )


      withActionButton(EventHandler {
        myOpenDocument.accept(createDocument(myState.server, createResource(myState)))
      })
    }
    val browserPaneElements = builder.build()
    return browserPaneElements.browserPane
  }

  private fun deleteResource(folderItem: WebDavResourceAsFolderItem) {
    val resource = folderItem.myResource
    try {
      resource.delete()
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
    } catch (e: WebDavResource.WebDavException) {
      myDialogUi.error(e)
    }
  }

  private fun loadFolder(selectedPath: Path,
                         showMaskPane: Consumer<Boolean>,
                         setResult: Consumer<ObservableList<WebDavResource>>,
                         dialogUi: StorageDialogBuilder.DialogUi) {
    var path = selectedPath
    if (path.getNameCount() > 1) {
      path = path.subpath(1, path.getNameCount())
    } else {
      path = path.getRoot()
    }
    myLoadService.setPath(path.toString())
    myState.folder = myLoadService.createRootResource()
    myLoadService.apply {
      onSucceeded = EventHandler { _ ->
        setResult.accept(value)
        showMaskPane.accept(false)
      }
      onFailed = EventHandler { _ ->
        showMaskPane.accept(false)
        dialogUi.error("WebdavService failed!")
      }
      onCancelled = EventHandler { _ ->
        showMaskPane.accept(false)
        GPLogger.log("WebdavService cancelled!")
      }
      restart()
    }
    showMaskPane.accept(true)
  }
}

fun createDocument(server: WebDavServerDescriptor, resource: WebDavResource): Document {
  return HttpDocument(resource, server.getUsername(), server.password, HttpDocument.NO_LOCK)
}
