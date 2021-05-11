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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.storage.DocumentUri
import biz.ganttproject.storage.Path
import biz.ganttproject.storage.cloud.http.JsonTask
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import fi.iki.elonen.NanoHTTPD
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Service
import javafx.concurrent.Task
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import okhttp3.*
import org.apache.commons.codec.binary.Base64InputStream
import org.apache.http.HttpHost
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URIBuilder
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.Random

class GPCloudException(val status: Int) : Exception()
/**
 * Background tasks which communicate with GP Cloud server and load
 * user team and project list.
 *
 * @author dbarashev@bardsoftware.com
 */

// Create LoadTask or CachedTask depending on whether we have cached response from GP Cloud or not
class LoaderService<T : CloudJsonAsFolderItem> : Service<ObservableList<T>>() {
  var busyIndicator: Consumer<Boolean> = Consumer {}
  var path: Path = DocumentUri(listOf(), true, RootLocalizer.formatText("cloud.officialTitle"))
  var jsonResult: SimpleObjectProperty<JsonNode> = SimpleObjectProperty()

  override fun createTask(): Task<ObservableList<T>> {
    if (jsonResult.value == null) {
      return LoaderTask(busyIndicator, this.path, this.jsonResult)
    } else {
      return CachedTask(this.path, this.jsonResult)
    }
  }
}

// Takes the root node of GP Cloud response and filters teams
fun filterTeams(jsonNode: JsonNode, filter: Predicate<JsonNode>): List<JsonNode> {
  return if (jsonNode is ArrayNode) {
    jsonNode.filter(filter::test)
  } else {
    emptyList()
  }
}

// Takes a list of team nodes and returns filtered projects.
// This can work if teams.size > 1 (e.g. to find all projects matching some criteria)
// but in practice we expect teams.size == 1
fun filterProjects(teams: List<JsonNode>, filter: Predicate<JsonNode>): List<JsonNode> {
  return teams.flatMap { team ->
    team.get("projects").let { node ->
      if (node is ArrayNode) {
        node.filter(filter::test).map { project -> project.also { (it as ObjectNode).put("team", team["name"].asText()) } }
      } else {
        emptyList()
      }
    }
  }
}

// Processes cached response from GP Cloud
class CachedTask<T : CloudJsonAsFolderItem>(val path: Path, private val jsonNode: Property<JsonNode>) : Task<ObservableList<T>>() {
  override fun call(): ObservableList<T> {
    val list: List<CloudJsonAsFolderItem> = when (path.getNameCount()) {
      0 -> filterTeams(jsonNode.value, { true }).map(::TeamJsonAsFolderItem)
      1 -> {
        filterProjects(
            filterTeams(jsonNode.value) { it["name"].asText() == path.getName(0) },
            { true }
        ).map(::ProjectJsonAsFolderItem)
      }
      else -> emptyList()
    }
    return FXCollections.observableArrayList(list as List<T>)

  }

  fun callPublic(): ObservableList<T> {
    return this.call()
  }
}

class OfflineException(cause: Exception) : RuntimeException(cause)

// Sends HTTP request to GP Cloud and returns a list of teams.
class LoaderTask<T : CloudJsonAsFolderItem>(
    private val busyIndicator: Consumer<Boolean>,
    val path: Path,
    private val resultStorage: Property<JsonNode>) : Task<ObservableList<T>>() {
  override fun call(): ObservableList<T>? {
    return try {
      val jsonNode = JsonTask(
        method = HttpMethod.GET,
        uri = "/team/list",
        kv = mapOf("owned" to "true", "participated" to "true"),
        busyIndicator = { busyIndicator.accept(it) },
        onFailure = {_, resp ->
          with(LOG) {
            error(
              "Failed to get team list. Response code=${resp.code} reason=${resp.reason}")
          }
          throw GPCloudException(resp.code)
        }
      ).let {
        it.execute()
      }

      resultStorage.value = jsonNode
      CachedTask<T>(this.path, this.resultStorage).callPublic()
    } catch (ex: IOException) {
      LOG.error("Failed to contact ${HOST}", ex)
      throw GPCloudException(HttpStatus.SC_SERVICE_UNAVAILABLE)
    }

  }
}

private val OBJECT_MAPPER = ObjectMapper()

typealias ErrorUi = (String) -> Unit


