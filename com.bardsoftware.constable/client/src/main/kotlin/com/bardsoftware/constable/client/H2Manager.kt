package com.bardsoftware.constable.client

import java.io.Closeable
import java.sql.DriverManager

class H2Manager(path: String) : Closeable {
    private val connection = DriverManager.getConnection("jdbc:h2:$path")

    fun execute() {
        val sqlQuery = "SELECT 2 + 2;"
        connection.createStatement().use { statement -> statement.execute(sqlQuery) }
    }

    override fun close() {
        connection.close()
    }
}