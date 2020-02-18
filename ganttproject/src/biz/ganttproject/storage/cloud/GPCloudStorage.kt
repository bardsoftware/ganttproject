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
const val GPCLOUD_IP = "216.239.32.21"
//const val GPCLOUD_HOST = "cloud.ganttproject.biz"
const val GPCLOUD_HOST = "ganttproject.cloud"
const val GPCLOUD_ORIGIN = "https://$GPCLOUD_HOST"
const val GPCLOUD_LANDING_URL = "https://$GPCLOUD_HOST"
const val GPCLOUD_PROJECT_READ_URL = "$GPCLOUD_ORIGIN/p/read"
const val GPCLOUD_SIGNIN_URL = "https://$GPCLOUD_HOST/__/auth/desktop"
const val GPCLOUD_SIGNUP_URL = "https://$GPCLOUD_HOST/__/auth/handler"
const val GPCLOUD_WEBSOCKET_URL = "wss://ws.$GPCLOUD_HOST"
typealias SceneChanger = (Node) -> Unit

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudStorage(
    private val mode: StorageDialogBuilder.Mode,
    private val openDocument: (Document) -> Unit,
    private val dialogUi: StorageDialogBuilder.DialogUi,
    private val documentManager: DocumentManager) : StorageDialogBuilder.Ui {
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
            webSocket.start()
            GlobalScope.launch(Dispatchers.Main) {
              sceneChanger(storageUi)
              browserPane.reset()
            }
          },
          unauthenticated = Consumer {
            when (it) {
              "NO_ACCESS_TOKEN" -> {
                GlobalScope.launch(Dispatchers.Main) {
                  sceneChanger(signupUi)
                }
              }
              "ACCESS_TOKEN_EXPIRED" -> {
                GlobalScope.launch(Dispatchers.Main) {
                  sceneChanger(signinUi)
                }
              }
              "INVALID" -> {
                GlobalScope.launch(Dispatchers.Main) {
                  sceneChanger(signupUi)
                }
              }
              "OFFLINE" -> {
                sceneChanger(offlineUi)
              }
              else -> {
              }
            }
          }
      )
    }
  }

  private fun doCreateUi(): Pane {
    val browserPane = GPCloudBrowserPane(this.mode, this.dialogUi, this.documentManager) { doc ->
      GlobalScope.async(Dispatchers.JavaFx) {
        val spinner = Spinner().also { it.state = Spinner.State.WAITING }
        nextPage(spinner.pane)
      }
      GlobalScope.launch {
        openDocument(doc)
      }
    }
    val onTokenCallback: AuthTokenCallback = { token, validity, userId, websocketToken ->
      GPCloudOptions.onAuthToken().invoke(token, validity, userId, websocketToken)
      Platform.runLater {
        nextPage(browserPane.createStorageUi())
      }
    }

    val offlinePane = GPCloudOfflinePane(this.mode, this.dialogUi)
    val signinPane = SigninPane(onTokenCallback)
    val signupPane = GPCloudSignupPane(signinPane, ::nextPage)
    Controller(signupPane, signinPane, offlinePane, browserPane, this::nextPage).start()
    return myPane
  }

  private fun nextPage(newPage: Node) {
    Platform.runLater {
      FXUtil.transitionCenterPane(myPane, newPage) { dialogUi.resize() }
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
