package cloud.ganttproject.colloboque

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import fi.iki.elonen.NanoHTTPD

fun main(args: Array<String>) = DevServerMain().main(args)

class DevServerMain : CliktCommand() {
  val port by option("--port").int().default(9000)

  override fun run() {
    println("Starting dev Colloboque server on port $port")
    ColloboqueHttpServer(port).start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
  }
}

class ColloboqueHttpServer(port: Int) : NanoHTTPD("localhost", port) {
}