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
import biz.ganttproject.core.option.DefaultStringOption
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.core.option.StringOption
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.StorageDialogBuilder
import com.fasterxml.jackson.databind.node.ObjectNode
import fi.iki.elonen.NanoHTTPD
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.document.AbstractURLDocument
import net.sourceforge.ganttproject.document.Document
import org.apache.commons.codec.binary.Base64InputStream
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
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
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.protocol.HttpContext
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

private const val GPCLOUD_HOST = "cumulus-dot-ganttproject-cloud.appspot.com"
//private const val GPCLOUD_HOST = "cloud.ganttproject.biz"
private const val GPCLOUD_ORIGIN = "https://$GPCLOUD_HOST"
const val GPCLOUD_LANDING_URL = "https://$GPCLOUD_HOST"
private const val GPCLOUD_PROJECT_READ_URL = "$GPCLOUD_ORIGIN/p/read"
const val GPCLOUD_SIGNIN_URL = "https://$GPCLOUD_HOST/__/auth/desktop"
const val GPCLOUD_SIGNUP_URL = "https://$GPCLOUD_HOST/__/auth/handler"

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
    val browserPane = GPCloudBrowserPane(this.mode, this.dialogUi, this.openDocument)
    val onTokenCallback: AuthTokenCallback = { token, validity, userId ->
      with(GPCloudOptions) {
        this.authToken.value = token
        this.validity.value = Instant.now().plus(validity?.toLongOrNull() ?: 0L, ChronoUnit.HOURS).epochSecond.toString()
        this.userId.value = userId
      }
      Platform.runLater {
        nextPage(browserPane.createStorageUi())
      }
    }

    val signupPane = GPCloudSignupPane(onTokenCallback)
    if (GPCloudOptions.authToken.value != "") {
      val paneBuilder = VBoxBuilder("pane-service-contents")
      paneBuilder.addTitle("Signing in to GanttProject Cloud")
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
        paneBuilder.add(ProgressIndicator(-1.0), null, Priority.ALWAYS)
        nextPage(paneBuilder.vbox)
      }
    }
    signupPane.tryAccessToken(
        Consumer { _ ->
          println("Auth token is valid!")
          Platform.runLater {
            nextPage(browserPane.createStorageUi())
          }
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

  private fun nextPage(newPage: Pane): Pane {
    FXUtil.transitionCenterPane(myPane, newPage) { dialogUi.resize() }
    return newPage
  }
}

// Persistently stored options
object GPCloudOptions {
  val authToken: StringOption = DefaultStringOption("authToken", "")
  val validity: StringOption = DefaultStringOption("validity", "")
  val userId: StringOption = DefaultStringOption("userId")

  val optionGroup: GPOptionGroup = GPOptionGroup("ganttproject-cloud", authToken, validity, userId)
}

// HTTP server for sign in into GP Cloud
typealias AuthTokenCallback = (token: String?, validity: String?, userId: String?) -> Unit

class HttpServerImpl : NanoHTTPD("localhost", 0) {
  var onTokenReceived: AuthTokenCallback? = null

  private fun getParam(session: IHTTPSession, key: String): String? {
    val values = session.parameters[key]
    return if (values?.size == 1) values[0] else null
  }

  override fun serve(session: IHTTPSession): Response {
    val args = mutableMapOf<String, String>()
    session.parseBody(args)
    val token = getParam(session, "token")
    val userId = getParam(session, "userId")
    val validity = getParam(session, "validity")

    onTokenReceived?.invoke(token, validity, userId)
    val resp = newFixedLengthResponse("")
    resp.addHeader("Access-Control-Allow-Origin", GPCLOUD_ORIGIN)
    return resp
  }
}

data class GPCloudHttpClient(
    val client: HttpClient, val host: HttpHost, val context: HttpContext)

object HttpClientBuilder {
  fun buildHttpClient(): GPCloudHttpClient {
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
    return GPCloudHttpClient(httpClient, httpHost, context)
  }
}

class GPCloudDocument(private val teamName: String, private val projectRefid: String, private val projectName: String)
  : AbstractURLDocument() {
  constructor(projectNode: ObjectNode): this(projectNode["team"].asText(), projectNode["refid"].asText(), projectNode["name"].asText()) {}

  override fun getFileName(): String {
    return this.projectName
  }

  override fun canRead(): Boolean = true

  override fun canWrite(): IStatus = Status.OK_STATUS

  override fun isValidForMRU(): Boolean = true

  override fun getInputStream(): InputStream {
    val http = HttpClientBuilder.buildHttpClient()
    val projectRead = HttpGet("/p/read$queryArgs")
    val resp = http.client.execute(http.host, projectRead, http.context)
    if (resp.statusLine.statusCode == 200) {
      return Base64InputStream(resp.entity.content)
    } else {
      throw IOException("Failed to read from GanttProject Cloud")
    }
  }

  override fun getOutputStream(): OutputStream {
    return object : ByteArrayOutputStream() {
      override fun close() {
        super.close()
        val http = HttpClientBuilder.buildHttpClient()
        val projectWrite = HttpPost("/p/write")
        val multipartBuilder = MultipartEntityBuilder.create()
        multipartBuilder.addPart("projectRefid", StringBody(
            this@GPCloudDocument.projectRefid, ContentType.TEXT_PLAIN))
        multipartBuilder.addPart("fileContents", StringBody(
            Base64.getEncoder().encodeToString(this.toByteArray()), ContentType.TEXT_PLAIN))
        projectWrite.entity = multipartBuilder.build()

        val resp = http.client.execute(http.host, projectWrite, http.context)
        if (resp.statusLine.statusCode != 200) {
          throw IOException("Failed to write to GanttProject Cloud")
        }
      }
    }
  }

  override fun getPath(): String = """ganttproject.cloud://${this.teamName}/${this.projectName}/${this.projectRefid}"""

  override fun write() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getURI(): URI = URI("""$GPCLOUD_PROJECT_READ_URL$queryArgs""")

  override fun isLocal(): Boolean = false

  private val queryArgs: String
    get() = "?projectRefid=${this.projectRefid}"
}
