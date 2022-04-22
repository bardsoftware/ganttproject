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
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.Response.Status
import fi.iki.elonen.NanoWSD
import kotlinx.coroutines.channels.Channel
import net.sourceforge.ganttproject.GPLogger
import org.slf4j.LoggerFactory
import java.io.IOException

fun main(args: Array<String>) = DevServerMain().main(args)

class DevServerMain : CliktCommand() {
  private val port by option("--port").int().default(9000)
  private val wsPort by option("--ws-port").int().default(9001)
  private val pgHost by option("--pg-host").default("localhost")
  private val pgPort by option("--pg-port").int().default(5432)
  private val pgSuperUser by option("--pg-super-user").default("postgres")
  private val pgSuperAuth by option("--pg-super-auth").default("")

  override fun run() {
    LoggerFactory.getLogger("Startup").info("Starting dev Colloboque server on port $port")

    val initInputChannel = Channel<InitRecord>()
    val updateInputChannel = Channel<InputXlog>()
    val dataSourceFactory = PostgresDataSourceFactory(pgHost, pgPort, pgSuperUser, pgSuperAuth)
    val colloboqueServer = ColloboqueServer(dataSourceFactory::createDataSource, initInputChannel, updateInputChannel)
    ColloboqueHttpServer(port, colloboqueServer).start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    ColloboqueWebSocketServer(wsPort, colloboqueServer).start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
  }
}

class ColloboqueHttpServer(port: Int, private val colloboqueServer: ColloboqueServer) : NanoHTTPD("localhost", port) {
  override fun serve(session: IHTTPSession): Response =
    when (session.uri) {
      "/init" -> {
        session.parameters["projectRefid"]?.firstOrNull()?.let {
          colloboqueServer.init(it)
          newFixedLengthResponse("Ok")
        } ?: newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "projectRefid is missing")
      }
      "/" -> newFixedLengthResponse("Hello")
      else -> newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }
}

class ColloboqueWebSocketServer(port: Int, private val colloboqueServer: ColloboqueServer) : NanoWSD("localhost", port) {
  override fun openWebSocket(handshake: IHTTPSession?): WebSocket {
    return WebSocketImpl(handshake)
  }

  private class WebSocketImpl(handshake: IHTTPSession?): WebSocket(handshake) {
    override fun onOpen() {
      LOG.debug("WebSocket opened")
    }

    override fun onClose(code: WebSocketFrame.CloseCode?, reason: String?, initiatedByRemote: Boolean) {
      LOG.debug("WebSocket closed")
    }

    override fun onMessage(message: WebSocketFrame?) {
      try {
        LOG.debug("Message received ${message?.textPayload}")
        send(message?.textPayload)
      } catch (e: IOException) {
        LOG.error("Failed to send response {}", e)
      }
    }

    override fun onPong(pong: WebSocketFrame?) {}

    override fun onException(exception: IOException?) {
      LOG.error("WebSocket exception $exception")
    }
  }
}

private val LOG = GPLogger.create("ColloboqueWebServer")