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

import biz.ganttproject.FXUtil
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.Spinner
import biz.ganttproject.storage.BROWSE_PANE_LOCALIZER
import biz.ganttproject.storage.StorageDialogBuilder
import biz.ganttproject.storage.StorageUi
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

//const val GPCLOUD_HOST = "cumulus-dot-ganttproject-cloud.appspot.com"
const val GPCLOUD_SCHEME = "https"
//const val GPCLOUD_HOST = "cloud.ganttproject.biz"
const val GPCLOUD_HOST = "ganttproject.cloud"
const val GPCLOUD_ORIGIN = "$GPCLOUD_SCHEME://$GPCLOUD_HOST"
const val GPCLOUD_PROJECT_READ_URL = "$GPCLOUD_ORIGIN/p/read"
const val GPCLOUD_SIGNIN_URL = "$GPCLOUD_ORIGIN/__/auth/desktop"
const val GPCLOUD_SIGNUP_URL = "$GPCLOUD_ORIGIN/__/auth/handler"
const val GPCLOUD_WEBSOCKET_URL = "wss://ws.$GPCLOUD_HOST"
enum class SceneId { BROWSER, SIGNUP, SIGNIN, OFFLINE, SPINNER, TOKEN_SPINNER, OFFLINE_BROWSER }
typealias SceneChanger = (Node, SceneId) -> Unit

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudStorage(
    private val dialogUi: StorageDialogBuilder.DialogUi,
    private val mode: StorageDialogBuilder.Mode,
    private val currentDocument: Document,
    private val openDocument: (Document) -> Unit,
    private val documentManager: DocumentManager) : StorageUi {
  private lateinit var browserPane: GPCloudBrowserPane
  private val myPane: BorderPane = BorderPane()

  override val name = i18n.formatText("listLabel")

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }

  override val category = "cloud"

  override fun createUi(): Pane {
    return doCreateUi()
  }

  private fun doCreateUi(): Pane {
    val documentConsumer: (Document) -> Unit = {doc ->
      GlobalScope.launch {
        openDocument(doc)
      }
    }
    browserPane = GPCloudBrowserPane(this.mode, this.dialogUi, this.documentManager, documentConsumer, this.currentDocument)

    val offlinePane = GPCloudOfflinePane(this.mode)
    val offlineBrowser = GPCloudOfflineBrowser(this.mode, this.dialogUi, documentConsumer)
    GPCloudUiFlowBuilder().apply {
      wrapperPane = myPane
      dialog = dialogUi.dialogController
      mainPage = browserPane
      offlineAlertPage = offlinePane
      offlineMainPage = offlineBrowser
      build().start()
    }
    return myPane
  }

  private fun nextPage(newPage: Node, sceneId: SceneId) {
    Platform.runLater {
      FXUtil.transitionCenterPane(myPane, newPage) {
        dialogUi.resize()
        if (sceneId == SceneId.BROWSER) {
          browserPane.focus()
        }
      }
    }
  }

}

fun (GPCloudOptions).onAuthToken(): AuthTokenCallback {
  return { token, validity, userId, websocketToken ->
    val validityAsLong = validity?.toLongOrNull()
    this.authToken.value = token
    this.validity.value = if (validityAsLong == null || validityAsLong == 0L) {
      ""
    } else {
      Instant.now().plus(validityAsLong, ChronoUnit.HOURS).epochSecond.toString()
    }
    this.userId.value = userId
    this.websocketToken = websocketToken
    webSocket.start()
    this.cloudStatus.value = CloudStatus.CONNECTED
  }
}

fun (GPCloudOptions).disconnect() {
  webSocket.stop()
  authToken.value = ""
  validity.value = ""
  userId.value = ""
  websocketToken = ""
  cloudStatus.value = CloudStatus.DISCONNECTED

}
private val i18n = RootLocalizer.createWithRootKey("storageService.cloud", BROWSE_PANE_LOCALIZER)
