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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.base.Strings
import com.google.common.hash.Hashing
import com.google.common.io.ByteStreams
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.document.AbstractURLDocument
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
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

typealias OfflineDocumentFactory = (path: String) -> Document?
typealias ProxyDocumentFactory = (document: Document) -> Document

private val ourExecutor = Executors.newSingleThreadExecutor()

class GPCloudDocument(private val teamRefid: String?,
                      private val teamName: String,
                      internal var projectRefid: String?,
                      private val projectName: String,
                      val projectJson: ProjectJsonAsFolderItem?)
  : AbstractURLDocument(), LockableDocument, OnlineDocument {
  private var lastOfflineContents: ByteArray? = null

  override val isMirrored = SimpleBooleanProperty()
  override val status = SimpleObjectProperty<LockStatus>()
  override val mode = SimpleObjectProperty<OnlineDocumentMode>(OnlineDocumentMode.ONLINE_ONLY)
  override val fetchResultProperty = SimpleObjectProperty<FetchResult>()
  override val latestVersionProperty = SimpleObjectProperty<LatestVersion>(this, "")

  private val queryArgs: String
    get() = "?projectRefid=${this.projectRefid}"

  val projectIdFingerprint get() = this.projectRefid?.let {
    Hashing.farmHashFingerprint64().hashUnencodedChars(it).toString()
  } ?: ""

  var offlineDocumentFactory: OfflineDocumentFactory = { null }
  var proxyDocumentFactory: ProxyDocumentFactory = { doc -> doc }
  var httpClientFactory: () -> GPCloudHttpClient = HttpClientBuilder::buildHttpClient
  var isNetworkAvailable = Callable<Boolean> { isNetworkAvailable() }
  var executor = ourExecutor

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
        this.mirrorOptions?.let {
          it.clearOfflineMirror()
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
    }
    get() {
      return (field ?: {
        val fp = this.projectIdFingerprint
        val fileOptions = GPCloudOptions.cloudFiles.getFileOptions(fp)
        val offlineMirrorPath = fileOptions.offlineMirror
        if (offlineMirrorPath != null && Files.exists(Paths.get(offlineMirrorPath))) {
          this.offlineDocumentFactory(offlineMirrorPath)
        } else {
          null
        }
      }()).also {
        field = it
      }
    }

  private var modeValue: OnlineDocumentMode
    get() {
      return this.mode.get()
    }
    set(value) {
      if (value == this.mode.get()) {
        return
      }
      val change = this.mode.value to value
      when (change) {
        OnlineDocumentMode.ONLINE_ONLY to OnlineDocumentMode.OFFLINE_ONLY -> {
          this.startReconnectPing()
        }
        OnlineDocumentMode.MIRROR      to OnlineDocumentMode.OFFLINE_ONLY -> {
          this.lastOfflineContents?.let { this.saveOfflineBody(it) }
          this.startReconnectPing()
        }
        OnlineDocumentMode.OFFLINE_ONLY to OnlineDocumentMode.MIRROR -> {
          // This will throw exception if network is unavailable
          this.lastOfflineContents?.let {
            this.saveOnline(it)
          }
        }
        OnlineDocumentMode.ONLINE_ONLY to OnlineDocumentMode.MIRROR -> {
          this.offlineMirror = this.offlineMirror ?: this.offlineDocumentFactory(".CloudOfflineMirrors/${this.projectIdFingerprint}")
          this.fetchResultProperty.get()?.let {
            this.saveOfflineMirror(it)
          }
        }
        OnlineDocumentMode.MIRROR to OnlineDocumentMode.ONLINE_ONLY -> {
          this.offlineMirror = null
        }
      }
      this.mode.set(value)
    }

  val mirrorOptions: GPCloudFileOptions?
  get() {
    return GPCloudOptions.cloudFiles.files[this.projectIdFingerprint]
  }
  private fun makeMirrorOptions() = GPCloudOptions.cloudFiles.getFileOptions(this.projectIdFingerprint)

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

  override fun setMirrored(mirrored: Boolean) =
      if (mirrored) this.modeValue = OnlineDocumentMode.MIRROR else this.modeValue = OnlineDocumentMode.ONLINE_ONLY

  private fun saveOfflineMirror(fetch: FetchResult) {
    val document = this.offlineMirror
    if (document != null) {
      if (document is FileDocument) {
        document.create()
      }

      document.outputStream.use {
        ByteStreams.copy(ByteArrayInputStream(fetch.body), it)
      }
      this.makeMirrorOptions().let {
        it.lastOnlineVersion = fetch.actualVersion.toString()
        it.lastOnlineChecksum = fetch.actualChecksum
        GPCloudOptions.cloudFiles.save()
      }
      this.mode.set(OnlineDocumentMode.MIRROR)
    } else {
      this.mode.set(OnlineDocumentMode.ONLINE_ONLY)
    }
  }

  private fun saveOfflineBody(body: ByteArray) {
    this.offlineMirror?.let { document ->
      if (document is FileDocument) {
        document.create()
      }

      document.outputStream.use {
        ByteStreams.copy(ByteArrayInputStream(body), it)
      }
    }
  }

  override fun getInputStream(): InputStream {
    var fetchResult = this.fetchResultProperty.get() ?: runBlocking { fetch().also { it.update() }}
    if (fetchResult.useMirror) {
      val mirrorBytes = this.offlineMirror!!.inputStream.readBytes()
      saveOnline(mirrorBytes)
      return ByteArrayInputStream(mirrorBytes)
    } else {
      saveOfflineMirror(fetchResult)
      return ByteArrayInputStream(fetchResult.body)
    }
  }

  override suspend fun fetch(): FetchResult {
    return callReadProject()
  }

  override suspend fun fetchVersion(version: Long): FetchResult {
    return callReadProject(version)
  }

  @Throws(ForbiddenException::class)
  private fun callReadProject(version: Long = -1): FetchResult {
    val http = this.httpClientFactory()
    val resp = if (version == -1L) http.sendGet("/p/read$queryArgs") else http.sendGet("/p/read$queryArgs&generation=$version")
    when (resp.code) {
      200 -> {
        val etagValue = resp.header("ETag")
        val digestValue = resp.header("Digest")?.substringAfter("crc32c=")

        val documentBody = resp.decodedBody

        return FetchResult(
            this@GPCloudDocument,
            this.mirrorOptions?.lastOnlineChecksum ?: "",
            this.mirrorOptions?.lastOnlineVersion?.toLong() ?: -1L,
            digestValue ?: "",
            etagValue?.toLong() ?: -1,
            documentBody,
            fetchResultProperty::setValue
        )
      }
      403 -> {
        throw ForbiddenException()
      }
      else -> {
        throw IOException("Failed to read from GanttProject Cloud: got response ${resp.code} : ${resp.reason}")
      }
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
        doc.lastOfflineContents = documentBody
        when (doc.modeValue) {
          OnlineDocumentMode.ONLINE_ONLY -> {
            saveOnline(documentBody)
          }
          OnlineDocumentMode.MIRROR -> {
            saveOnline(documentBody)
          }
          OnlineDocumentMode.OFFLINE_ONLY -> {
            saveOfflineBody(documentBody)
          }
        }
      }
    }
  }

  private val OBJECT_MAPPER = ObjectMapper()

  @Throws(NetworkUnavailableException::class, VersionMismatchException::class, ForbiddenException::class)
  private fun saveOnline(body: ByteArray) {
    val http = this.httpClientFactory()
    try {
      val resp = http.sendPost("/p/write", mapOf(
          "projectRefid" to this.projectRefid.orEmpty(),
          "teamRefid" to this.teamRefid,
          "filename" to this.projectName,
          "fileContents" to Base64.getEncoder().encodeToString(body),
          "lockToken" to this.lock?.get("lockToken")?.textValue(),
          "oldVersion" to this.fetchResultProperty.get()?.actualVersion?.toString()
      ))
      when (resp.code) {
        200 -> {
          val etagValue = resp.header("ETag")
          val digestValue = resp.header("Digest")?.substringAfter("crc32c=")

          val response = OBJECT_MAPPER.readValue(resp.rawBody, ProjectWriteResponse::class.java)
          println(response)
          this.projectRefid = response.projectRefid
          val fetch = FetchResult(
              this@GPCloudDocument,
              this.mirrorOptions?.lastOnlineChecksum ?: "",
              this.mirrorOptions?.lastOnlineVersion?.toLong() ?: -1L,
              digestValue ?: "",
              etagValue?.toLong() ?: -1,
              body,
              fetchResultProperty::setValue
          )
          this.saveOfflineMirror(fetch)
          fetch.update()
        }
        403 -> {
          throw ForbiddenException()
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
          val respBody = resp.decodedBody.toString(com.google.common.base.Charsets.UTF_8)
          println(respBody)
          throw IOException("Failed to write to GanttProject Cloud. Got HTTP ${resp.code}: ${resp.reason}")
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
    this.fetchResultProperty.set(null)
    this.proxyDocumentFactory(this).write()
  }

  override fun getURI(): URI = URI("""$GPCLOUD_PROJECT_READ_URL$queryArgs""")

  override fun isLocal(): Boolean = false

  fun listenEvents(webSocket: WebSocketClient) {
    webSocket.apply {
      onLockStatusChange { msg ->
        if (!msg["locked"].booleanValue()) {
          status.set(LockStatus(false))
        } else {
          status.set(LockStatus(locked = true,
              lockOwnerName = msg.path("author")?.path("name")?.textValue(),
              lockOwnerEmail = null,
              lockOwnerId = msg.path("author")?.path("id")?.textValue()))
        }
      }

      onContentChange { msg -> GlobalScope.launch(Dispatchers.IO) { onWebSocketContentChange(msg) }}
    }
  }

  internal suspend fun onWebSocketContentChange(msg: ObjectNode) {
    if (msg["projectRefid"].textValue() == projectRefid) {
      val timestamp = msg["timestamp"]?.asLong() ?: return
      val author = msg["author"]?.get("name")?.textValue() ?: return
      fetch().also {
        if (it.actualVersion != fetchResultProperty.get().actualVersion) {
          latestVersionProperty.set(LatestVersion(timestamp, author))
        }
      }
    }
  }

  override fun toggleLocked(duration: Duration?): CompletableFuture<LockStatus> {
    val result = CompletableFuture<LockStatus>()
    val lockService = LockService {
      result.completeExceptionally(RuntimeException(it))
    }
    lockService.project = this.projectJson!!
    lockService.busyIndicator = Consumer {}
    lockService.requestLockToken = true
    lockService.duration = duration ?: Duration.ZERO
    lockService.onSucceeded = EventHandler {
      val status = json2lockStatus(lockService.value)
      val projectNode = this.projectJson.node
      if (projectNode is ObjectNode) {
        projectNode.set("lock", lockService.value)
      }
      this.lock = lockService.value
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
          json["name"]?.textValue(), json["email"]?.textValue(), json["uid"]?.textValue(), json)
    } else {
      LockStatus(false)
    }

  }

  private fun startReconnectPing() {
    val callable = this.isNetworkAvailable
    val retryConfig = RetryConfigBuilder()
        .retryOnReturnValue(false)
        .retryOnAnyException()
        .withMaxNumberOfTries(10).withRandomExponentialBackoff().withDelayBetweenTries(1, ChronoUnit.SECONDS).build()
    val executor = CallExecutorBuilder<Boolean>().config(retryConfig)
        .onSuccessListener {
          this.modeValue = OnlineDocumentMode.MIRROR
        }
        .beforeNextTryListener { println("Next try") }
        .buildAsync(executor)
    executor.execute(callable)
  }
}

