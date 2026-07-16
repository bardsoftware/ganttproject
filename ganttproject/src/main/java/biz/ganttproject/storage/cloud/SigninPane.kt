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
import biz.ganttproject.app.*
import biz.ganttproject.core.option.LabelPosition
import biz.ganttproject.core.option.ObservableString
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.isBrowseSupported
import biz.ganttproject.lib.fx.openInBrowser
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.geometry.HPos
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
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
class SigninPane() : FlowPage() {
  companion object {
    private const val SIGNIN_TIMEOUT_SECONDS = 60
  }

  enum class Status {
    INIT, WAITING_FOR_BROWSER, WAITING_FOR_AUTH, AUTH_COMPLETED
  }


  private lateinit var controller: GPCloudUiFlow
  private var status = Status.INIT
  private var statusText = SimpleStringProperty()
  private val spinner = Spinner()
  private val indicatorPane = BorderPane().apply {
    styleClass.add("indicator-pane")
  }
  val urlOption = ObservableString("foo1", "")
  val tokenOption = ObservableString("foo2", "")

  private fun onStartCallback() {
    FXThread.runLater {
      status = Status.WAITING_FOR_AUTH
      spinner.state = Spinner.State.ATTENTION

      statusText.value = ourLocalizer.formatText("text.browser_ready")
    }
  }

  fun createSigninPane(): Pane {
    val uri = "$GPCLOUD_SIGNIN_URL?callback=${controller.httpd.listeningPort}&timeout=$SIGNIN_TIMEOUT_SECONDS"

    val vboxBuilder = VBoxBuilder("signin-pane", "pane-service-contents")
    vboxBuilder.addTitle(ourLocalizer.formatText("title")).also {
      it.styleClass += "title-integrated"
    }
    vboxBuilder.add(Label().also {
      it.styleClass += "medskip"
      it.textProperty().bind(statusText)
      it.isWrapText = true
    }, Pos.CENTER_LEFT, Priority.NEVER)

    indicatorPane.center = spinner.pane
    vboxBuilder.add(indicatorPane, Pos.CENTER, Priority.NEVER)
    spinner.state = Spinner.State.WAITING
    statusText.value = ourLocalizer.formatText("text.browser_opening")

    val copyButton = Button(ourLocalizer.formatText("button.copyLink"), FontAwesomeIconView(FontAwesomeIcon.COPY)).apply {
      contentDisplay = ContentDisplay.RIGHT
      addEventHandler(ActionEvent.ACTION) {
        Clipboard.getSystemClipboard().setContent(ClipboardContent().apply {
          putString(uri)
        })
      }
    }

    urlOption.value = uri
    urlOption.isWritable.value = false
    val submitButton = Button(ourLocalizer.formatText("button.submitToken")).apply {
      addEventHandler(ActionEvent.ACTION) { submitToken(tokenOption.value ?: "") }
    }

    val tokenPane = properties(ourLocalizer) {
      text(urlOption) {
        labelText = ourLocalizer.formatText("text.browser_failed")
        labelPosition = LabelPosition.ABOVE
        labelHAlignment = HPos.LEFT
        rightNode = copyButton
      }
      text(tokenOption) {
        labelText = ourLocalizer.formatText("label.pasteToken")
        labelHAlignment = HPos.LEFT
        labelPosition = LabelPosition.ABOVE
        rightNode = submitButton
      }
    }
    vboxBuilder.add(tokenPane, Pos.CENTER, Priority.NEVER)


    vboxBuilder.vbox.let {
      it.stylesheets.addAll(
          DIALOG_STYLESHEET,
          "/biz/ganttproject/app/Util.css",
          "biz/ganttproject/storage/StorageDialog.css",
          "biz/ganttproject/storage/cloud/GPCloudSignupPane.css"
      )
    }
    FXThread.runLater {
      if (isBrowseSupported()) {
        status = Status.WAITING_FOR_BROWSER
        openInBrowser(uri.trim())
        startBrowserTimeout(uri)
      }
    }
    return vboxBuilder.vbox
  }

  private fun startBrowserTimeout(uri: String) {
    Timer().schedule(60000) {
      if (status == Status.WAITING_FOR_BROWSER) {
        FXThread.runLater {
          statusText.value = ourLocalizer.formatText("text.browser_failed")
        }
      }
    }
  }

  private fun submitToken(rawInput: String) {
    if (rawInput.isBlank()) return
    val params = rawInput.split("&").associate {
      val parts = it.split("=", limit = 2)
      if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
    }
    controller.httpd.onTokenReceived?.invoke(
        params["token"],
        params["validity"],
        params["userId"],
        params["websocketToken"]
    )
  }

  override fun createUi(): Pane = createSigninPane()

  override fun resetUi() {}

  override fun setController(controller: GPCloudUiFlow) {
    this.controller = controller
    controller.httpd.onStart = ::onStartCallback
    controller.httpd.onAuthReceived = {
      FXUtil.runLater {
        tokenOption.isWritable.value = false
      }
    }
  }
}


private val ourLocalizer = RootLocalizer.createWithRootKey(
    rootKey = "cloud.signin",
    baseLocalizer = RootLocalizer.createWithRootKey("cloud.signup", RootLocalizer)
)
