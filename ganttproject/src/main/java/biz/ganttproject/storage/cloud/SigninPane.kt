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
import biz.ganttproject.app.DIALOG_STYLESHEET
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.Spinner
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.isBrowseSupported
import biz.ganttproject.lib.fx.openInBrowser
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
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
class SigninPane() : FlowPage() {
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

  private fun onStartCallback() {
    GlobalScope.launch(Dispatchers.JavaFx) {
      status = Status.WAITING_FOR_AUTH
      spinner.state = Spinner.State.ATTENTION

      statusText.value = ourLocalizer.formatText("text.browser_ready")
    }
  }

  fun createSigninPane(): Pane {
    val uri = "$GPCLOUD_SIGNIN_URL?callback=${controller.httpd.listeningPort}"

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


    vboxBuilder.vbox.let {
      it.stylesheets.addAll(
          DIALOG_STYLESHEET,
          "/biz/ganttproject/app/Util.css",
          "biz/ganttproject/storage/StorageDialog.css",
          "/biz/ganttproject/storage/cloud/GPCloudSignupPane.css"
      )
    }
    GlobalScope.launch(Dispatchers.IO) {
      if (isBrowseSupported()) {
        status = Status.WAITING_FOR_BROWSER
        openInBrowser(uri.trim())
        startBrowserTimeout(uri)
      } else {
        GlobalScope.launch(Dispatchers.JavaFx) {
          FXUtil.transitionCenterPane(indicatorPane, createUrlPane(uri)) {}
        }
      }
    }
    return vboxBuilder.vbox
  }

  private fun startBrowserTimeout(uri: String) {
    Timer().schedule(60000) {
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
  }

  override fun createUi(): Pane = createSigninPane()

  override fun resetUi() {}

  override fun setController(controller: GPCloudUiFlow) {
    this.controller = controller
    controller.httpd.onStart = ::onStartCallback
  }
}


private val ourLocalizer = RootLocalizer.createWithRootKey(
    rootKey = "cloud.signin",
    baseLocalizer = RootLocalizer.createWithRootKey("cloud.signup", RootLocalizer)
)
