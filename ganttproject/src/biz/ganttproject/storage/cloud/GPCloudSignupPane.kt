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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
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
  private val tokenVerificationUi: Pane by lazy { createTokenVerificationProgressUi() }

  init {
    this.httpd.onTokenReceived = this.onTokenCallback
  }


  fun createPane(msgIntro: String? = null): Pane {
    val vboxBuilder = VBoxBuilder()
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
      it.styleClass.add("medskip")
    }
    vboxBuilder.add(mdfx, Pos.CENTER, Priority.ALWAYS)


    val btnSignUp = Button(i18n.formatText("register"))
    btnSignUp.styleClass.add("btn-attention")
    btnSignUp.addEventHandler(ActionEvent.ACTION) {
      openInBrowser(GPCLOUD_SIGNUP_URL)
    }
    val btnSignIn = Button(i18n.formatText("generic.signIn")).also {
      it.addEventFilter(ActionEvent.ACTION) {
        this@GPCloudSignupPane.pageSwitcher(createSigninPane())
      }
      it.styleClass.addAll("btn-attention", "secondary")
    }


    GridPane().also { grid ->
      grid.add(Pane(), 0, 0)
      grid.add(btnSignUp, 1, 0)
      grid.add(btnSignIn, 2, 0)
      GridPane.setMargin(btnSignUp, Insets(0.0, 5.0, 0.0, 0.0))
      grid.columnConstraints.add(ColumnConstraints().apply {
        hgrow = Priority.ALWAYS
        isFillWidth = true
      })
      grid.styleClass.addAll("fill-parent", "btnbar")
      vboxBuilder.add(grid, Pos.CENTER_RIGHT, Priority.NEVER)
    }


    return paneAndImage(vboxBuilder.vbox)
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
    val vboxBuilder = VBoxBuilder()
    vboxBuilder.addTitle(i18nSignin.formatText("title"))
    vboxBuilder.add(Label().apply {
      this.textProperty().bind(i18nSignin.create("titleHelp"))
      this.styleClass.add("help")
    })

    val uri = "$GPCLOUD_SIGNIN_URL?callback=${httpd.listeningPort}"

    Label(i18nSignin.formatText("text.line1")).also {
      it.styleClass.addAll("helpline", "medskip")
      vboxBuilder.add(it)
    }
    Label(i18nSignin.formatText("text.line2")).also {
      it.styleClass.add("helpline")
      vboxBuilder.add(it)
    }
    HBox().also {
      val copyBtn = Button(i18nSignin.formatText("copyLink"), FontAwesomeIconView(FontAwesomeIcon.COPY)).also { btn ->
        btn.contentDisplay = ContentDisplay.RIGHT
        btn.styleClass.add("btn-secondary")
        btn.addEventHandler(ActionEvent.ACTION) {
          Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
            putString(uri)
          })
        }
      }
      val textField = TextField().apply {
        text = uri
        isEditable = false
        onMouseClicked = EventHandler { this.selectAll() }
        HBox.setHgrow(this, Priority.ALWAYS)
      }

      it.styleClass.addAll("smallskip", "row-copy-link")
      it.children.addAll(textField, copyBtn)
      HBox.setHgrow(textField, Priority.ALWAYS)
      HBox.setMargin(copyBtn, Insets(0.0, 0.0, 0.0, 5.0))
      vboxBuilder.add(it, Pos.CENTER_LEFT, Priority.NEVER)
    }

    return paneAndImage(vboxBuilder.vbox).also {
      it.stylesheets.addAll("/com/sandec/mdfx/mdfx.css")
      Platform.runLater {
        openInBrowser(uri.trim())
      }
    }
  }

  private fun createTokenVerificationProgressUi(): Pane {
    val i18nSignin = DefaultLocalizer("cloud.authPane", i18n)
    val vboxBuilder = VBoxBuilder("fill-parent")
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

    return paneAndImage(vboxBuilder.vbox)
  }

  private fun paneAndImage(centerNode: Node, imagePath: String = "/icons/ganttproject-logo-512.png"): Pane {
    return BorderPane().also {
      it.styleClass.addAll("dlg", "signup-pane")
      it.stylesheets.addAll(
          "/biz/ganttproject/app/Dialog.css",
          "/biz/ganttproject/app/Util.css",
          "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
          "/biz/ganttproject/storage/cloud/GPCloudSignupPane.css"
      )
      it.left = ImageView(Image(
          this.javaClass.getResourceAsStream(imagePath),
          64.0, 64.0, false, true))
      it.center = centerNode.also { node -> node.styleClass.add("signup-body") }
    }
  }
}
