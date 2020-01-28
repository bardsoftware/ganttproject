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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.animation.*
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
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
import javafx.scene.layout.HBox
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

      statusText.value = "Please sign into GanttProject Cloud from your browser"
      animationStopper = iconView.jump()
    }
  }

  fun createSigninPane(): Pane {
    val uri = "$GPCLOUD_SIGNIN_URL?callback=${httpd.listeningPort}"

    val i18nSignin = RootLocalizer.createWithRootKey("cloud.signin", i18n)
    val vboxBuilder = VBoxBuilder()
    vboxBuilder.addTitle(i18nSignin.formatText("title")).also {
      it.styleClass.add("title-integrated")
    }

    vboxBuilder.add(indicatorPane, Pos.CENTER, Priority.NEVER)
//    indicatorPane.center = ProgressIndicator(-1.0).also {
//      it.maxWidth = Double.MAX_VALUE
//      it.maxHeight = Double.MAX_VALUE
//    }
    indicatorPane.center = iconView
    animationStopper = iconView.rotate()
    statusText.value = "Opening sign in page in browser..."
    vboxBuilder.add(Label().also {
      it.textProperty().bind(statusText)
      it.isWrapText = true
    }, Pos.CENTER, Priority.NEVER)


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
        FXUtil.transitionCenterPane(indicatorPane, createUrlPane(uri), {})
      }
    }
  }

  private fun createUrlPane(uri: String): Node {
    return HBox().apply {
      styleClass.addAll("smallskip", "row-copy-link")
      children.add(
          Button(i18n.formatText("copyLink"), FontAwesomeIconView(FontAwesomeIcon.COPY)).apply {
            contentDisplay = ContentDisplay.RIGHT
            styleClass.add("btn-secondary")
            addEventHandler(ActionEvent.ACTION) {
              Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
                putString(uri)
              })
            }
            HBox.setMargin(this, Insets(0.0, 0.0, 0.0, 5.0))
          }
      )
      children.add(TextField().apply {
        text = uri
        isEditable = false
        onMouseClicked = EventHandler { this.selectAll() }
        HBox.setHgrow(this, Priority.ALWAYS)
      })
    }
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
  val jump = TranslateTransition(Duration.millis(100.0), this)
  jump.interpolatorProperty().set(Interpolator.SPLINE(.1, .1, .7, .7))
  jump.byY = -20.0
  jump.isAutoReverse = true
  jump.cycleCount = 10

  val pause = PauseTransition(Duration.seconds(2.0))
  val seq = SequentialTransition(jump, pause)
  seq.cycleCount = Animation.INDEFINITE
  seq.interpolator = Interpolator.LINEAR
  seq.play()
  return seq::stop
}

private val i18n = RootLocalizer.createWithRootKey("cloud.signup", RootLocalizer)
