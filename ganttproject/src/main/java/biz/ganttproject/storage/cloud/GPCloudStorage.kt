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
import java.util.function.Consumer

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

  data class Controller(
      val signupPane: GPCloudSignupPane,
      private val signinPane: SigninPane,
      val offlinePane: GPCloudOfflinePane,
      private val browserPane: GPCloudBrowserPane,
      val sceneChanger: SceneChanger) {
    init {
      offlinePane.controller = this
      browserPane.controller = this
    }

    private val storageUi: Pane by lazy { browserPane.createStorageUi() }
    private val signupUi: Pane by lazy { signupPane.createPane() }
    private val signinUi: Pane by lazy { signinPane.createSigninPane() }
    private val offlineUi: Pane by lazy { offlinePane.createPane() }
    private var startCount = 0

    fun start() {
      if (startCount++ >= 5) {
        return
      }
      signupPane.tryAccessToken(
          success = Consumer {
            //webSocket.start()
            GlobalScope.launch(Dispatchers.Main) {
              sceneChanger(storageUi, SceneId.BROWSER)
              browserPane.reset()
            }
          },
          unauthenticated = Consumer {
            when (it) {
              "NO_ACCESS_TOKEN" -> {
                GlobalScope.launch(Dispatchers.Main) {
                  sceneChanger(signupUi, SceneId.SIGNUP)
                }
              }
              "ACCESS_TOKEN_EXPIRED" -> {
                GlobalScope.launch(Dispatchers.Main) {
                  sceneChanger(signinUi, SceneId.SIGNIN)
                }
              }
              "INVALID" -> {
                GlobalScope.launch(Dispatchers.Main) {
                  sceneChanger(signupUi, SceneId.SIGNUP)
                }
              }
              "OFFLINE" -> {
                sceneChanger(offlineUi, SceneId.OFFLINE)
              }
              else -> {
              }
            }
          }
      )
    }
  }

  private fun doCreateUi(): Pane {
    val documentConsumer: (Document) -> Unit = {doc ->
      GlobalScope.async(Dispatchers.JavaFx) {
        val spinner = Spinner().also { it.state = Spinner.State.WAITING }
        nextPage(spinner.pane, SceneId.SPINNER)
      }
      GlobalScope.launch {
        openDocument(doc)
      }
    }
    browserPane = GPCloudBrowserPane(this.mode, this.dialogUi, this.documentManager, documentConsumer, this.currentDocument)
    val onTokenCallback: AuthTokenCallback = { token, validity, userId, websocketToken ->
      GPCloudOptions.onAuthToken().invoke(token, validity, userId, websocketToken)
      Platform.runLater {
        nextPage(browserPane.createStorageUi(), SceneId.BROWSER)
      }
    }

    val offlinePane = GPCloudOfflinePane(this.mode, this.dialogUi, documentConsumer)
    val signinPane = SigninPane(onTokenCallback)
    val signupPane = GPCloudSignupPane(signinPane) { node, sceneId -> nextPage(node, sceneId) }
    Controller(signupPane, signinPane, offlinePane, browserPane, this::nextPage).start()
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
  }
}

private val i18n = RootLocalizer.createWithRootKey("storageService.cloud", BROWSE_PANE_LOCALIZER)
