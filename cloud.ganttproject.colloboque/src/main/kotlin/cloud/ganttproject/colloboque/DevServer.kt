/*
Copyright 2022 BarD Software s.r.o., GanttProject Cloud OU

This file is part of GanttProject Cloud.

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
package cloud.ganttproject.colloboque

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.output.MordantHelpFormatter
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import fi.iki.elonen.NanoWSD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.InitRecord
import net.sourceforge.ganttproject.storage.InputXlog
import net.sourceforge.ganttproject.storage.ServerResponse
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import java.util.zip.CRC32

fun main(args: Array<String>) = DevServerMain().main(args)

class DevServerMain : CliktCommand() {
  private val port by option("--port", help = "HTTP port to listen on").int().default(9000)
  private val wsPort by option("--ws-port", help = "WebSocket port to listen on").int().default(9001)
  private val pgHost by option("--pg-host", help = "Postgres host name").default("localhost")
  private val pgPort by option("--pg-port", help = "Postgres port").int().default(5432)
  private val pgSuperUser by option("--pg-super-user", help = "Postgres super user name").default("postgres")
  private val pgSuperAuth by option("--pg-super-auth", help = "Postgres super user password").default("")

  init {
    // TODO: is there a better place for this?
    localeApi
    context {
      helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true) }
    }
  }
  override fun run() {
    STARTUP_LOG.debug("Starting dev Colloboque server on port {}", port)

    val initInputChannel = Channel<InitRecord>()
    val updateInputChannel = Channel<InputXlog>()
    val serverResponseChannel = Channel<ServerResponse>()
    val connectionFactory = PostgresConnectionFactory(pgHost, pgPort, pgSuperUser, pgSuperAuth)
    val colloboqueServer = ColloboqueServer(connectionFactory::createConnection,
      PostgreStorageApi(connectionFactory),
      updateInputChannel, serverResponseChannel)
    ColloboqueHttpServer(port, colloboqueServer).start(0, false)
    ColloboqueWebSocketServer(wsPort, colloboqueServer, updateInputChannel, serverResponseChannel).start(0, false)
  }
}

class ColloboqueHttpServer(port: Int, private val colloboqueServer: ColloboqueServer) : NanoHTTPD("localhost", port) {
  override fun serve(session: IHTTPSession): Response {
    LOG.debug(session.uri)
    return when (session.uri) {
      "/init" -> {
        session.parameters["projectRefid"]?.firstOrNull()?.let {
          colloboqueServer.init(it, PROJECT_XML_TEMPLATE)
          newFixedLengthResponse("Ok")
        } ?: newFixedLengthResponse(Status.BAD_REQUEST, MIME_PLAINTEXT, "projectRefid is missing")
      }

      "/" -> newFixedLengthResponse("Hello")
      "/p/read" -> {
        session.parameters["projectRefid"]?.firstOrNull()?.let {projectRefid ->

          val snapshot = colloboqueServer.getProjectXml(projectRefid)
          newFixedLengthResponse(snapshot.projectXml!!.toBase64()).also { response ->
            response.addHeader("ETag", "-1")
            response.addHeader("Digest", CRC32().let { hash ->
              hash.update(snapshot.projectXml!!.toByteArray())
              hash.value.toString()
            })
            response.addHeader("BaseTxnId", snapshot.baseTxnId.toString())
          }
        } ?: newFixedLengthResponse(Status.BAD_REQUEST, MIME_PLAINTEXT, "projectRefid is missing")
      }

      else -> newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }
  }
}

class ColloboqueWebSocketServer(port: Int, private val colloboqueServer: ColloboqueServer,
                                private val updateInputChannel: Channel<InputXlog>,
                                private val serverResponseChannel: Channel<ServerResponse>) :
  NanoWSD("localhost", port) {
  private val wsResponseScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
  private val wsRequestScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
  private val connectedClients = mutableMapOf<ProjectRefid, MutableList<WebSocket>>()

  override fun openWebSocket(handshake: IHTTPSession): WebSocket {
    return WebSocketImpl(handshake)
  }

  private inner class WebSocketImpl(handshake: IHTTPSession) : WebSocket(handshake) {
    init {
//      handshake.parameters["projectRefid"]?.firstOrNull()?.also { refid ->
//        colloboqueServer.init(refid)
//        this.handshakeResponse.addHeader("baseTxnId", colloboqueServer.getBaseTxnId(refid))
//      }
      wsResponseScope.launch {
        for (response in serverResponseChannel) {
          LOG.debug("Sending response {}", response)
          LOG.debug("Connected clients: {}", connectedClients)
          try {
            val projectRefid = when (response) {
              is ServerResponse.CommitResponse -> response.projectRefid
              is ServerResponse.ErrorResponse -> response.projectRefid
            }
            connectedClients[projectRefid]?.forEach {
              it.send(Json.encodeToString(ServerResponse.serializer(), response))
            }
          } catch (e: Exception) {
            LOG.error("Failed to send response {}", response, e)
          }
        }
      }
    }
    private fun parseInputXlog(message: String): InputXlog? = try {
      if (message.startsWith("XLOG ")) {
        Json.decodeFromStream<InputXlog>(
          Base64.getDecoder().decode(message.substring("XLOG ".length)).inputStream()
        )
      } else null
    } catch (e: Exception) {
      LOG.error("Failed to parse {}", message, e)
      null
    }

    override fun onOpen() {
      LOG.debug("WebSocket opened")
    }

    override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
      for (webSockets in connectedClients.values) {
        webSockets.remove(this)
      }
      LOG.debug("WebSocket closed")
    }

    override fun onMessage(message: WebSocketFrame) {
      if (message.textPayload.startsWith("LISTEN")) {
        val refid = message.textPayload.substring("LISTEN ".length)
        connectedClients.getOrPut(refid) { mutableListOf() }.add(this)
        return
      }
      val inputXlog = parseInputXlog(message.textPayload) ?: return
      LOG.debug("Message received\n {}", inputXlog)
      wsRequestScope.launch {
        updateInputChannel.send(inputXlog)
      }
    }

    override fun onPong(pong: WebSocketFrame?) {}

    override fun onException(exception: IOException) {
      LOG.error("WebSocket exception", exception)
    }
  }
}

private val STARTUP_LOG = GPLogger.create("Startup")
private val LOG = GPLogger.create("ColloboqueWebServer")