// History service and tasks load project change history.
class HistoryService : Service<ObservableList<VersionJsonAsFolderItem>>() {
  var busyIndicator: (Boolean) -> Unit = {}
  lateinit var projectRefid: String

  override fun createTask(): Task<ObservableList<VersionJsonAsFolderItem>> {
    return HistoryTask(busyIndicator, projectRefid)
  }

}

class HistoryTask(private val busyIndicator: (Boolean) -> Unit,
                  private val projectRefid: String) : Task<ObservableList<VersionJsonAsFolderItem>>() {
  override fun call(): ObservableList<VersionJsonAsFolderItem> {
    this.busyIndicator(true)
    val log = GPLogger.getLogger("GPCloud")
    val http = HttpClientBuilder.buildHttpClient()

    val jsonBody = let {
      val resp = http.sendGet("/p/versions", mapOf("projectRefid" to projectRefid))
      if (resp.code == 200) {
        resp.rawBody.toString(Charsets.UTF_8)
      } else {
        with(log) {
          warning(
              "Failed to get project history. Response code=${resp.code} reason=${resp.reason}")
        }
        throw IOException("Server responded with HTTP ${resp.code}")
      }
    }

    val objectMapper = ObjectMapper()
    val jsonNode = objectMapper.readTree(jsonBody)
    return if (jsonNode is ArrayNode) {
      FXCollections.observableArrayList(jsonNode.map(::VersionJsonAsFolderItem))
    } else {
      FXCollections.observableArrayList()
    }
  }
}

internal enum class CloseReason {
  UNKNOWN, INVALID_UID, UNKNOWN_HEARTBEAT, NETWORK_FAILURE
}

private class WebSocketListenerImpl(
    private val token: String,
    private val onAuthCompleted: () -> Unit,
    private val onPayload: (ObjectNode) -> Unit,
    private val onClose: (CloseReason) -> Unit
) : WebSocketListener() {
  private lateinit var webSocket: WebSocket

  override fun onOpen(webSocket: WebSocket, response: Response) {
    LOG.debug("WebSocket opened")
    this.webSocket = webSocket
    LOG.debug("Trying sending token ${this.token}")
    this.webSocket.send("Basic ${this.token}")
    onAuthCompleted()
  }

  override fun onMessage(webSocket: WebSocket, text: String?) {
    val payload = OBJECT_MAPPER.readTree(text)
    if (payload is ObjectNode) {
      LOG.debug("WebSocket message:\n{}", payload)
      onPayload(payload)
    }
  }

  override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
    LOG.error("WebSocket closed. Code={}, reason={}", code, reason ?: "")
    val reasonEnum = if (code == 1003) {
      when (reason) {
        "UNKNOWN_HEARTBEAT" -> CloseReason.UNKNOWN_HEARTBEAT
        "INVALID_UID" -> CloseReason.INVALID_UID
        else -> CloseReason.UNKNOWN
      }
    } else {
      CloseReason.UNKNOWN
    }
    onClose(reasonEnum)
  }

  override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
    LOG.error("WebSocket network failure: {}", response ?: "", exception = t)
    onClose(CloseReason.NETWORK_FAILURE)
  }
}

class WebSocketClient {
  private var heartbeatFuture: ScheduledFuture<*>? = null
  private var wsListener: WebSocketListenerImpl? = null
  private val heartbeatExecutor = Executors.newSingleThreadScheduledExecutor()
  private var websocket: WebSocket? = null
  private val structureChangeListeners = mutableListOf<(Any) -> Unit>()
  private val lockStatusChangeListeners = mutableListOf<(ObjectNode) -> Unit>()
  private val contentChangeListeners = mutableListOf<(ObjectNode) -> Unit>()
  private var listeningDocument: GPCloudDocument? = null

