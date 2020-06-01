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

import biz.ganttproject.storage.DocumentUri
import biz.ganttproject.storage.Path
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.io.ByteStreams
import com.google.common.io.CharStreams
import com.google.common.io.Closer
import fi.iki.elonen.NanoHTTPD
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Service
import javafx.concurrent.Task
import javafx.event.EventHandler
import net.sourceforge.ganttproject.GPLogger
import okhttp3.*
import org.apache.commons.codec.binary.Base64InputStream
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.ClientContext
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.io.InputStreamReader
import java.net.Socket
import java.time.Duration
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
class LoaderService<T: CloudJsonAsFolderItem> : Service<ObservableList<T>>() {
  var busyIndicator: Consumer<Boolean> = Consumer {}
  var path: Path = DocumentUri(listOf(), true, "GanttProject Cloud")
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
class CachedTask<T: CloudJsonAsFolderItem>(val path: Path, private val jsonNode: Property<JsonNode>) : Task<ObservableList<T>>() {
  override fun call(): ObservableList<T> {
    val list: List<CloudJsonAsFolderItem> = when (path.getNameCount()) {
      0 -> filterTeams(jsonNode.value, Predicate { true }).map(::TeamJsonAsFolderItem)
      1 -> {
        filterProjects(
            filterTeams(jsonNode.value, Predicate { it["name"].asText() == path.getName(0).toString() }),
            Predicate { true }
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
class LoaderTask<T: CloudJsonAsFolderItem>(
    private val busyIndicator: Consumer<Boolean>,
    val path: Path,
    private val resultStorage: Property<JsonNode>) : Task<ObservableList<T>>() {
  override fun call(): ObservableList<T>? {
    busyIndicator.accept(true)
    val log = GPLogger.getLogger("GPCloud")
    val http = HttpClientBuilder.buildHttpClientApache()
    val teamList = HttpGet("/team/list?owned=true&participated=true")

    return try {
      val jsonBody = let {
        val resp = http.client.execute(http.host, teamList, http.context)
        if (resp.statusLine.statusCode == 200) {
          CharStreams.toString(InputStreamReader(resp.entity.content, Charsets.UTF_8))
        } else {
          with(log) {
            warning(
                "Failed to get team list. Response code=${resp.statusLine.statusCode} reason=${resp.statusLine.reasonPhrase}")
            fine(EntityUtils.toString(resp.entity))
          }
          throw GPCloudException(resp.statusLine.statusCode)
        }
      }
      val jsonNode = OBJECT_MAPPER.readTree(jsonBody)
      resultStorage.value = jsonNode
      CachedTask<T>(this.path, this.resultStorage).callPublic()
    } catch (ex: IOException) {
      log.log(Level.SEVERE, "Failed to contact ${http.host}", ex)
      throw GPCloudException(HttpStatus.SC_SERVICE_UNAVAILABLE)
    }

  }
}

private val OBJECT_MAPPER = ObjectMapper()

typealias ErrorUi = (String) -> Unit

class LockService(private val errorUi: ErrorUi) : Service<JsonNode>() {
  var busyIndicator: Consumer<Boolean> = Consumer {}
  lateinit var project: ProjectJsonAsFolderItem
  var requestLockToken: Boolean = false
  lateinit var duration: Duration

  override fun createTask(): Task<JsonNode> {
    val task = LockTask(this.busyIndicator, project, requestLockToken, duration)
    task.onFailed = EventHandler { _ ->
      val errorDetails = if (task.exception != null) {
        GPLogger.getLogger("GPCloud").log(Level.WARNING, "", task.exception)
        "\n${task.exception.message}"
      } else {
        ""
      }
      this.errorUi("Failed to lock project: $errorDetails")
    }
    return task
  }
}

class LockTask(private val busyIndicator: Consumer<Boolean>,
               val project: ProjectJsonAsFolderItem,
               val requestLockToken: Boolean,
               val duration: Duration) : Task<JsonNode>() {
  override fun call(): JsonNode {
    busyIndicator.accept(true)
    val log = GPLogger.getLogger("GPCloud")
    val http = HttpClientBuilder.buildHttpClientApache()
    val resp = if (project.isLocked) {
      val projectUnlock = HttpPost("/p/unlock")
      val params = listOf(
          BasicNameValuePair("projectRefid", project.refid))
      projectUnlock.entity = UrlEncodedFormEntity(params)
      http.client.execute(http.host, projectUnlock, http.context)
    } else {
      val projectLock = HttpPost("/p/lock")
      val params = listOf(
          BasicNameValuePair("projectRefid", project.refid),
          BasicNameValuePair("expirationPeriodSeconds", this.duration.seconds.toString()),
          BasicNameValuePair("requestLockToken", requestLockToken.toString())
      )
      projectLock.entity = UrlEncodedFormEntity(params)

      http.client.execute(http.host, projectLock, http.context)
    }
    if (resp.statusLine.statusCode == 200) {
      val jsonBody = CharStreams.toString(InputStreamReader(resp.entity.content, Charsets.UTF_8))
      return if (jsonBody == "") {
        MissingNode.getInstance()
      } else {
        OBJECT_MAPPER.readTree(jsonBody)
      }
    } else {
      with(log) {
        warning(
            "Failed to get lock project. Response code=${resp.statusLine.statusCode} reason=${resp.statusLine.reasonPhrase}")
      }
      throw IOException("Server responded with HTTP ${resp.statusLine.statusCode}")
    }
  }
}


// History service and tasks load project change history.
class HistoryService : Service<ObservableList<VersionJsonAsFolderItem>>() {
  var busyIndicator: (Boolean) -> Unit = {}
  lateinit var projectNode: ProjectJsonAsFolderItem

  override fun createTask(): Task<ObservableList<VersionJsonAsFolderItem>> {
    return HistoryTask(busyIndicator, projectNode)
  }

}

class HistoryTask(private val busyIndicator: (Boolean) -> Unit,
                  private val project: ProjectJsonAsFolderItem) : Task<ObservableList<VersionJsonAsFolderItem>>() {
  override fun call(): ObservableList<VersionJsonAsFolderItem> {
    this.busyIndicator(true)
    val log = GPLogger.getLogger("GPCloud")
    val http = HttpClientBuilder.buildHttpClientApache()
    val teamList = HttpGet("/p/versions?projectRefid=${project.refid}")

    val jsonBody = let {
      val resp = http.client.execute(http.host, teamList, http.context)
      if (resp.statusLine.statusCode == 200) {
        CharStreams.toString(InputStreamReader(resp.entity.content, Charsets.UTF_8))
      } else {
        with(log) {
          warning(
              "Failed to get project history. Response code=${resp.statusLine.statusCode} reason=${resp.statusLine.reasonPhrase}")
          fine(EntityUtils.toString(resp.entity))
        }
        throw IOException("Server responded with HTTP ${resp.statusLine.statusCode}")
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
  fun sendGet(uri: String): Response
  @Throws(IOException::class)
  fun sendPost(uri: String, parts: Map<String, String?>): Response
}

class HttpClientApache(
    val client: HttpClient, val host: HttpHost, val context: HttpContext) : GPCloudHttpClient {
  class ResponseImpl(val resp: HttpResponse) : GPCloudHttpClient.Response {
    override val decodedBody: ByteArray by lazy {
      Base64InputStream(resp.entity.content).readAllBytes()
    }

    override val rawBody: ByteArray
      get() = resp.entity.content.readAllBytes()

    override val code: Int by lazy {
      resp.statusLine.statusCode
    }

    override val reason: String by lazy {
      resp.statusLine.reasonPhrase
    }

    override fun header(name: String): String? {
      return resp.getFirstHeader(name)?.value
    }
  }

  override fun sendGet(uri: String): GPCloudHttpClient.Response {
    return ResponseImpl(this.client.execute(this.host, HttpGet(uri), this.context))
  }

  override fun sendPost(uri: String, parts: Map<String, String?>): GPCloudHttpClient.Response {
    val httpPost = HttpPost(uri)
    val multipartBuilder = MultipartEntityBuilder.create()
    parts.filterValues { it != null }.forEach { key, value ->
      multipartBuilder.addPart(key, StringBody(value, ContentType.TEXT_PLAIN.withCharset(Charsets.UTF_8)))
    }
    httpPost.entity = multipartBuilder.build()
    return ResponseImpl(this.client.execute(this.host, httpPost, this.context))
  }
}

object HttpClientBuilder {
  fun buildHttpClient(): GPCloudHttpClient {
    return buildHttpClientApache()
  }

  fun buildHttpClientApache(): HttpClientApache {
    val httpClient = if (true || System.getProperty("gp.ssl.trustAll")?.toBoolean() == true) {
      val trustAll = TrustStrategy { _, _ -> true }
      val sslSocketFactory = SSLSocketFactory(
          trustAll, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
      val schemeRegistry = SchemeRegistry()
      schemeRegistry.register(Scheme("https", 443, sslSocketFactory))
      val connectionManager = PoolingClientConnectionManager(schemeRegistry)
      DefaultHttpClient(connectionManager)
    } else {
      DefaultHttpClient()
    }
    val context = BasicHttpContext()
    val httpHost = HttpHost(GPCLOUD_HOST, 443, "https")
    if (GPCloudOptions.authToken.value != "") {
      httpClient.credentialsProvider.setCredentials(
          AuthScope(httpHost), UsernamePasswordCredentials(GPCloudOptions.userId.value, GPCloudOptions.authToken.value))
      val authCache = BasicAuthCache()
      authCache.put(httpHost, BasicScheme())
      context.setAttribute(ClientContext.AUTH_CACHE, authCache)
    }
    return HttpClientApache(httpClient, httpHost, context)
  }
}

fun isNetworkAvailable(): Boolean {
  return try {
    Closer.create().use { closer ->
      val pingSocket = Socket(GPCLOUD_IP, 80).also { closer.register(it) }
      closer.register(pingSocket.getOutputStream())
      closer.register(pingSocket.getInputStream())
    }
    true
  } catch (e: IOException) {
    false
  }
}
private val LOG = GPLogger.create("Cloud.Http")

data class Lock(var uid: String = "",
                var name: String = "",
                var email: String = ""
)

data class ProjectWriteResponse(
    var projectRefid: String = "",
    var lock: Lock? = null
)
