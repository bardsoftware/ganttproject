package cloud.ganttproject.colloboque

import com.github.ajalt.clikt.core.CliktCommand

fun main(args: Array<String>) = GrpcServerDev().main(args)

class GrpcServerDev : CliktCommand() {
  override fun run() {
    println("Starting dev Colloboque server")
  }
}