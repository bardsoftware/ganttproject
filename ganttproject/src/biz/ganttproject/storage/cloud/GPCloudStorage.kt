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
import biz.ganttproject.storage.LockStatus
import biz.ganttproject.storage.LockableDocument
import biz.ganttproject.storage.StorageDialogBuilder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.hash.Hashing
import com.google.common.io.CharStreams
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.document.AbstractURLDocument
import net.sourceforge.ganttproject.document.Document
import org.apache.commons.codec.binary.Base64InputStream
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

const val GPCLOUD_HOST = "cumulus-dot-ganttproject-cloud.appspot.com"
//private const val GPCLOUD_HOST = "cloud.ganttproject.biz"
const val GPCLOUD_ORIGIN = "https://$GPCLOUD_HOST"
const val GPCLOUD_LANDING_URL = "https://$GPCLOUD_HOST"
private const val GPCLOUD_PROJECT_READ_URL = "$GPCLOUD_ORIGIN/p/read"
const val GPCLOUD_SIGNIN_URL = "https://$GPCLOUD_HOST/__/auth/desktop"
const val GPCLOUD_SIGNUP_URL = "https://$GPCLOUD_HOST/__/auth/handler"

typealias SceneChanger = (Node) -> Unit

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudStorage(
    private val mode: StorageDialogBuilder.Mode,
    private val openDocument: Consumer<Document>,
    private val dialogUi: StorageDialogBuilder.DialogUi) : StorageDialogBuilder.Ui {
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
    val browserPane = GPCloudBrowserPane(this.mode, this.dialogUi, this.openDocument, ::nextPage)
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

    val signupPane = GPCloudSignupPane(onTokenCallback, ::nextPage)
    val paneBuilder = VBoxBuilder("pane-service-contents")
    paneBuilder.addTitle("Signing in to GanttProject Cloud")
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
        Consumer { _ ->
          println("Auth token is valid!")
          webSocket.start()
          nextPage(browserPane.createStorageUi())
        },
        Consumer {
          println("Auth token is NOT valid!")
          Platform.runLater {
            signupPane.createPane().thenApply { pane ->
              nextPage(pane)
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

// HTTP server for sign in into GP Cloud
typealias AuthTokenCallback = (token: String?, validity: String?, userId: String?, websocketToken: String?) -> Unit

class GPCloudDocument(private val teamRefid: String?,
                      private val teamName: String,
                      private val projectRefid: String?,
                      private val projectName: String,
                      val projectJson: ProjectJsonAsFolderItem?)
  : AbstractURLDocument(), LockableDocument {
  override val status = SimpleObjectProperty<LockStatus>()
  init {
    status.set(if (projectJson?.isLocked == true) {
      LockStatus(true, projectJson.lockOwner, projectJson.lockOwnerEmail, projectJson.lockOwnerId)
    } else {
      LockStatus(false)
    })
  }


  constructor(projectJson: ProjectJsonAsFolderItem) : this(
      teamRefid = null,
      teamName = projectJson.node["team"].asText(),
      projectRefid = projectJson.node["refid"].asText(),
      projectName = projectJson.node["name"].asText(),
      projectJson = projectJson)


  constructor(team: TeamJsonAsFolderItem, projectName: String) : this(
      teamRefid = team.node["refid"].asText(),
      teamName = team.name,
      projectRefid = null,
      projectName = projectName,
      projectJson = null
  )

  override fun getFileName(): String {
    return this.projectName
  }

  override fun canRead(): Boolean = true

  override fun canWrite(): IStatus {
    return if (this.projectJson == null || !this.projectJson.isLocked || this.projectJson.canChangeLock) {
      Status.OK_STATUS
    } else {
      Status(IStatus.ERROR, Document.PLUGIN_ID, Document.ErrorCode.NOT_WRITABLE.ordinal, "", null)
    }
  }

  override fun isValidForMRU(): Boolean = true

  override fun getInputStream(): InputStream {
    val http = HttpClientBuilder.buildHttpClient()
    val projectRead = HttpGet("/p/read$queryArgs")
    val resp = http.client.execute(http.host, projectRead, http.context)
    if (resp.statusLine.statusCode == 200) {
      return Base64InputStream(resp.entity.content)
    } else {
      throw IOException("Failed to read from GanttProject Cloud: got response ${resp.statusLine}")
    }
  }

  override fun getOutputStream(): OutputStream {
    return object : ByteArrayOutputStream() {
      override fun close() {
        super.close()
        val http = HttpClientBuilder.buildHttpClient()
        val projectWrite = HttpPost("/p/write")
        val multipartBuilder = MultipartEntityBuilder.create()
        if (this@GPCloudDocument.projectRefid != null) {
          multipartBuilder.addPart("projectRefid", StringBody(
              this@GPCloudDocument.projectRefid, ContentType.TEXT_PLAIN))
        } else {
          multipartBuilder.addPart("teamRefid", StringBody(
              this@GPCloudDocument.teamRefid, ContentType.TEXT_PLAIN))
          multipartBuilder.addPart("filename", StringBody(
              this@GPCloudDocument.projectName, ContentType.TEXT_PLAIN))
        }
        multipartBuilder.addPart("fileContents", StringBody(
            Base64.getEncoder().encodeToString(this.toByteArray()), ContentType.TEXT_PLAIN))
        this@GPCloudDocument.lock?.get("lockToken")?.textValue()?.let {
          multipartBuilder.addPart("lockToken", StringBody(it, ContentType.TEXT_PLAIN))
        }
        projectWrite.entity = multipartBuilder.build()

        val resp = http.client.execute(http.host, projectWrite, http.context)
        if (resp.statusLine.statusCode != 200) {
          val body = CharStreams.toString(resp.entity.content.bufferedReader(Charsets.UTF_8))
          println(body)
          throw IOException("Failed to write to GanttProject Cloud. Got HTTP ${resp.statusLine.statusCode}: ${resp.statusLine.reasonPhrase}")
        }
      }
    }
  }

  override fun getPath(): String = """https://GanttProject Cloud/${this.teamName}/${this.projectName}?refid=${this.projectRefid}"""

  override fun write() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getURI(): URI = URI("""$GPCLOUD_PROJECT_READ_URL$queryArgs""")

  override fun isLocal(): Boolean = false
  fun listenLockChange(webSocket: WebSocketClient) {
    webSocket.onLockStatusChange { msg ->
      println(msg)
      if (!msg["locked"].booleanValue()) {
        this.status.set(LockStatus(false))
      } else {
        this.status.set(LockStatus(locked = true,
            lockOwnerName = msg.path("author")?.path("name")?.textValue(),
            lockOwnerEmail = null,
            lockOwnerId = msg.path("author")?.path("id")?.textValue()))
      }
    }
  }

  override fun toggleLocked() : CompletableFuture<LockStatus> {
    val result = CompletableFuture<LockStatus>()
    val lockService = LockService {
      result.completeExceptionally(RuntimeException(it))
    }
    lockService.project = this.projectJson!!
    lockService.busyIndicator = Consumer {}
    lockService.requestLockToken = false
    lockService.duration = Duration.ofHours(1)
    lockService.onSucceeded = EventHandler {
      val status = json2lockStatus(lockService.value)
      val projectNode = this.projectJson.node
      if (projectNode is ObjectNode) {
        projectNode.set("lock", lockService.value)
      }

      result.complete(status)
    }
    lockService.onFailed = EventHandler { result.completeExceptionally(RuntimeException("Failed")) }
    lockService.onCancelled = EventHandler { result.completeExceptionally(RuntimeException("Cancelled")) }
    lockService.restart()
    return result
  }

  private val queryArgs: String
    get() = "?projectRefid=${this.projectRefid}"

  var lock: JsonNode? = null
    set(value) {
      field = value
      this.status.set(json2lockStatus(value))
      value?.let {
        GPCloudOptions.cloudFiles.files[this.projectRefid!!] = GPCloudFileOptions(
            fingerprint = Hashing.farmHashFingerprint64().hashUnencodedChars(this.projectRefid!!).toString(),
            lockToken = it.get("lockToken")?.textValue() ?: "",
            lockExpiration = it.get("expirationEpochTs")?.textValue() ?: "",
            name = this.projectName
        )
        GPCloudOptions.cloudFiles.save()
      }
    }

  private fun json2lockStatus(json: JsonNode?): LockStatus {
    return if (json?.isMissingNode == false) {
      LockStatus(true,
          json["name"]?.textValue(), json["email"]?.textValue(), json["uid"]?.textValue())
    } else {
      LockStatus(false)
    }

  }

}
