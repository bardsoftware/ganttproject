package cloud.ganttproject.colloboque

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import fi.iki.elonen.NanoHTTPD
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
    val updateInputChannel = Channel<ClientXlog>()
    val dataSourceFactory = PostgresDataSourceFactory(pgHost, pgPort, pgSuperUser, pgSuperAuth)
    val colloboqueServer = ColloboqueServer(dataSourceFactory::createDataSource, initInputChannel, updateInputChannel)
    ColloboqueHttpServer(port, colloboqueServer).start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
  }
}

class ColloboqueHttpServer(port: Int, private val colloboqueServer: ColloboqueServer) : NanoHTTPD("localhost", port) {
}