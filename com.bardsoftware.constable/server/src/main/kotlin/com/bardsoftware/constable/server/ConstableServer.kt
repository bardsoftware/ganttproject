package com.bardsoftware.constable.server

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import mu.KotlinLogging
import java.sql.DriverManager
import java.sql.SQLException

private val logger = KotlinLogging.logger {}

class ConstableServer : CliktCommand() {
    private val `pg-host` by option().default("127.0.0.1")
    private val `pg-port` by option().int().default(5432).validate { require(it in 1024..65535) }
    private val `pg-database` by option().default("postgres")
    private val `pg-user` by option().default("postgres")
    private val `pg-password` by option().default("")

    override fun run() {
        val connectionUrl = "jdbc:postgresql://$`pg-host`:$`pg-port`/$`pg-database`"
        try {
            DriverManager.getConnection(connectionUrl, `pg-user`, `pg-password`).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT VERSION()").use { resultSet ->
                        if (resultSet.next()) {
                            println(resultSet.getString(1))
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            logger.error(e) { "Something went wrong. ${e.message}" }
            println("Something went wrong. ${e.message}")
        }
    }
}

fun main(args: Array<String>) = ConstableServer().main(args)