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

import biz.ganttproject.storage.LockStatus
import biz.ganttproject.storage.LockableDocument
import biz.ganttproject.storage.NetworkUnavailableException
import biz.ganttproject.storage.OnlineDocument
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.base.Strings
import com.google.common.hash.Hashing
import com.google.common.io.ByteStreams
import com.google.common.io.CharStreams
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.document.AbstractURLDocument
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import org.apache.commons.codec.Charsets
import org.apache.commons.codec.binary.Base64InputStream
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import java.io.*
import java.net.SocketException
import java.net.URI
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

// HTTP server for sign in into GP Cloud
typealias AuthTokenCallback = (token: String?, validity: String?, userId: String?, websocketToken: String?) -> Unit

typealias OfflineDocumentFactory = (path: String) -> Document?

class GPCloudDocument(private val teamRefid: String?,
                      private val teamName: String,
                      private val projectRefid: String?,
                      private val projectName: String,
                      val projectJson: ProjectJsonAsFolderItem?)
  : AbstractURLDocument(), LockableDocument, OnlineDocument {

  private var lastKnownContents: ByteArray = ByteArray(0)
  override val isAvailableOffline = SimpleBooleanProperty()
  override val status = SimpleObjectProperty<LockStatus>()
  private val queryArgs: String
    get() = "?projectRefid=${this.projectRefid}"

  private val projectIdFingerprint = Hashing.farmHashFingerprint64().hashUnencodedChars(this.projectRefid!!).toString()
  var lock: JsonNode? = null
    set(value) {
      field = value
      this.status.set(json2lockStatus(value))
      value?.let {
        if (it["uid"]?.textValue() == GPCloudOptions.userId.value) {
          val options = GPCloudOptions.cloudFiles.files.getOrPut(this.projectRefid!!) {
            GPCloudFileOptions(
                fingerprint = this.projectIdFingerprint,
                name = this.projectName
            )
          }
          options.lockToken = it.get("lockToken")?.textValue() ?: ""
          options.lockExpiration = it.get("expirationEpochTs")?.longValue()?.toString() ?: ""
          GPCloudOptions.cloudFiles.save()
        }
      }
    }

  override var offlineMirror: Document? = null
    set(value) {
      val currentValue = field
      if (value == null && currentValue is FileDocument) {
        currentValue.delete()
        GPCloudOptions.cloudFiles.files[this.projectIdFingerprint!!]?.let {
          it.offlineMirror = null
          GPCloudOptions.cloudFiles.save()
        }
      }
      field = value
      value?.let {
        val options = GPCloudOptions.cloudFiles.files.getOrPut(this.projectIdFingerprint!!) {
          GPCloudFileOptions(
              fingerprint = this.projectIdFingerprint,
              name = this.projectName
          )
        }
        options.offlineMirror = it.filePath
        GPCloudOptions.cloudFiles.save()
      }
      this.isAvailableOffline.value = (field != null)
    }

  var offlineDocumentFactory: OfflineDocumentFactory = { null }
    set(value) {
      field = value
      this.initOfflineMirror()
    }

  init {
    status.set(if (projectJson?.isLocked == true) {
      LockStatus(true, projectJson.lockOwner, projectJson.lockOwnerEmail, projectJson.lockOwnerId)
    } else {
      LockStatus(false)
    })
    val fp = this.projectIdFingerprint
    val fileOptions = GPCloudOptions.cloudFiles.getFileOptions(fp)
    if (projectJson?.isLocked == true) {
      val lockToken = fileOptions.lockToken
      if (Strings.nullToEmpty(lockToken) != "") {
        (projectJson.node as ObjectNode).put("lockToken", lockToken)
      }
      this.lock = projectJson.node["lock"]
    }
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

  fun saveOfflineMirror(contents: ByteArray) {
    this@GPCloudDocument.offlineMirror?.let { document ->
      if (document is FileDocument) {
        document.create()
      }
      document.outputStream.use {
        ByteStreams.copy(ByteArrayInputStream(contents), it)
      }
    }
  }

  override fun getInputStream(): InputStream {
    val http = HttpClientBuilder.buildHttpClient()
    val projectRead = HttpGet("/p/read$queryArgs")
    val resp = http.client.execute(http.host, projectRead, http.context)
    if (resp.statusLine.statusCode == 200) {
      val encodedStream = Base64InputStream(resp.entity.content)
      val documentBody = ByteStreams.toByteArray(encodedStream)
      this@GPCloudDocument.lastKnownContents = documentBody
      GlobalScope.launch {
        saveOfflineMirror(documentBody)
      }
      return ByteArrayInputStream(documentBody)
    } else {
      throw IOException("Failed to read from GanttProject Cloud: got response ${resp.statusLine}")
    }
  }

  fun (Exception).isNetworkUnavailable(): Boolean {
    if (this is SocketException  && this.message?.contains("network is unreachable", true) != false) {
      return true
    }
    if (this is UnknownHostException) {
      return true
    }
    return false;
  }

  override fun getOutputStream(): OutputStream {
    return object : ByteArrayOutputStream() {
      override fun close() {
        super.close()
        val documentBody = this.toByteArray()
        this@GPCloudDocument.lastKnownContents = documentBody
        GlobalScope.launch {
          saveOfflineMirror(documentBody)
        }
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
            Base64.getEncoder().encodeToString(documentBody), ContentType.TEXT_PLAIN))
        this@GPCloudDocument.lock?.get("lockToken")?.textValue()?.let {
          multipartBuilder.addPart("lockToken", StringBody(it, ContentType.TEXT_PLAIN))
        }
        projectWrite.entity = multipartBuilder.build()

        try {
          val resp = http.client.execute(http.host, projectWrite, http.context)
          if (resp.statusLine.statusCode != 200) {
            val body = CharStreams.toString(resp.entity.content.bufferedReader(Charsets.UTF_8))
            println(body)
            throw IOException("Failed to write to GanttProject Cloud. Got HTTP ${resp.statusLine.statusCode}: ${resp.statusLine.reasonPhrase}")
          }
        } catch (ex: Exception) {
          when {
            ex.isNetworkUnavailable() -> {
              throw NetworkUnavailableException(ex)
            }
            else -> throw ex
          }
        }
      }
    }
  }

  override fun getPath(): String = """https://GanttProject Cloud/${this.teamName}/${this.projectName}?refid=${this.projectRefid}"""

  override fun write() {
    error("Not implemented")
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

  override fun toggleLocked(): CompletableFuture<LockStatus> {
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

  private fun json2lockStatus(json: JsonNode?): LockStatus {
    return if (json?.isMissingNode == false) {
      LockStatus(true,
          json["name"]?.textValue(), json["email"]?.textValue(), json["uid"]?.textValue())
    } else {
      LockStatus(false)
    }

  }

  override fun toggleAvailableOffline() {
    val mirror = this.offlineMirror
    when (mirror) {
      null -> {
        this.offlineMirror = this.offlineDocumentFactory(".CloudOfflineMirrors/${this.projectIdFingerprint}")
        GlobalScope.launch {
          saveOfflineMirror(this@GPCloudDocument.lastKnownContents)
        }
      }
      else -> {
        this.offlineMirror = null
      }
    }
  }

  private fun initOfflineMirror() {
    val fp = this.projectIdFingerprint
    val fileOptions = GPCloudOptions.cloudFiles.getFileOptions(fp)
    val offlineMirrorPath = fileOptions.offlineMirror
    if (offlineMirrorPath != null && Files.exists(Paths.get(offlineMirrorPath))) {
      this.offlineMirror = this.offlineDocumentFactory(offlineMirrorPath)
    }
  }
}
