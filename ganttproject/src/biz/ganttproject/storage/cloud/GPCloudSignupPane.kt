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

import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.openInBrowser
import com.google.common.io.Closer
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.Pane
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.language.GanttLanguage
import org.apache.http.client.methods.HttpGet
import org.controlsfx.control.HyperlinkLabel
import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import java.util.logging.Level






/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudSignupPane internal constructor(
    val onTokenCallback: AuthTokenCallback,
    val pageSwitcher: SceneChanger) : GPCloudStorage.PageUi {
  private val i18n = GanttLanguage.getInstance()

  private val httpd: HttpServerImpl by lazy {
    HttpServerImpl().apply { this.start() }
  }

  override fun createPane(): CompletableFuture<Pane> {
    val result = CompletableFuture<Pane>()

    val rootPane = VBoxBuilder("pane-service-contents", "cloud-storage")
    rootPane.addTitle("GanttProject Cloud")
    rootPane.add(Label("collaborative storage for your projects").apply {
      this.styleClass.add("subtitle")
    })

    val btnSignIn = Button("Sign In")
    btnSignIn.styleClass.add("btn-attention")

    btnSignIn.addEventHandler(ActionEvent.ACTION) {
      val uri = "$GPCLOUD_SIGNIN_URL?callback=${httpd.listeningPort}"
      expandMsg(uri)

      this.httpd.onTokenReceived = this.onTokenCallback
      openInBrowser(uri)
    }

    rootPane.add(btnSignIn, Pos.CENTER, null).apply {
      this.styleClass.add("doclist-save-box")
    }

    rootPane.add(Label("Not registered yet? Sign up now!").apply {
      this.styleClass.add("h2")
    })
    rootPane.add(HyperlinkLabel("GanttProject Cloud is free for up to 2 users per month. [Learn more]").apply {
      this.styleClass.add("help")
      this.onAction = EventHandler {
        val link = it.source as Hyperlink?
        when (link?.text) {
          "Learn more" -> openInBrowser(GPCLOUD_LANDING_URL)
          else -> Unit
        }
      }
    })
    val signupBtn = Button("Sign Up")
    signupBtn.styleClass.add("btn-attention")
    signupBtn.addEventHandler(ActionEvent.ACTION) {
      openInBrowser(GPCLOUD_SIGNUP_URL)
    }

    rootPane.add(signupBtn, Pos.CENTER, null).apply {
      this.styleClass.add("doclist-save-box")
    }

//    notification.content = rootPane.vbox
//    notification.isShowFromTop = false
//    notification.styleClass.addAll("fill-parent", "alert-info")
//    val wrapperPane = BorderPane(notification)
    rootPane.vbox.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
    result.complete(rootPane.vbox)
    return result
  }

  private fun isNetworkAvailable(): Boolean {
    return try {
      Closer.create().use { closer ->
        val pingSocket = Socket(GPCLOUD_IP, 80).also { closer.register(it) }
        closer.register(pingSocket.getOutputStream())
        closer.register(pingSocket.getInputStream())
      }
      true
    } catch (e: IOException) {
      false
    }
  }

  fun tryAccessToken(success: Consumer<String>, unauthenticated: Consumer<String>) {
    GlobalScope.launch {
      try {
        callAuthCheck(success, unauthenticated)
      } catch (ex: Exception) {
        if (ex is UnknownHostException) {
          if (!isNetworkAvailable()) {
            unauthenticated.accept("OFFLINE")
          } else {
            unauthenticated.accept("")
          }
        } else {
          GPLogger.getLogger("GPCloud").log(Level.SEVERE, "Failed to contact GPCloud server", ex)
          unauthenticated.accept("")
        }
      }
    }
  }

  private fun callAuthCheck(onSuccess: Consumer<String>, onUnauthenticated: Consumer<String>) {
    val http = HttpClientBuilder.buildHttpClient()
    val teamList = HttpGet("/access-token/check")
    val resp = http.client.execute(http.host, teamList, http.context)
    when (resp.statusLine.statusCode) {
      200 -> onSuccess.accept("")
      401 -> onUnauthenticated.accept("INVALID")
      else -> {
        onUnauthenticated.accept("INVALID")
      }
    }
  }

  fun expandMsg(uri: String) {
    val rootPane = VBoxBuilder("pane-service-contents", "cloud-storage", "signing")
    rootPane.addTitle("Signing In...")
    val msgText = """
        We will open a new browser tab to sign in into GanttProject Cloud.
        If it doesn't work, copy this link to your browser address bar:
        ${uri}""".trimIndent()
    val label = Label(msgText)
    label.isWrapText = true
    label.styleClass.add("first-child")
    rootPane.add(label)
    val copyBtn = Button("Copy Link")
    copyBtn.addEventHandler(ActionEvent.ACTION) {
      Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
        putString(uri)
      })
    }
    rootPane.add(copyBtn, Pos.CENTER, null).apply {
      styleClass.add("smallskip")
    }
    rootPane.vbox.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
    pageSwitcher(rootPane.vbox)
  }

}
