package com.bardsoftware.constable.client

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import mu.KotlinLogging
import java.sql.SQLException

private val logger = KotlinLogging.logger {}

class ConstableClient : CliktCommand() {
    private val path by argument(name = "h2-path", help = "Path to database file")
    private val isQuery by option("-q",
            help = "Specify if SQL command is a query and an output is required").flag(default = false)
    private val command by option("--h2-command", help = "SQL command to execute").required()

    override fun run() {
        try {
            if (isQuery)
                H2Manager(path).use { it.executeQuery(command) }
            else {
                H2Manager(path).use { it.execute(command) }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Something went wrong. ${e.message}" }
            println("Something went wrong. " + e.message)
        }
    }
}

fun main(args: Array<String>) = ConstableClient().main(args)