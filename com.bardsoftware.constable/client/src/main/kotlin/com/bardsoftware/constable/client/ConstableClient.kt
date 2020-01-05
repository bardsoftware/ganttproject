package com.bardsoftware.constable.client

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import mu.KotlinLogging
import java.sql.SQLException

private val logger = KotlinLogging.logger {}

class ConstableClient : CliktCommand() {
    private val path by argument(help = "Path to database file")

    override fun run() {
        try {
            H2Manager(path).use { it.execute() }
        } catch (e: SQLException) {
            logger.error(e) { "Something went wrong. ${e.message}" }
            println("Something went wrong. " + e.message)
        }
    }
}

fun main(args: Array<String>) = ConstableClient().main(args)