package com.bardsoftware.constable.client

import java.io.Closeable
import java.io.PrintStream
import java.sql.DriverManager
import java.sql.ResultSet

class H2Manager(path: String) : Closeable {
    private val connection = DriverManager.getConnection("jdbc:h2:$path")

    /** Executes any SQL command and provides no output. */
    fun execute(sqlCommand: String) {
        connection.createStatement().use { statement -> statement.execute(sqlCommand) }
    }

    /** Executes SQL query that should start with SELECT operator and outputs result. */
    fun executeQuery(sqlQuery: String) {
        connection.createStatement().use { statement ->
            statement.executeQuery(sqlQuery)
                    .use { rs -> printResultSet(rs, System.out) }
        }
    }

    override fun close() {
        connection.close()
    }

    private fun printResultSet(resultSet: ResultSet, printStream: PrintStream) {
        val columnsCount = resultSet.metaData.columnCount
        for (i in 1..columnsCount) {
            printStream.print(resultSet.metaData.getColumnName(i) + " ")
        }
        printStream.println()
        while (resultSet.next()) {
            for (i in 1..columnsCount) {
                val columnValue: String = resultSet.getString(i)
                print("$columnValue ")
            }
            printStream.println()
        }
    }
}