  fun start() {

    LOG.debug("WebSocket started")
    val req = Request.Builder().url(GPCLOUD_WEBSOCKET_URL).build()
    this.websocket?.close(1000, "Reset Websocket")
    this.heartbeatFuture?.cancel(true)
    wsListener = WebSocketListenerImpl(GPCloudOptions.websocketAuthToken, this::onAuthDone, this::onMessage, this::onClose)
    this.websocket = OkHttpClient.Builder()
      .connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS))
      .build().newWebSocket(req, this.wsListener)
  }

  fun stop() {
    this.websocket?.close(1000, "Close Websocket")
    this.heartbeatFuture?.cancel(true)
  }

  private fun onAuthDone() {
    this.heartbeatFuture = this.heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 30, 60, TimeUnit.SECONDS)
  }

  private fun onMessage(payload: ObjectNode) {
    payload.get("type")?.textValue()?.let {
      when (it) {
        "ProjectLockStatusChange" -> fireLockStatusChange(payload)
        "ProjectChange", "ProjectRevert" -> fireContentsChange(payload)
        else -> fireStructureChange(payload)
      }
    }
  }

  private fun onClose(reason: CloseReason) {
    LOG.debug("WebSocket closed. reason={}", reason)
    when (reason) {
      CloseReason.UNKNOWN_HEARTBEAT -> GlobalScope.launch {
        LOG.debug("Trying to restart WebSocket")
        delay(Random.nextLong(10000, 60000))
        start()
      }
      CloseReason.INVALID_UID -> LOG.error("Need to re-authenticate!")
      CloseReason.NETWORK_FAILURE -> GlobalScope.launch {
        LOG.error("Trying to restart WebSocket")
        delay(Random.nextLong(10000, 60000))
        start()
      }
      else -> {
        LOG.error("WebSocket has been closed")
      }
    }
  }

  private fun fireStructureChange(payload: ObjectNode) {
    for (listener in this.structureChangeListeners) {
      listener(Any())
    }
  }

  private fun fireLockStatusChange(payload: ObjectNode) {
    for (listener in this.lockStatusChangeListeners) {
      listener(payload)
    }
  }

  private fun fireContentsChange(payload: ObjectNode) {
    LOG.debug("ProjectChange: {}", payload)
    this.contentChangeListeners.forEach { it(payload) }
  }

  fun onStructureChange(listener: (Any) -> Unit) {
    this.structureChangeListeners.add(listener)
  }

  fun onLockStatusChange(listener: (ObjectNode) -> Unit): ()->Unit {
    this.lockStatusChangeListeners.add(listener)
    return {
      this.lockStatusChangeListeners.remove(listener)
    }
  }

  fun onContentChange(listener: (ObjectNode) -> Unit): ()->Unit {
    this.contentChangeListeners.add(listener)
    return {
      this.contentChangeListeners.remove(listener)
    }
  }

  private fun sendHeartbeat() {
    this.websocket?.let { websocket ->
      websocket.send("HB")
    }
  }

  fun register(document: GPCloudDocument) {
    this.listeningDocument?.let {
      it.detachWebsocket(this)
    }
    this.listeningDocument = document.also {
      it.attachWebsocket(this)
    }
    if (this.heartbeatFuture?.let { it.isCancelled } != false) {
      this.start()
    }
  }
}

val webSocket = WebSocketClient()

// HTTP server for sign in into GP Cloud
typealias AuthTokenCallback = (token: String?, validity: String?, userId: String?, websocketToken: String?) -> Unit
typealias AuthStartCallback = ()->Unit

class HttpServerImpl : NanoHTTPD("localhost", 0) {
  var onTokenReceived: AuthTokenCallback? = null
  var onStart: AuthStartCallback? = null

  private fun getParam(session: IHTTPSession, key: String): String? {
    val values = session.parameters[key]
    return if (values?.size == 1) values[0] else null
  }

  override fun serve(session: IHTTPSession): Response {
    val args = mutableMapOf<String, String>()
    session.parseBody(args)
    return when (session.uri) {
      "/auth" -> {
        val token = getParam(session, "token")
        val userId = getParam(session, "userId")
        val validity = getParam(session, "validity")
        val websocketToken = getParam(session, "websocketToken")

        onTokenReceived?.invoke(token, validity, userId, websocketToken)
        newFixedLengthResponse("").apply {
          addHeader("Access-Control-Allow-Origin", GPCLOUD_ORIGIN)
        }
      }
      "/start" -> {
        onStart?.invoke()
        newFixedLengthResponse("").apply {
          addHeader("Access-Control-Allow-Origin", GPCLOUD_ORIGIN)
        }
      }
      else -> {
        LOG.error("Request to unknown URI: {}", session.uri)
        newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "").apply {
          addHeader("Access-Control-Allow-Origin", GPCLOUD_ORIGIN)
        }
      }
    }
  }
}

