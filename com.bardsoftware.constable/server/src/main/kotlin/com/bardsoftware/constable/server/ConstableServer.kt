package com.bardsoftware.constable.server

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Level
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger(ConstableServer::class.java.name)

class ConstableServer : CliktCommand() {
    private val url by option(help = "url").default("127.0.0.1")
    private val port by option(help = "port").int().default(5432).validate { require(it in 1024..65535) }
    private val name by option(help = "data base name").default("postgres")
    private val user by option(help = "user").default("postgres")
    private val password by option(help = "password").default("")

    override fun run() {
        val connectionUrl = "jdbc:postgresql://$url:$port/$name"
        try {
            DriverManager.getConnection(connectionUrl, user, password).use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT VERSION()").use { resultSet ->
                        if (resultSet.next()) {
                            println(resultSet.getString(1))
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            logger.log(Level.SEVERE, e.message, e)
            println("Something went wrong. " + e.message)
        }
    }
}

fun main(args: Array<String>) = ConstableServer().main(args)