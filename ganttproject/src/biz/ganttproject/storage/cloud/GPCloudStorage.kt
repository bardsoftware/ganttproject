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
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.StorageDialogBuilder
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

const val GPCLOUD_HOST = "cumulus-dot-ganttproject-cloud.appspot.com"
const val GPCLOUD_IP = "216.239.32.21"
//private const val GPCLOUD_HOST = "cloud.ganttproject.biz"
const val GPCLOUD_ORIGIN = "https://$GPCLOUD_HOST"
const val GPCLOUD_LANDING_URL = "https://$GPCLOUD_HOST"
const val GPCLOUD_PROJECT_READ_URL = "$GPCLOUD_ORIGIN/p/read"
const val GPCLOUD_SIGNIN_URL = "https://$GPCLOUD_HOST/__/auth/desktop"
const val GPCLOUD_SIGNUP_URL = "https://$GPCLOUD_HOST/__/auth/handler"

typealias SceneChanger = (Node) -> Unit

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudStorage(
    private val mode: StorageDialogBuilder.Mode,
    private val openDocument: Consumer<Document>,
    private val dialogUi: StorageDialogBuilder.DialogUi,
    private val documentManager: DocumentManager) : StorageDialogBuilder.Ui {
  private val myPane: BorderPane = BorderPane()

  internal interface PageUi {
    fun createPane(): CompletableFuture<Pane>
  }

  override fun getName(): String {
    return "GanttProject Cloud"
  }

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }

  override fun getCategory(): String {
    return "cloud"
  }

  override fun createUi(): Pane {
    return doCreateUi()
  }

  private fun doCreateUi(): Pane {
    val browserPane = GPCloudBrowserPane(this.mode, this.dialogUi, this.openDocument, this.documentManager, ::nextPage)
    val onTokenCallback: AuthTokenCallback = { token, validity, userId, websocketToken ->
      val validityAsLong = validity?.toLongOrNull()
      with(GPCloudOptions) {
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
      Platform.runLater {
        nextPage(browserPane.createStorageUi())
      }
    }

    val offlinePane = GPCloudOfflinePane()
    val signupPane = GPCloudSignupPane(onTokenCallback, ::nextPage)
    val paneBuilder = VBoxBuilder("pane-service-contents")
    paneBuilder.addTitle("Signing into GanttProject Cloud")
    if (GPCloudOptions.authToken.value != "") {
      val expirationInstant = Instant.ofEpochSecond(GPCloudOptions.validity.value.toLong())
      val remainingDuration = Duration.between(Instant.now(), expirationInstant)
      if (!remainingDuration.isNegative) {
        val hours = remainingDuration.toHours()
        val minutes = remainingDuration.minusMinutes(hours * 60).toMinutes()
        val expirationLabel = if (hours > 0) {
          "${hours}h ${minutes}m"
        } else {
          "${minutes}m"
        }
        paneBuilder.add(Label("Your access token expires in $expirationLabel"), Pos.BASELINE_LEFT, Priority.NEVER)
      }
    }
    paneBuilder.add(ProgressIndicator(-1.0), null, Priority.ALWAYS)
    nextPage(paneBuilder.vbox)

    signupPane.tryAccessToken(
        success = Consumer {
          println("Auth token is valid!")
          webSocket.start()
          nextPage(browserPane.createStorageUi())
        },
        unauthenticated = Consumer {
          when (it) {
            "INVALID" -> {
              println("Auth token is NOT valid!")
              Platform.runLater {
                signupPane.createPane().thenApply { pane ->
                  nextPage(pane)
                }
              }
            }
            "OFFLINE" -> {
              nextPage(offlinePane.createPane())
            }
            else -> {
            }
          }
        }
    )
    return myPane
  }

  private fun nextPage(newPage: Node) {
    Platform.runLater {
      FXUtil.transitionCenterPane(myPane, newPage) { dialogUi.resize() }
    }
  }
}

class GPCloudOfflinePane {
  fun createPane(): Pane {
    val rootPane = VBoxBuilder("pane-service-contents", "cloud-storage")
    rootPane.add(Pane(), Pos.CENTER, Priority.ALWAYS)
    rootPane.add(buildContentPane(), Pos.CENTER, Priority.NEVER)
    rootPane.add(Pane(), Pos.CENTER, Priority.ALWAYS)
    return rootPane.vbox
  }

  private fun buildContentPane(): Pane {
    val vbox = VBoxBuilder("content-pane")
    vbox.addTitle("You seem to be offline")
    vbox.add(Label("We couldn't contact $GPCLOUD_HOST by name nor by IP address").apply { styleClass.add("help") })
    return DialogPane().apply {
      styleClass.add("dlg-lock")
      stylesheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      graphic = FontAwesomeIconView(FontAwesomeIcon.SIGNAL)

      content = vbox.vbox

      buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
      lookupButton(ButtonType.OK).apply {
        if (this is Button) {
          text = "Use offline mirrors"
        }
      }
      lookupButton(ButtonType.CANCEL).apply {
        if (this is Button) {
          text = "Try again"
        }
      }

    }
  }
}
