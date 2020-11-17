// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage.webdav

import biz.ganttproject.FXUtil
import biz.ganttproject.storage.*
import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import com.google.common.base.Strings
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.webdav.WebDavServerDescriptor
import java.util.*
import java.util.function.Consumer


/**
 * @author dbarashev@bardsoftware.com
 */
class WebdavStorage(
    private var myServer: WebDavServerDescriptor,
    private val myMode: StorageDialogBuilder.Mode,
    private val myOpenDocument: (Document) -> Unit,
    private val myDialogUi: StorageDialogBuilder.DialogUi,
    private val myOptions: GPCloudStorageOptions
) : StorageUi {

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
    val serverUi = WebdavBrowserPane(myServer, myMode, myOpenDocument, myDialogUi)
    return serverUi.createStorageUi()
  }

  private fun createPasswordUi(): Pane {
    val passwordPane = WebdavPasswordPane(myServer, Consumer { this.onPasswordEntered(it) })
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

