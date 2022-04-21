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
import kotlinx.coroutines.channels.Channel
import org.slf4j.LoggerFactory

fun main(args: Array<String>) = DevServerMain().main(args)

class DevServerMain : CliktCommand() {
  val port by option("--port").int().default(9000)
  val pgHost by option("--pg-host").default("localhost")
  val pgPort by option("--pg-port").int().default(5432)
  val pgSuperUser by option("--pg-super-user").default("postgres")
  val pgSuperAuth by option("--pg-super-auth").default("")

  override fun run() {
    LoggerFactory.getLogger("Startup").info("Starting dev Colloboque server on port $port")

    val initInputChannel = Channel<InitRecord>()
    val updateInputChannel = Channel<InputXlog>()
    val dataSourceFactory = PostgresDataSourceFactory(pgHost, pgPort, pgSuperUser, pgSuperAuth)
    val colloboqueServer = ColloboqueServer(dataSourceFactory::createDataSource, initInputChannel, updateInputChannel)
    ColloboqueHttpServer(port, colloboqueServer).start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
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