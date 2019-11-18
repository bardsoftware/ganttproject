package com.bardsoftware.constable

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.validate
import java.io.File

class H2Client : CliktCommand() {
    private val path by argument(help = "Path to sql file to load").validate {
        require(File(it).exists()) { "File does not exist" }
    }

    override fun run() {
        val h2Manager = H2Manager()
        h2Manager.execute(path)
    }
}

fun main(args: Array<String>) = H2Client().main(args)