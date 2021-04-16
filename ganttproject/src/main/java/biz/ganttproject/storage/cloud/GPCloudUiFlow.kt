/*
Copyright 2021 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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

import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

interface FlowPage {
  fun createUi(): Pane
  fun resetUi()
  fun setController(controller: GPCloudUiFlow)
}
interface SignupPage : FlowPage

class GPCloudUiFlow(
  private val signupPane: FlowPage,
  private val signinPane: FlowPage,
  val offlinePane: FlowPage,
  private val browserPane: FlowPage,
  val sceneChanger: SceneChanger
) {
  private val tokenVerificationPage = TokenVerificationPage()
  init {
    offlinePane.setController(this)
    browserPane.setController(this)
  }

  private val tokenVerificationUi: Pane by lazy { tokenVerificationPage.createUi() }
  private val storageUi: Pane by lazy { browserPane.createUi() }
  private val signupUi: Pane by lazy { signupPane.createUi() }
  private val signinUi: Pane by lazy { signinPane.createUi() }
  private val offlineUi: Pane by lazy { offlinePane.createUi() }
  private var startCount = 0

  fun start() {
    if (startCount++ >= 5) {
      return
    }
    tryAccessToken(
        onSuccess = {
          //webSocket.start()
          GlobalScope.launch(Dispatchers.Main) {
            sceneChanger(storageUi, SceneId.BROWSER)
            browserPane.resetUi()
          }
        },
        onError = {
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

  fun tryAccessToken(onSuccess: (String) -> Unit,
                     onError: (String) -> Unit) {
    biz.ganttproject.storage.cloud.http.tryAccessToken(
      onStart = { sceneChanger(tokenVerificationUi, SceneId.TOKEN_SPINNER) },
      onSuccess = {
        GPCloudOptions.cloudStatus.value = CloudStatus.CONNECTED
        onSuccess(it)
      },
      onError = {
        GPCloudOptions.cloudStatus.value = CloudStatus.DISCONNECTED
        onError(it)
      }
    )
  }
}


fun paneAndImage(centerNode: Node, imagePath: String = "/icons/ganttproject-logo-512.png"): Pane {
  return BorderPane().also {
    it.styleClass.addAll("dlg", "signup-pane")
    it.stylesheets.addAll(
      "/biz/ganttproject/app/Dialog.css",
      "/biz/ganttproject/app/Util.css",
      "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
      "/biz/ganttproject/storage/cloud/GPCloudSignupPane.css"
    )
    it.left = ImageView(
      Image(
      GPCloudStorage::class.java.getResourceAsStream(imagePath),
      64.0, 64.0, false, true)
    )
    it.center = centerNode.also { node -> node.styleClass.add("signup-body") }
  }
}
