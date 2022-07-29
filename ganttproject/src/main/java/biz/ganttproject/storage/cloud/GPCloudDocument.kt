/*
Copyright 2018-2020 BarD Software s.r.o

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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.storage.*
import biz.ganttproject.storage.cloud.http.IsLockedService
import biz.ganttproject.storage.cloud.http.LockService
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.base.Strings
import com.google.common.hash.Hashing
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.AbstractURLDocument
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.FileDocument
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import java.io.*
import java.net.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.function.Consumer

typealias OfflineDocumentFactory = (path: String) -> Document?
typealias ProxyDocumentFactory = (document: Document) -> Document

const val URL_SCHEME = "cloud:"

/**
 * This class represents a document stored on GanttProject Cloud.
 */
class GPCloudDocument(val teamRefid: String?,
                      private val teamName: String,
                      internal var projectRefid: String?,
                      private val projectName: String,
                      projectJson: ProjectJsonAsFolderItem?)
  : AbstractURLDocument(), LockableDocument, OnlineDocument {

  override val isMirrored = SimpleBooleanProperty()
  override val status = SimpleObjectProperty<LockStatus>()
  override val mode = SimpleObjectProperty(OnlineDocumentMode.ONLINE_ONLY)
  override val fetchResultProperty = SimpleObjectProperty<FetchResult>()
  override val latestVersionProperty = SimpleObjectProperty<LatestVersion>(this, "")
  override val id: String
    get() = this.projectRefid ?: ""

  private val queryArgs: String
    get() = "?projectRefid=${this.projectRefid}"

  internal val projectIdFingerprint get() = this.projectRefid?.let {
    Hashing.farmHashFingerprint64().hashUnencodedChars(it).toString()
  } ?: ""

  private var lastOfflineContents: ByteArray? = null
  internal var offlineDocumentFactory: OfflineDocumentFactory = { null }
  internal var proxyDocumentFactory: ProxyDocumentFactory = { doc -> doc }
  internal var httpClientFactory: () -> GPCloudHttpClient = { HttpClientBuilder.buildHttpClient() }
  internal var isNetworkAvailable = Callable { isNetworkAvailable() }
  internal var executor = ourExecutor

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
        val onlineOnly = fileOptions.onlineOnly.toBoolean()
        val offlineMirrorPath = fileOptions.offlineMirror
        if (!onlineOnly && offlineMirrorPath != null && Files.exists(Paths.get(offlineMirrorPath))) {
          this.offlineDocumentFactory(offlineMirrorPath)
        } else {
          null
        }
      }()).also {
        field = it
      }
    }

  internal var modeValue: OnlineDocumentMode
    get() {
      return this.mode.get()
    }
    set(value) {
      if (value == this.mode.get()) {
        return
      }
      when (this.mode.value to value) {
        OnlineDocumentMode.ONLINE_ONLY to OnlineDocumentMode.OFFLINE_ONLY -> {
        }
        OnlineDocumentMode.MIRROR      to OnlineDocumentMode.OFFLINE_ONLY -> {
          this.lastOfflineContents?.let { this.saveOfflineBody(it) }
        }
        OnlineDocumentMode.OFFLINE_ONLY to OnlineDocumentMode.MIRROR -> {
          // This will throw exception if network is unavailable
          this.lastOfflineContents?.let {
            this.saveOnline(it)
          }
        }
        OnlineDocumentMode.ONLINE_ONLY to OnlineDocumentMode.MIRROR -> {
          if (this.projectRefid != null) {
            this.offlineMirror = this.offlineMirror
                ?: this.offlineDocumentFactory(".CloudOfflineMirrors/${this.projectIdFingerprint}")
            this.fetchResultProperty.get()?.let {
              this.saveOfflineMirror(it)
            }
          }
        }
        OnlineDocumentMode.OFFLINE_ONLY to OnlineDocumentMode.ONLINE_ONLY -> {
          this.offlineMirror = null
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

  var colloboqueClient: ColloboqueClient? = null
  set(value) {
    field = value
    if (value != null) {
      fetchResultProperty.get()?.baseColloboqueTxnId?.let {
        value.start(projectRefid!!, it)
      }
    }
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

  constructor(projectJson: ProjectJsonAsFolderItem, teamRefid: String?) : this(
      teamRefid = teamRefid,
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
    val lockStatus = this.status.get()
    return if (lockStatus == null || !lockStatus.locked || !lockStatus.lockedBySomeone) {
      Status.OK_STATUS
    } else {
      Status(IStatus.ERROR, Document.PLUGIN_ID,
          Document.ErrorCode.NOT_WRITABLE.ordinal,
          RootLocalizer.createWithRootKey("cloud.statusBar").formatText("lockedBy", lockStatus.lockOwnerName ?: ""),
          null)
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
        it.write(fetch.body)
      }
      this.makeMirrorOptions().let {
        it.name = fileName
        it.teamName = teamName
        it.lastOnlineVersion = fetch.actualVersion.toString()
        it.lastOnlineChecksum = fetch.actualChecksum
        it.projectRefid = this.projectRefid!!
        it.onlineOnly = ""
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
        it.write(body)
      }
    }
  }

  override fun getInputStream(): InputStream {
    val fetchResult = this.fetchResultProperty.get() ?: runBlocking { fetch().also { it.update() }}
    return if (fetchResult.useMirror) {
      val mirrorBytes = this.offlineMirror!!.inputStream.readBytes()
      saveOnline(mirrorBytes)
      ByteArrayInputStream(mirrorBytes)
    } else {
      saveOfflineMirror(fetchResult)
      ByteArrayInputStream(fetchResult.body)
    }
  }

  override suspend fun fetch(): FetchResult {
    return callReadProject()
  }

  @Throws(ForbiddenException::class, PaymentRequiredException::class, ForbiddenException::class)
  override suspend fun fetchVersion(version: Long): FetchResult {
    return callReadProject(version)
  }


  /**
   * Sends an HTTP request to GP Cloud server to read the project contents.
   */
  @Throws(ForbiddenException::class, PaymentRequiredException::class, ForbiddenException::class)
  private fun callReadProject(version: Long = -1): FetchResult {
    LOG.debug("Calling /p/read")
    val http = this.httpClientFactory()
    try {
      val resp = if (version == -1L) http.sendGet("/p/read$queryArgs") else http.sendGet("/p/read$queryArgs&generation=$version")
      LOG.debug("Received HTTP {}", resp.code)
      when (resp.code) {
        200 -> {
          val etagValue = resp.header("ETag")
          val digestValue = resp.header("Digest")?.substringAfter("crc32c=")
          val colloboqueBaseTxnId = resp.header("BaseTxnId")
          if (colloboqueBaseTxnId != null) {
            colloboqueClient?.run {
               start(projectRefid!!, colloboqueBaseTxnId)
            }
          }
          val documentBody = resp.decodedBody

          //GlobalScope.launch { println(loadTeamResources(this@GPCloudDocument)) }
          return FetchResult(
              this@GPCloudDocument,
              this.mirrorOptions?.lastOnlineChecksum ?: "",
              this.mirrorOptions?.lastOnlineVersion?.toLong() ?: -1L,
              digestValue ?: "",
              etagValue?.toLong() ?: -1,
              colloboqueBaseTxnId,
              documentBody,
              fetchResultProperty::setValue
          )
        }
        402 -> throw PaymentRequiredException(resp.rawBody.toString(Charsets.UTF_8).trim())
        401, 403 -> {
          throw ForbiddenException()
        }
        else -> {
          throw IOException("Failed to read from GanttProject Cloud: got response ${resp.code} : ${resp.reason}")
        }
      }
    } catch (ex: Exception) {
      when {
        ex.isNetworkUnavailable() -> {
          LOG.error("We seem to be offline", kv = mapOf("when" to "/p/read"), exception = ex)
          this.offlineMirror?.let { _ ->
            this.modeValue = OnlineDocumentMode.OFFLINE_ONLY
            return FetchResult(
                onlineDocument = this@GPCloudDocument,
                syncChecksum = this.mirrorOptions?.lastOnlineChecksum ?: "",
                syncVersion = this.mirrorOptions?.lastOnlineVersion?.toLong() ?: -1L,
                // As if we just've synced this mirror with the cloud
                actualChecksum = this.mirrorOptions?.lastOnlineChecksum ?: "",
                actualVersion = this.mirrorOptions?.lastOnlineVersion?.toLong() ?: -1L,
                baseColloboqueTxnId = null,
                body = ByteArray(0),
                updateFxn = fetchResultProperty::setValue
            ).also { it.useMirror = true }
          }
        }
        else -> {}
      }
      throw ex
    }
  }

  private fun (Exception).isNetworkUnavailable(): Boolean {
    if (this is SocketException && this.message?.contains("network is unreachable", true) != false) {
      return true
    }
    if (this is SocketTimeoutException) {
      return true
    }
    if (this is ConnectException) {
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

  private val OBJECT_MAPPER = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  @Throws(NetworkUnavailableException::class, VersionMismatchException::class, ForbiddenException::class)
  private fun saveOnline(body: ByteArray) {
    LOG.debug("Calling /p/write")
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
      LOG.debug("Received HTTP {}", resp.code)
      when (resp.code) {
        200 -> {
          val etagValue = resp.header("ETag")
          val digestValue = resp.header("Digest")?.substringAfter("crc32c=")
          val colloboqueBaseTxnId = resp.header("BaseTxnId")
          val response = OBJECT_MAPPER.readValue(resp.rawBody, ProjectWriteResponse::class.java)
          this.projectRefid = response.projectRefid
          val fetch = FetchResult(
              this@GPCloudDocument,
              this.mirrorOptions?.lastOnlineChecksum ?: "",
              this.mirrorOptions?.lastOnlineVersion?.toLong() ?: -1L,
              digestValue ?: "",
              etagValue?.toLong() ?: -1,
              colloboqueBaseTxnId,
              body,
              fetchResultProperty::setValue
          )
          this.saveOfflineMirror(fetch)
          fetch.update()
        }
        401, 403 -> throw ForbiddenException()
        409 -> throw VersionMismatchException(canOverwrite = false)
        402 -> throw PaymentRequiredException(resp.rawBody.toString(Charsets.UTF_8).trim())
        412 -> throw VersionMismatchException()
        404 -> {
          if (!isNetworkAvailable()) {
            this.modeValue = OnlineDocumentMode.OFFLINE_ONLY
          } else {
            throw IOException("Got 404")
          }
        }
        else -> {
          throw IOException("Failed to write to GanttProject Cloud. Got HTTP ${resp.code}: ${resp.reason}")
        }
      }
    } catch (ex: Exception) {
      when {
        ex.isNetworkUnavailable() -> {
          LOG.error("We seem to be offline", ex)
          this.modeValue = OnlineDocumentMode.OFFLINE_ONLY
        }
        else -> throw ex
      }
    }
  }

  override fun getPath(): String = """cloud://${this.projectRefid}/${this.teamName}/${this.projectName}"""

  override fun write() {
    error("Not implemented")
  }

  override fun write(force: Boolean) {
    this.fetchResultProperty.set(null)
    this.proxyDocumentFactory(this).write()
  }

  override fun getURI(): URI = URI("""$GPCLOUD_PROJECT_READ_URL$queryArgs""")

  override fun isLocal(): Boolean = false

  private val websocketCleaners = mutableListOf<()->Unit>()
  fun attachWebsocket(webSocket: WebSocketClient) {
    websocketCleaners.add(webSocket.onLockStatusChange { msg ->
      if (msg["projectRefid"].textValue() == this.projectRefid) {
        if (!msg["locked"].booleanValue()) {
          status.set(LockStatus(false))
        } else {
          status.set(
            LockStatus(
              locked = true,
              lockOwnerName = msg.path("author")?.path("name")?.textValue(),
              lockOwnerEmail = null,
              lockOwnerId = msg.path("author")?.path("id")?.textValue()
            )
          )
        }
      }
    })

    websocketCleaners.add(webSocket.onContentChange { msg -> GlobalScope.launch(Dispatchers.IO) { onWebSocketContentChange(msg) }})
    colloboqueClient?.let {
      it.attach(webSocket)
    }
  }

  fun detachWebsocket(webSocket: WebSocketClient) {
    this.websocketCleaners.forEach { it() }
    this.websocketCleaners.clear()
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
    val lockService = LockService(this.projectRefid!!, this.status.get().locked) {
      result.completeExceptionally(RuntimeException(it))
    }
    lockService.busyIndicator = Consumer {}
    lockService.requestLockToken = true
    lockService.duration = duration ?: Duration.ZERO
    lockService.onSucceeded = EventHandler {
      val status = json2lockStatus(lockService.value)
      this.lock = lockService.value
      result.complete(status)
    }
    lockService.onFailed = EventHandler { result.completeExceptionally(RuntimeException("Failed")) }
    lockService.onCancelled = EventHandler { result.completeExceptionally(RuntimeException("Cancelled")) }
    lockService.restart()
    return result
  }

  override fun reloadLockStatus(): CompletableFuture<LockStatus> {
    val result = CompletableFuture<LockStatus>()
    this.projectRefid?.let {
      val service = IsLockedService(
          errorUi = {errorMsg -> result.completeExceptionally(RuntimeException(errorMsg)) },
          busyIndicator = {},
          projectRefid = this.projectRefid!!
      ) {value ->
        this.lock = value
        result.complete(this.status.get())
      }
      service.restart()
    } ?: result.complete(LockStatus(locked = false))
    return result
  }

  private fun json2lockStatus(json: JsonNode?): LockStatus {
    return if (json?.isMissingNode == false && json["expirationEpochTs"]?.asLong() != -1L) {
      LockStatus(true,
          json["name"]?.textValue(), json["email"]?.textValue(), json["uid"]?.textValue(), json)
    } else {
      LockStatus(false)
    }
  }
}

fun GPCloudDocument.onboard(documentManager: DocumentManager, webSocket: WebSocketClient) {
  this.offlineDocumentFactory = { path -> documentManager.newDocument(path) }
  this.proxyDocumentFactory = documentManager::getProxyDocument
  webSocket.register(this)
  this.projectRefid?.let { webSocket.sendProjectRefId(it) }

  if (GPCloudOptions.defaultOfflineMode.value && !GPCloudOptions.cloudFiles.getFileOptions(this.projectIdFingerprint).onlineOnly.toBoolean()) {
    this.modeValue = OnlineDocumentMode.MIRROR
  }
}

private val ourExecutor = Executors.newSingleThreadExecutor()
private val LOG = GPLogger.create("Cloud.Document")

