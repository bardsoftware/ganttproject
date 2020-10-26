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
import net.sourceforge.ganttproject.GPLogger
import okhttp3.*
import org.apache.commons.codec.binary.Base64InputStream
import org.apache.http.HttpHost
import org.apache.http.HttpStatus
import org.apache.http.client.utils.URIBuilder
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.logging.Level

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
    busyIndicator.accept(true)
    val log = GPLogger.getLogger("GPCloud")
    val http = HttpClientBuilder.buildHttpClient()

    return try {
      val jsonBody = let {
        val resp = http.sendGet("/team/list", mapOf("owned" to "true", "participated" to "true"))
        if (resp.code == 200) {
          resp.rawBody.toString(Charsets.UTF_8)
        } else {
          with(log) {
            warning(
                "Failed to get team list. Response code=${resp.code} reason=${resp.reason}")
          }
          throw GPCloudException(resp.code)
        }
      }
      val jsonNode = OBJECT_MAPPER.readTree(jsonBody)
      resultStorage.value = jsonNode
      CachedTask<T>(this.path, this.resultStorage).callPublic()
    } catch (ex: IOException) {
      log.log(Level.SEVERE, "Failed to contact ${HOST}", ex)
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

class WebSocketListenerImpl : WebSocketListener() {
  private var webSocket: WebSocket? = null
  private val structureChangeListeners = mutableListOf<(Any) -> Unit>()
  private val lockStatusChangeListeners = mutableListOf<(ObjectNode) -> Unit>()
  private val contentChangeListeners = mutableListOf<(ObjectNode) -> Unit>()

  internal val token: String?
    get() = GPCloudOptions.websocketAuthToken
  lateinit var onAuthCompleted: () -> Unit

  override fun onOpen(webSocket: WebSocket, response: Response) {
    println("WebSocket opened")
    this.webSocket = webSocket
    this.trySendToken()
  }

  private fun trySendToken() {
    println("Trying sending token ${this.token}")
    if (this.webSocket != null && this.token != null) {
      this.webSocket?.send("Basic ${this.token}")
      this.onAuthCompleted()
      println("Token is sent!")
    }
  }

  override fun onMessage(webSocket: WebSocket?, text: String?) {
    val payload = OBJECT_MAPPER.readTree(text)
    if (payload is ObjectNode) {
      LOG.debug("WebSocket message:\n{}", payload)
      payload.get("type")?.textValue()?.let {
        when (it) {
          "ProjectLockStatusChange" -> onLockStatusChange(payload)
          "ProjectChange", "ProjectRevert" -> onProjectContentsChange(payload)
          else -> onStructureChange(payload)
        }
      }
    }
  }

  private fun onStructureChange(payload: ObjectNode) {
    for (listener in this.structureChangeListeners) {
      listener(Any())
    }
  }

  private fun onLockStatusChange(payload: ObjectNode) {
    for (listener in this.lockStatusChangeListeners) {
      listener(payload)
    }
  }

  private fun onProjectContentsChange(payload: ObjectNode) {
    LOG.debug("ProjectChange: {}", payload)
    this.contentChangeListeners.forEach { it(payload) }
  }

  override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
    LOG.debug("WebSocket closed")
  }

  override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
    println("failure: $response.")
    t.printStackTrace()
  }

  fun addOnStructureChange(listener: (Any) -> Unit): () -> Unit {
    this.structureChangeListeners.add(listener)
    return { this.structureChangeListeners.remove(listener) }
  }

  fun addOnLockStatusChange(listener: (ObjectNode) -> Unit) {
    this.lockStatusChangeListeners.add(listener)
  }

  fun addOnContentChange(listener: (ObjectNode) -> Unit) {
    this.contentChangeListeners.add(listener)
  }

}

class WebSocketClient {
  private val okClient = OkHttpClient.Builder()
          .connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS))
          .build()
  private var isStarted = false
  private val wsListener = WebSocketListenerImpl()
  private val heartbeatExecutor = Executors.newSingleThreadScheduledExecutor()
  private lateinit var websocket: WebSocket

  fun start() {
    if (isStarted) {
      return
    }
    val req = Request.Builder().url(GPCLOUD_WEBSOCKET_URL).build()
    this.wsListener.onAuthCompleted = {
      this.heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, 30, 60, TimeUnit.SECONDS)
    }
    this.websocket = this.okClient.newWebSocket(req, this.wsListener)
    isStarted = true
  }

  fun onStructureChange(listener: (Any) -> Unit): () -> Unit {
    return this.wsListener.addOnStructureChange(listener)
  }

  fun onLockStatusChange(listener: (ObjectNode) -> Unit) {
    return this.wsListener.addOnLockStatusChange(listener)
  }

  fun onContentChange(listener: (ObjectNode) -> Unit) {
    return this.wsListener.addOnContentChange(listener)
  }

  fun sendHeartbeat() {
    this.websocket.send("HB")
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
        println("Unknown URI: ${session.uri}")
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
    val authToken: String = "") : GPCloudHttpClient {
  private val okHttpClient: OkHttpClient
  private val credentials = Credentials.basic(userId, authToken)
  init {
    okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .connectionSpecs(listOf(ConnectionSpec.COMPATIBLE_TLS))
        .followRedirects(true)
        .followSslRedirects(true).apply {
          if (userId.isNotBlank() && authToken.isNotBlank()) {
            addInterceptor {
              it.proceed(it.request().newBuilder().header("Authorization", credentials).build())
            }
          }
        }.build()
  }
  override fun sendGet(uri: String, args: Map<String, String?>): GPCloudHttpClient.Response {
    val uriBuilder = URIBuilder(uri).also {
      args.forEach { (key, value) -> it.addParameter(key, value) }
      it.host = host
      it.scheme = "https"
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
          it.scheme = "https"
        }
        Request.Builder().url(uriBuilder.build().toURL()).post(body).build()
      }
      HttpPostEncoding.URLENCODED -> {
        val body = FormBody.Builder().also {
          parts.forEach { (k, v) ->  it.add(k, v)}
        }.build()
        val uriBuilder = URIBuilder(uri).also {
          it.host = host
          it.scheme = "https"
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
      HttpClientOk(HOST.toHostString(), GPCloudOptions.userId?.value ?: "", GPCloudOptions.authToken?.value ?: "")
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
