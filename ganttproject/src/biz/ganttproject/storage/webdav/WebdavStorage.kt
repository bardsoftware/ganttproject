// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav

import biz.ganttproject.FXUtil
import biz.ganttproject.storage.*
import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import com.google.common.base.Strings
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.webdav.HttpDocument
import net.sourceforge.ganttproject.document.webdav.WebDavResource
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor
import net.sourceforge.ganttproject.language.GanttLanguage
import java.nio.file.Path
import java.nio.file.Paths
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

  override fun getName(): String {
    return myServer.getName()
  }

  override fun getCategory(): String {
    return "webdav"
  }

  override fun getId(): String {
    return myServer.rootUrl
  }

  override fun createUi(): Pane {
    if (Strings.isNullOrEmpty(myServer.password)) {
      myBorderPane.center = createPasswordUi()
    } else {
      myBorderPane.center = createStorageUi()
    }
    return myBorderPane
  }

  private fun createStorageUi(): Pane {
    val serverUi = WebdavServerUi(myServer,
        when(myMode) {
          StorageDialogBuilder.Mode.OPEN -> StorageMode.Open()
          StorageDialogBuilder.Mode.SAVE -> StorageMode.Save()
        }, myOpenDocument, myDialogUi)
    return serverUi.createStorageUi()
  }

  private fun createPasswordUi(): Pane {
    val passwordPane = WebdavPasswordPane(myServer, Consumer<WebDavServerDescriptor> { this.onPasswordEntered(it) })
    return passwordPane.createUi()
  }

  private fun onPasswordEntered(server: WebDavServerDescriptor) {
    myServer = server
    FXUtil.transitionCenterPane(myBorderPane, createUi()) { myDialogUi.resize() }
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
                     private val myMode: StorageMode,
                     private val myOpenDocument: Consumer<Document>,
                     private val myDialogUi: StorageDialogBuilder.DialogUi) {
  private val i18n = GanttLanguage.getInstance()
  private val myLoadService: WebdavLoadService = WebdavLoadService(myServer)
  private val myUtil = StorageUtil(myMode)
  private val myState = State(server = myServer, resource = null, filename = null, folder = null)

  private fun createResource(state: State): WebDavResource {
    return state.resource ?: myLoadService.createResource(state.folder, state.filename)
  }

  fun createStorageUi(): Pane {
    val rootPane = VBox()
    rootPane.styleClass.add("pane-service-contents")
    rootPane.prefWidth = 400.0


    val buttonBar = HBox()
    buttonBar.styleClass.add("webdav-button-pane")
    val filename = TextField()

    val titleBox = HBox()
    titleBox.styleClass.add("title")
    val title = Label(i18n.formatText(String.format("webdav.ui.title.%s", myMode.name.toLowerCase()), myServer.name))
    titleBox.children.add(title)

    val isLockingSupported = SimpleBooleanProperty()
    isLockingSupported.addListener({ _, _, newValue ->
      System.err.println("is locking supported=" + newValue!!)
    })
    val listView = FolderView(
        myDialogUi,
        Consumer<WebDavResourceAsFolderItem> { item -> deleteResource(item) },
        Consumer<WebDavResourceAsFolderItem> { item -> toggleLockResource(item) },
        isLockingSupported)


    val onSelectCrumb = Consumer { selectedPath: Path ->
      val wrappers = FXCollections.observableArrayList<WebDavResourceAsFolderItem>()
      val consumer = Consumer { webDavResources: ObservableList<WebDavResource> ->
        webDavResources.forEach { resource -> wrappers.add(WebDavResourceAsFolderItem(resource)) }
        listView.setResources(wrappers)
      }
      loadFolder(selectedPath,
          Consumer<Boolean> { myDialogUi.showBusyIndicator(it) },
          consumer, myDialogUi)
    }
    val breadcrumbView = BreadcrumbView(Paths.get("/", myServer.name), onSelectCrumb)

    fun selectItem(item: WebDavResourceAsFolderItem, withEnter: Boolean, withControl: Boolean) {
      if (item.isDirectory && withEnter) {
        breadcrumbView.append(item.name)
        myState.folder = item.myResource
        myState.filename = null
        myState.resource = null
        filename.text = ""
      } else if (!item.isDirectory) {
        myState.resource = item.myResource
        filename.text = item.name
        if (withControl) {
          myOpenDocument.accept(createDocument(myState.server, createResource(myState)))
        }
      }
    }
    fun selectItem(withEnter: Boolean, withControl: Boolean) {
      listView.selectedResource.ifPresent { item -> selectItem(item, withEnter, withControl) }
    }

    fun onFilenameEnter() {
      val filtered = listView.doFilter(filename.text)
      if (filtered.size == 1) {
        selectItem(filtered[0], true, true)
      }
    }

    val errorLabel = Label("", FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE))
    errorLabel.styleClass.addAll("hint", "noerror")
    connect(filename, listView, breadcrumbView, ::selectItem, ::onFilenameEnter)

    val btnSave = Button(i18n.getText(myUtil.i18nKey("storageService.local.%s.actionLabel")))
    val btnBox = setupSaveButton(btnSave, myOpenDocument, myState, this::createResource)

    rootPane.children.addAll(titleBox, breadcrumbView.breadcrumbs, filename, errorLabel, listView.listView, btnBox)
    return rootPane
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
    if (path.nameCount > 1) {
      path = path.subpath(1, path.nameCount)
    } else {
      path = path.root
    }
    myLoadService.setPath(path.toString())
    myState.folder = myLoadService.createRootResource()
    myLoadService.onSucceeded = EventHandler{ _ ->
      setResult.accept(myLoadService.value)
      showMaskPane.accept(false)
    }
    myLoadService.onFailed = EventHandler{ _ ->
      showMaskPane.accept(false)
      dialogUi.error("WebdavService failed!")
    }
    myLoadService.onCancelled = EventHandler{ _ ->
      showMaskPane.accept(false)
      GPLogger.log("WebdavService cancelled!")
    }
    myLoadService.restart()
    showMaskPane.accept(true)
  }
}

fun createDocument(server: WebDavServerDescriptor, resource: WebDavResource): Document {
  return HttpDocument(resource, server.getUsername(), server.password, HttpDocument.NO_LOCK)
}


fun setupSaveButton(btnSave: Button,
                    receiver: Consumer<Document>,
                    state: State, resourceFactory: (State) -> WebDavResource): Node {
  btnSave.addEventHandler(ActionEvent.ACTION, {
    receiver.accept(createDocument(state.server, resourceFactory(state)))
  })
  btnSave.styleClass.add("doclist-save")
  val btnSaveBox = HBox()
  btnSaveBox.styleClass.add("doclist-save-box")
  btnSaveBox.maxWidth = Double.MAX_VALUE
  btnSaveBox.children.addAll(btnSave)
  return btnSaveBox
}