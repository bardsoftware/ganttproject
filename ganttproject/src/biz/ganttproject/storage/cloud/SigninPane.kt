/*
Copyright 2020 BarD Software s.r.o

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
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.openInBrowser
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.animation.*
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.util.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.schedule


/**
 * This is a user interface for signing into GanttProject Cloud.
 * Its main purpose is to open a signin page in a browser and wait for
 * /auth request from the browser. It shows a progress indicator white browser page is opening
 * and another indicator when it receives /start request from the browser.
 *
 * @author dbarashev@bardsoftware.com
 */
class SigninPane(private val onTokenCallback: AuthTokenCallback) {
  enum class Status {
    INIT, WAITING_FOR_BROWSER, WAITING_FOR_AUTH, AUTH_COMPLETED
  }

  private val httpd: HttpServerImpl by lazy {
    HttpServerImpl().apply { this.start() }
  }

  private var status = Status.INIT
  private var statusText = SimpleStringProperty()
  private val indicatorPane = BorderPane().apply {
    styleClass.add("indicator-pane")
  }
  private val iconView = ImageView(Image(
      GPCloudStorage::class.java.getResourceAsStream("/icons/ganttproject-logo-512.png"),
      128.0, 128.0, false, true))
  private var animationStopper: (()->Unit) ? = null

  init {
    this.httpd.onTokenReceived = this.onTokenCallback
    this.httpd.onStart = ::onStartCallback
  }

  fun onStartCallback() {
    GlobalScope.launch(Dispatchers.JavaFx) {
      status = Status.WAITING_FOR_AUTH
      animationStopper?.let {
        it.invoke()
      }

      statusText.value = ourLocalizer.formatText("text.browser_ready")
      animationStopper = iconView.jump()
    }
  }

  fun createSigninPane(): Pane {
    val uri = "$GPCLOUD_SIGNIN_URL?callback=${httpd.listeningPort}"

    val vboxBuilder = VBoxBuilder()
    vboxBuilder.addTitle(ourLocalizer.formatText("title")).also {
      it.styleClass += "title-integrated"
    }
    vboxBuilder.add(Label().also {
      it.styleClass += "medskip"
      it.textProperty().bind(statusText)
      it.isWrapText = true
    }, Pos.CENTER_LEFT, Priority.NEVER)

    vboxBuilder.add(indicatorPane, Pos.CENTER, Priority.NEVER)
    indicatorPane.center = iconView
    animationStopper = iconView.rotate()
    statusText.value = ourLocalizer.formatText("text.browser_opening")


    vboxBuilder.vbox.let {
      it.styleClass.addAll("signin-pane", "pane-service-contents")
      it.stylesheets.addAll(
          "/biz/ganttproject/app/Dialog.css",
          "/biz/ganttproject/app/Util.css",
          "biz/ganttproject/storage/StorageDialog.css",
          "/biz/ganttproject/storage/cloud/GPCloudSignupPane.css"
      )
    }
    GlobalScope.launch(Dispatchers.IO) {
      status = Status.WAITING_FOR_BROWSER
      openInBrowser(uri.trim())
      startBrowserTimeout(uri)
    }
    return vboxBuilder.vbox
  }

  private fun startBrowserTimeout(uri: String) {
    Timer().schedule(10000) {
      if (status == Status.WAITING_FOR_BROWSER) {
        GlobalScope.launch(Dispatchers.JavaFx) {
          FXUtil.transitionCenterPane(indicatorPane, createUrlPane(uri), {})
          statusText.value = ourLocalizer.formatText("text.browser_failed")
        }
      }
    }
  }

  private fun createUrlPane(uri: String): Node {
    return vbox {
      i18n = ourLocalizer
      vbox.spacing = 5.0
      add(TextField().apply {
        text = uri
        isEditable = false
        onMouseClicked = EventHandler { this.selectAll() }
      }, Pos.CENTER, Priority.NEVER)
      add(Button(i18n.formatText("button.copyLink"), FontAwesomeIconView(FontAwesomeIcon.COPY)).apply {
        contentDisplay = ContentDisplay.RIGHT
        styleClass.addAll("btn-attention")
        addEventHandler(ActionEvent.ACTION) {
          Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
            putString(uri)
          })
        }
      }, Pos.CENTER, Priority.NEVER)
    }
//    return HBox().apply {
//      styleClass.addAll("smallskip", "row-copy-link")
//      children.add(
//          Button(i18n.formatText("copyLink"), FontAwesomeIconView(FontAwesomeIcon.COPY)).apply {
//            contentDisplay = ContentDisplay.RIGHT
//            styleClass.add("btn-secondary")
//            addEventHandler(ActionEvent.ACTION) {
//              Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
//                putString(uri)
//              })
//            }
//            HBox.setMargin(this, Insets(0.0, 0.0, 0.0, 5.0))
//          }
//      )
//      children.add()
//    }
  }
}

private fun (ImageView).rotate() : ()->Unit {
  val rt = RotateTransition(Duration.millis(3000.0), this)
  rt.fromAngle = 0.0
  rt.toAngle = 360.0
  rt.cycleCount = Animation.INDEFINITE
  rt.interpolator = Interpolator.LINEAR
  rt.play()

  return {
    rt.cycleCount = 1
    rt.duration = Duration.millis(1000.0)
    rt.playFromStart()
  }
}

private fun (ImageView).jump() : ()->Unit {
  val longJump = TranslateTransition(Duration.millis(200.0), this)
  longJump.interpolatorProperty().set(Interpolator.SPLINE(.1, .1, .7, .7))
  longJump.byY = -30.0
  longJump.isAutoReverse = true
  longJump.cycleCount = 2

  val shortJump = TranslateTransition(Duration.millis(100.0), this)
  shortJump.interpolatorProperty().set(Interpolator.SPLINE(.1, .1, .7, .7))
  shortJump.byY = -15.0
  shortJump.isAutoReverse = true
  shortJump.cycleCount = 6

  val pause = PauseTransition(Duration.seconds(2.0))
  val seq = SequentialTransition(longJump, shortJump, pause)
  seq.cycleCount = Animation.INDEFINITE
  seq.interpolator = Interpolator.LINEAR
  seq.play()
  return seq::stop
}

private val ourLocalizer = RootLocalizer.createWithRootKey(
    rootKey = "cloud.signin",
    baseLocalizer = RootLocalizer.createWithRootKey("cloud.signup", RootLocalizer)
)
