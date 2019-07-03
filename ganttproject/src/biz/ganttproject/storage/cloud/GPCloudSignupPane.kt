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

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.openInBrowser
import com.google.common.base.Strings
import com.sandec.mdfx.MDFXNode
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import org.apache.http.client.methods.HttpGet
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant
import java.util.function.Consumer
import java.util.logging.Level


/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudSignupPane(
    val onTokenCallback: AuthTokenCallback,
    val pageSwitcher: SceneChanger) {
  private val i18n = DefaultLocalizer("cloud.signup", RootLocalizer)

  private val httpd: HttpServerImpl by lazy {
    HttpServerImpl().apply { this.start() }
  }

  init {
    this.httpd.onTokenReceived = this.onTokenCallback
  }

  val tokenVerificationUi: Pane by lazy { createTokenVerificationProgressUi() }

  fun createPane(msgIntro: String? = null): Pane {

    val vboxBuilder = VBoxBuilder("dlg-lock")
    vboxBuilder.addTitle(i18n.formatText("title"))
    vboxBuilder.add(Label().apply {
      this.textProperty().bind(i18n.create("titleHelp"))
      this.styleClass.add("help")
    })
    if (!Strings.isNullOrEmpty(msgIntro)) {
      vboxBuilder.add(Label(msgIntro).apply {
        this.styleClass.add("intro")
      })
    }
    val mdfx = MDFXNode(i18n.create("body").value).also {
      it.styleClass.add("signup-body")
    }
    vboxBuilder.add(mdfx, Pos.CENTER, Priority.ALWAYS)


    val signupBtn = Button(i18n.formatText("generic.signUp"))
    signupBtn.styleClass.add("btn-attention")
    signupBtn.addEventHandler(ActionEvent.ACTION) {
      openInBrowser(GPCLOUD_SIGNUP_URL)
    }
    val btnSignIn = Button(i18n.formatText("generic.signIn")).also {
      it.addEventFilter(ActionEvent.ACTION) {
        this@GPCloudSignupPane.pageSwitcher(createSigninPane())
      }
    }

    val grid = GridPane()
    repeat(2) {
      ColumnConstraints().also { it.percentWidth = 50.0; grid.columnConstraints.add(it); }
    }
    grid.add(Label("Start using now!").also {
      GridPane.setMargin(it, Insets(0.0, 10.0, 5.0, 0.0))
      GridPane.setHalignment(it, HPos.RIGHT)
      it.styleClass.add("helpline")
    }, 0, 0)

    grid.add(signupBtn.also {
      GridPane.setMargin(it, Insets(0.0, 10.0, 5.0, 0.0))
      GridPane.setHalignment(it, HPos.RIGHT)
    }, 0, 1)

    grid.add(Label("Already registered?").also {
      GridPane.setMargin(it, Insets(0.0, 0.0, 5.0, 10.0))
      GridPane.setHalignment(it, HPos.LEFT)
      it.styleClass.add("helpline")
    }, 1, 0)
    grid.add(btnSignIn.also {
      GridPane.setMargin(it, Insets(0.0, 0.0, 5.0, 10.0))
      GridPane.setHalignment(it, HPos.LEFT)
    }, 1, 1)

    HBox().also {
      it.children.add(grid)
      HBox.setHgrow(grid, Priority.ALWAYS)
      vboxBuilder.add(it, Pos.CENTER, Priority.SOMETIMES)

    }

      //vboxBuilder.add(signupBtn, Pos.CENTER, null).also { it.styleClass.add("smallskip") }
//    vboxBuilder.add(btnSignIn, Pos.CENTER, Priority.NEVER).also {
//      it.styleClass.add("smallskip")
//    }

      return DialogPane().also {
        it.styleClass.addAll("dlg-lock", "signup-pane")
        it.stylesheets.addAll(
            "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
            "/biz/ganttproject/storage/StorageDialog.css"
        )
        it.graphic = ImageView(Image(
            this.javaClass.getResourceAsStream("/icons/ganttproject-logo-512.png"),
            64.0, 64.0, false, true))
        it.content = vboxBuilder.vbox
      }
    }


  fun tryAccessToken(success: Consumer<String>, unauthenticated: Consumer<String>) {
    if (Strings.isNullOrEmpty(GPCloudOptions.authToken.value)) {
      unauthenticated.accept("NO_ACCESS_TOKEN")
      return
    }
    if (Instant.ofEpochSecond(GPCloudOptions.validity.value.toLongOrNull() ?: 0).isBefore(Instant.now())) {
      unauthenticated.accept("ACCESS_TOKEN_EXPIRED")
      return
    }

    pageSwitcher(tokenVerificationUi)

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
    val http = HttpClientBuilder.buildHttpClientApache()
    val accessTokenCheck = HttpGet("/access-token/check")
    val resp = http.client.execute(http.host, accessTokenCheck, http.context)
    when (resp.statusLine.statusCode) {
      200 -> onSuccess.accept("")
      401 -> onUnauthenticated.accept("INVALID")
      else -> {
        onUnauthenticated.accept("INVALID")
      }
    }
  }

  fun createSigninPane(): Pane {
    val i18nSignin = DefaultLocalizer("cloud.signin", i18n)
    val vboxBuilder = VBoxBuilder("dlg-lock")
    vboxBuilder.addTitle(i18nSignin.formatText("title"))
    vboxBuilder.add(Label().apply {
      this.textProperty().bind(i18nSignin.create("titleHelp"))
      this.styleClass.add("help")
    })

    val uri = "$GPCLOUD_SIGNIN_URL?callback=${httpd.listeningPort}"
    val mdfx = object : MDFXNode(i18nSignin.formatText("body", uri)) {
      override fun setLink(node: Node, link: String, description: String) {
        node.cursor = Cursor.HAND
        node.setOnMouseClicked { openInBrowser(link.trim()) }
      }
    }
    mdfx.styleClass.add("signup-body")
    vboxBuilder.add(mdfx, Pos.CENTER, Priority.ALWAYS)

    vboxBuilder.add(TextField(uri).apply {
      isEditable = false
      onMouseClicked = EventHandler { this.selectAll() }

    }, Pos.CENTER, Priority.NEVER)

    val copyBtn = Button(i18nSignin.formatText("copyLink")).also {
      it.styleClass.add("btn-secondary")
    }
    copyBtn.addEventHandler(ActionEvent.ACTION) {
      Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
        putString(uri)
      })
    }
    vboxBuilder.add(copyBtn, Pos.CENTER, Priority.NEVER).also {
      it.styleClass.add("smallskip")
    }

    return DialogPane().also {
      it.styleClass.addAll("dlg-lock", "signup-pane")
      it.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      it.graphic = ImageView(Image(
          this.javaClass.getResourceAsStream("/icons/ganttproject-logo-512.png"),
          64.0, 64.0, false, true))
      it.content = vboxBuilder.vbox
    }
  }

  fun createTokenVerificationProgressUi(): Pane {
    val i18nSignin = DefaultLocalizer("cloud.authPane", i18n)
    val vboxBuilder = VBoxBuilder("dlg-lock")
    vboxBuilder.addTitle(i18nSignin.formatText("title"))

    val expirationValue = {
      val expirationInstant = Instant.ofEpochSecond(GPCloudOptions.validity.value.toLongOrNull() ?: 0)
      val remainingDuration = Duration.between(Instant.now(), expirationInstant)
      if (!remainingDuration.isNegative) {
        val hours = remainingDuration.toHours()
        val minutes = remainingDuration.minusMinutes(hours * 60).toMinutes()
        if (hours > 0) {
          i18nSignin.formatText("expirationValue_hm", hours, minutes)
        } else {
          i18nSignin.formatText("expirationValue_m", minutes)
        }
      } else ""
    }()

    vboxBuilder.add(Label(i18nSignin.formatText("expirationMsg", expirationValue)).apply {
      this.styleClass.add("help")
    })
    vboxBuilder.add(ProgressIndicator(-1.0).also {
      it.maxWidth = Double.MAX_VALUE
      it.maxHeight = Double.MAX_VALUE
    }, Pos.CENTER, Priority.ALWAYS)
    vboxBuilder.add(Label(i18nSignin.formatText("progressLabel")), Pos.CENTER, Priority.NEVER).also {
      it.styleClass.add("medskip")
    }

    return DialogPane().also {
      it.styleClass.addAll("dlg-lock", "signup-pane")
      it.stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      it.graphic = ImageView(Image(
          this.javaClass.getResourceAsStream("/icons/ganttproject-logo-512.png"),
          64.0, 64.0, false, true))
      it.content = vboxBuilder.vbox
    }
  }

//  val progressIndicator: Pane by lazy {
//    val paneBuilder = VBoxBuilder("pane-service-contents")
//    paneBuilder.addTitle("Signing into GanttProject Cloud")
//    if (GPCloudOptions.authToken.value != "") {
//      val expirationInstant = Instant.ofEpochSecond(GPCloudOptions.validity.value.toLongOrNull() ?: 0)
//      val remainingDuration = Duration.between(Instant.now(), expirationInstant)
//      if (!remainingDuration.isNegative) {
//        val hours = remainingDuration.toHours()
//        val minutes = remainingDuration.minusMinutes(hours * 60).toMinutes()
//        val expirationLabel = if (hours > 0) {
//          "${hours}h ${minutes}m"
//        } else {
//          "${minutes}m"
//        }
//        paneBuilder.add(Label("Your access token expires in $expirationLabel"), Pos.BASELINE_LEFT, Priority.NEVER)
//      }
//    }
//    paneBuilder.add(ProgressIndicator(-1.0), null, Priority.ALWAYS)
//    paneBuilder.vbox
//  }
}
