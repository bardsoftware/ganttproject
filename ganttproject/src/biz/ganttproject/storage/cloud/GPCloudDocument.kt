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

import biz.ganttproject.storage.*
import com.evanlennick.retry4j.CallExecutorBuilder
import com.evanlennick.retry4j.config.RetryConfigBuilder
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
import org.apache.http.HttpResponse
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
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Consumer

// HTTP server for sign in into GP Cloud
typealias AuthTokenCallback = (token: String?, validity: String?, userId: String?, websocketToken: String?) -> Unit

typealias OfflineDocumentFactory = (path: String) -> Document?
typealias ProxyDocumentFactory = (document: Document) -> Document

class GPCloudDocument(private val teamRefid: String?,
                      private val teamName: String,
                      private val projectRefid: String?,
                      private val projectName: String,
                      val projectJson: ProjectJsonAsFolderItem?)
  : AbstractURLDocument(), LockableDocument, OnlineDocument {

  private var lastKnownContents: ByteArray = ByteArray(0)
  private var lastKnownVersion: String = ""
  private val retryExecutor = Executors.newSingleThreadExecutor()

  override val isMirrored = SimpleBooleanProperty()
  override val status = SimpleObjectProperty<LockStatus>()
  override val mode = SimpleObjectProperty<OnlineDocumentMode>(OnlineDocumentMode.ONLINE_ONLY)

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
        GPCloudOptions.cloudFiles.files[this.projectIdFingerprint]?.let {
          it.offlineMirror = null
          GPCloudOptions.cloudFiles.save()
        }
      }
      field = value
      value?.let {
        val options = GPCloudOptions.cloudFiles.files.getOrPut(this.projectIdFingerprint) {
          GPCloudFileOptions(
              fingerprint = this.projectIdFingerprint,
              name = this.projectName
          )
        }
        options.offlineMirror = it.filePath
        GPCloudOptions.cloudFiles.save()
      }
      this.isMirrored.value = (field != null)
    }

  private var modeValue: OnlineDocumentMode = this.mode.get()
    set(value) {
      if (this.mode.get() == OnlineDocumentMode.OFFLINE_ONLY && value == OnlineDocumentMode.MIRROR) {
        // This will throw exception if network is unavailable
        this.saveOnline(this.lastKnownContents)
      } else if (this.mode.get() == OnlineDocumentMode.ONLINE_ONLY && value == OnlineDocumentMode.OFFLINE_ONLY) {
        this.toggleMirrored()
      }
      if (value == OnlineDocumentMode.OFFLINE_ONLY) {
        this.startReconnectPing()
      }
      this.mode.set(value)
    }

  var offlineDocumentFactory: OfflineDocumentFactory = { null }
    set(value) {
      field = value
      this.initOfflineMirror()
    }

  var proxyDocumentFactory: ProxyDocumentFactory = { doc -> doc }

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
      projectJson = projectJson
  )


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

  private fun saveOfflineMirror(contents: ByteArray) {
    this.offlineMirror?.let { document ->
      if (document is FileDocument) {
        document.create()
      }
      document.outputStream.use {
        ByteStreams.copy(ByteArrayInputStream(contents), it)
      }
    }
  }

  private fun saveEtag(resp: HttpResponse) {
    val etagValue = resp.getFirstHeader("ETag")?.value ?: return
    println("ETag value: $etagValue")
    this.lastKnownVersion = etagValue
    if (this.isMirrored.get()) {
      GPCloudOptions.cloudFiles.files[this.projectIdFingerprint]?.let {
        it.lastWrittenVersion = etagValue
        GPCloudOptions.cloudFiles.save()
      } ?: println("No record ${this.projectIdFingerprint} in the options")
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
      this.saveEtag(resp)
      return ByteArrayInputStream(documentBody)
    } else {
      throw IOException("Failed to read from GanttProject Cloud: got response ${resp.statusLine}")
    }
  }

  private fun (Exception).isNetworkUnavailable(): Boolean {
    if (this is SocketException && this.message?.contains("network is unreachable", true) != false) {
      return true
    }
    if (this is UnknownHostException) {
      return true
    }
    return false
  }

  override fun getOutputStream(): OutputStream {
    return object : ByteArrayOutputStream() {
      override fun close() {
        super.close()
        val doc = this@GPCloudDocument
        val documentBody = this.toByteArray()
        doc.lastKnownContents = documentBody
        when (doc.modeValue) {
          OnlineDocumentMode.ONLINE_ONLY -> {
            saveOnline(documentBody)
          }
          OnlineDocumentMode.MIRROR -> {
            GlobalScope.launch {
              saveOfflineMirror(documentBody)
            }
            saveOnline(documentBody)
          }
          OnlineDocumentMode.OFFLINE_ONLY -> {
            saveOfflineMirror(documentBody)
          }
        }
      }
    }
  }

  @Throws(NetworkUnavailableException::class)
  private fun saveOnline(body: ByteArray) {
    val http = HttpClientBuilder.buildHttpClient()
    val projectWrite = HttpPost("/p/write")
    val multipartBuilder = MultipartEntityBuilder.create()
    if (this.projectRefid != null) {
      multipartBuilder.addPart("projectRefid", StringBody(
          this.projectRefid, ContentType.TEXT_PLAIN))
    } else {
      multipartBuilder.addPart("teamRefid", StringBody(
          this.teamRefid, ContentType.TEXT_PLAIN))
      multipartBuilder.addPart("filename", StringBody(
          this.projectName, ContentType.TEXT_PLAIN))
    }
    multipartBuilder.addPart("fileContents", StringBody(
        Base64.getEncoder().encodeToString(body), ContentType.TEXT_PLAIN))
    this.lock?.get("lockToken")?.textValue()?.let {
      multipartBuilder.addPart("lockToken", StringBody(it, ContentType.TEXT_PLAIN))
    }
    multipartBuilder.addPart("oldVersion", StringBody(this.lastKnownVersion, ContentType.TEXT_PLAIN))
    projectWrite.entity = multipartBuilder.build()

    try {
      val resp = http.client.execute(http.host, projectWrite, http.context)
      when (resp.statusLine.statusCode) {
        200 -> {
          this.saveEtag(resp)
        }
        412 -> {
          throw VersionMismatchException()
        }
        404 -> {
          if (!isNetworkAvailable()) {
            this.modeValue = OnlineDocumentMode.OFFLINE_ONLY
          } else {
            throw IOException("Got 404")
          }
        }
        else -> {
          val respBody = CharStreams.toString(resp.entity.content.bufferedReader(Charsets.UTF_8))
          println(respBody)
          throw IOException("Failed to write to GanttProject Cloud. Got HTTP ${resp.statusLine.statusCode}: ${resp.statusLine.reasonPhrase}")
        }
      }
    } catch (ex: Exception) {
      when {
        ex.isNetworkUnavailable() -> {
          this.modeValue = OnlineDocumentMode.OFFLINE_ONLY
        }
        else -> throw ex
      }
    }
  }

  override fun getPath(): String = """https://GanttProject Cloud/${this.teamName}/${this.projectName}?refid=${this.projectRefid}"""

  override fun write() {
    error("Not implemented")
  }

  override fun write(force: Boolean) {
    this.lastKnownVersion = ""
    this.proxyDocumentFactory(this).write()
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

  override fun toggleMirrored() {
    val mirror = this.offlineMirror
    when (mirror) {
      null -> {
        this.offlineMirror = this.offlineDocumentFactory(".CloudOfflineMirrors/${this.projectIdFingerprint}")
        GlobalScope.launch {
          saveOfflineMirror(this@GPCloudDocument.lastKnownContents)
        }
        this.modeValue = OnlineDocumentMode.MIRROR
      }
      else -> {
        this.offlineMirror = null
        this.modeValue = OnlineDocumentMode.ONLINE_ONLY
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

  private fun startReconnectPing() {
    val callable = Callable<Boolean> { isNetworkAvailable() }
    val retryConfig = RetryConfigBuilder()
        .retryOnReturnValue(false)
        .retryOnAnyException()
        .withMaxNumberOfTries(10).withRandomExponentialBackoff().withDelayBetweenTries(1, ChronoUnit.SECONDS).build()
    val executor = CallExecutorBuilder<Boolean>().config(retryConfig)
        .onSuccessListener {
          this.modeValue = OnlineDocumentMode.MIRROR
        }
        .beforeNextTryListener { println("Next try") }
        .buildAsync(this.retryExecutor)
    executor.execute(callable)
  }
}
