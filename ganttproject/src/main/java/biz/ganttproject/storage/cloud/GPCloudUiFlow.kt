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

import biz.ganttproject.FXUtil
import biz.ganttproject.app.DIALOG_STYLESHEET
import biz.ganttproject.app.DialogController
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane

abstract class FlowPage {
  open var active: Boolean
    get() = false
    set(_) {}

  val ui: Pane by lazy { createUi() }

  abstract fun createUi(): Pane
  abstract fun resetUi()
  abstract fun setController(controller: GPCloudUiFlow)
}

open class EmptyFlowPage : FlowPage() {
  override fun createUi() = Pane()
  override fun resetUi() {}
  override fun setController(controller: GPCloudUiFlow) {}
}

typealias FlowPageChanger = (FlowPage, ()->Unit) -> Unit
fun createFlowPageChanger(wrapperPane: BorderPane, controller: DialogController): FlowPageChanger = createFlowPageChanger(wrapperPane, controller::resize)
fun createFlowPageChanger(wrapperPane: BorderPane, resizer: ()->Unit = {}): FlowPageChanger = { page, onFinish ->
  FXUtil.transitionCenterPane(wrapperPane, page.ui) {
    resizer()
    onFinish()
  }
}
class GPCloudUiFlow(
  private val signupPane: FlowPage,
  private val signinPane: FlowPage,
  private val offlineAlertPage: FlowPage,
  private val mainPane: FlowPage,
  private val flowPageChanger: FlowPageChanger,
  private val offlineMainPage: FlowPage
) {
  internal val httpd: HttpServerImpl by lazy {
    HttpServerImpl().apply { this.start() }
  }

  private val tokenVerificationPage = TokenVerificationPage()
  private var currentPage: FlowPage = EmptyFlowPage()

  init {
    listOf(signupPane, signinPane, offlineAlertPage, offlineMainPage, mainPane).forEach { it.setController(this) }
  }

//  private val tokenVerificationUi: Pane by lazy { tokenVerificationPage.createUi() }
//  private val mainUi: Pane by lazy { mainPane.createUi() }
//  private val signupUi: Pane by lazy { signupPane.createUi() }
//  private val signinUi: Pane by lazy { signinPane.createUi() }
//  private val offlineUi: Pane by lazy { offlinePane.createUi() }
  private var startCount = 0

  fun start(sceneId: SceneId = SceneId.SIGNIN) {
    if (startCount++ >= 5) {
      return
    }
    if (sceneId == SceneId.SIGNUP) {
      transition(sceneId)
      return
    }
    // At the moment we do not consider any other start scenes besides SIGNUP and SIGNIN.
    tryAccessToken(
      onSuccess = {
        //webSocket.start()
        sceneChanger(mainPane, SceneId.BROWSER)
        mainPane.resetUi()
      },
      onError = {
        when (it) {
          "NO_ACCESS_TOKEN" -> {
            sceneChanger(signupPane, SceneId.SIGNUP)
          }
          "ACCESS_TOKEN_EXPIRED" -> {
            sceneChanger(signinPane, SceneId.SIGNIN)
          }
          "INVALID" -> {
            sceneChanger(signupPane, SceneId.SIGNUP)
          }
          "OFFLINE" -> {
            sceneChanger(offlineAlertPage, SceneId.OFFLINE)
          }
          else -> {
          }
        }
      }
    )
  }

  private fun tryAccessToken(onSuccess: (String) -> Unit,
                             onError: (String) -> Unit) {
    biz.ganttproject.storage.cloud.http.tryAccessToken(
      onStart = { sceneChanger(tokenVerificationPage, SceneId.TOKEN_SPINNER) },
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

  private fun sceneChanger(newPage: FlowPage, sceneId: SceneId) {
    Platform.runLater {
      currentPage.active = false
      if (sceneId == SceneId.SIGNIN) {
        httpd.onTokenReceived = { token, validity, userId, websocketToken ->
          GPCloudOptions.onAuthToken().invoke(token, validity, userId, websocketToken)
          transition(SceneId.BROWSER)
        }
      }
      flowPageChanger(newPage) {
        newPage.active = true
        currentPage = newPage
      }
    }
  }

  fun transition(page: SceneId) {
    when(page) {
      SceneId.SIGNIN -> {
        sceneChanger(signinPane, page)
      }
      SceneId.BROWSER -> { sceneChanger(mainPane, page) }
      SceneId.OFFLINE_BROWSER -> { sceneChanger(offlineMainPage, page) }
      SceneId.SIGNUP -> { sceneChanger(signupPane, page) }
      else -> error("Transition to $page was not expected")
    }
  }

}

class GPCloudUiFlowBuilder {
  lateinit var flowPageChanger: FlowPageChanger
  lateinit var mainPage: FlowPage
  var offlineAlertPage: FlowPage = EmptyFlowPage()
  var offlineMainPage: FlowPage = EmptyFlowPage()
  private var signinPage: FlowPage = SigninPane()
  private var signupPane: FlowPage = GPCloudSignupPane()


  fun build(): GPCloudUiFlow = GPCloudUiFlow(
    signupPane = signupPane,
    signinPane = signinPage,
    offlineAlertPage = offlineAlertPage,
    offlineMainPage = offlineMainPage,
    mainPane = mainPage,
    flowPageChanger = flowPageChanger
  )
}

fun paneAndImage1(centerNode: Node, imagePath: String = "/icons/ganttproject-logo-512.png"): Pane {
  return BorderPane().also {
    it.styleClass.addAll("dlg", "signup-pane")
    it.stylesheets.addAll(
      DIALOG_STYLESHEET,
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