interface GPCloudHttpClient {
  interface Response {
    val decodedBody: ByteArray
    val rawBody: ByteArray
    val code: Int
    val reason: String
    fun header(name: String): String?
  }
  @Throws(IOException::class)
  fun sendGet(uri: String, args: Map<String, String?> = emptyMap()): Response
  @Throws(IOException::class)
  fun sendPost(uri: String, parts: Map<String, String?>, encoding: HttpPostEncoding = HttpPostEncoding.MULTIPART): Response
}

class HttpClientOk(
    private val host: String,
    val userId: String = "",
    val authToken: () -> String = {""}) : GPCloudHttpClient {
  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.SECONDS)
      .callTimeout(60, TimeUnit.SECONDS)
      .connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
      .followRedirects(true)
      .followSslRedirects(true).apply {
        if (userId.isNotBlank() && authToken().isNotBlank()) {
          addInterceptor {
            it.proceed(it.request().newBuilder().header("Authorization", Credentials.basic(userId, authToken())).build())
          }
        }
      }.build()

  override fun sendGet(uri: String, args: Map<String, String?>): GPCloudHttpClient.Response {
    val uriBuilder = URIBuilder(uri).also {
      args.forEach { (key, value) -> it.addParameter(key, value) }
      it.host = host
      it.scheme = GPCLOUD_SCHEME
    }
    val req = Request.Builder().url(uriBuilder.build().toURL()).build()
    return this.okHttpClient.newCall(req).execute().use {
      ResponseImpl(it)
    }
  }

  override fun sendPost(uri: String, parts: Map<String, String?>, encoding: HttpPostEncoding): GPCloudHttpClient.Response {
    val req = when (encoding) {
      HttpPostEncoding.MULTIPART -> {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM).also {
          parts.forEach { (k, v) ->  if (v != null) { it.addFormDataPart(k, v) }}
        }.build()
        val uriBuilder = URIBuilder(uri).also {
          it.host = host
          it.scheme = GPCLOUD_SCHEME
        }
        Request.Builder().url(uriBuilder.build().toURL()).post(body).build()
      }
      HttpPostEncoding.URLENCODED -> {
        val body = FormBody.Builder().also {
          parts.forEach { (k, v) ->  it.add(k, v)}
        }.build()
        val uriBuilder = URIBuilder(uri).also {
          it.host = host
          it.scheme = GPCLOUD_SCHEME
        }
        Request.Builder().url(uriBuilder.build().toURL()).post(body).build()
      }
    }
    return this.okHttpClient.newCall(req).execute().use {
      ResponseImpl(it)
    }
  }

  class ResponseImpl(private val response: Response): GPCloudHttpClient.Response {
    override val decodedBody: ByteArray by lazy {
      Base64InputStream(rawBody.inputStream()).readAllBytes()
    }
    override val rawBody: ByteArray = response.body()?.bytes() ?: byteArrayOf()
    override val code: Int = response.code()
    override val reason: String = response.message()
    override fun header(name: String): String? = response.header(name)
  }
}

object HttpClientBuilder {
  fun buildHttpClient(withAuth: Boolean = true): GPCloudHttpClient {
    return buildHttpClientOk(withAuth)
  }

  fun buildHttpClientOk(withAuth: Boolean): HttpClientOk {
    return if (withAuth) {
      HttpClientOk(HOST.toHostString(), GPCloudOptions.userId?.value ?: "", { GPCloudOptions.authToken?.value ?: "" })
    } else {
      HttpClientOk(HOST.toHostString())
    }
  }
}

private val reconnectHttpClient by lazy { HttpClientBuilder.buildHttpClient() }
fun isNetworkAvailable(): Boolean {
  LOG.debug("Checking if network is available...")
  return try {
      reconnectHttpClient.sendGet("/").code >= 200
    } catch (e: Exception) {
      LOG.debug("... unavailable: {}", e.message ?: "")
      false
    }
}

enum class HttpMethod {
  GET, POST
}
enum class HttpPostEncoding {
  URLENCODED, MULTIPART
}
data class Lock(
    var uid: String = "",
    var name: String = "",
    var email: String = "",
    var expirationEpochTs: Long = 0,
)

data class ProjectWriteResponse(
    var projectRefid: String = "",
    var lock: Lock? = null
)

private val HOST =
    if (GPCLOUD_HOST == "ganttproject.localhost") HttpHost.create("http://ganttproject.localhost:80")
    else HttpHost.create("https://$GPCLOUD_HOST:443")

private val LOG = GPLogger.create("Cloud.Http")
