package com.bardsoftware.constable.client

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import java.sql.SQLException
import java.util.logging.Level
import java.util.logging.Logger

private val logger = Logger.getLogger(H2Manager::class.java.name)

class ConstableClient : CliktCommand() {
    private val path by argument(help = "Path to database file")

    override fun run() {
        try {
            H2Manager(path).use { it.execute() }
        } catch (e: SQLException) {
            logger.log(Level.SEVERE, e.stackTrace.toString())
            println("Something went wrong. " + e.message)
        }
    }
}

fun main(args: Array<String>) = ConstableClient().main(args)