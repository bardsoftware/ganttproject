package com.bardsoftware.constable

import org.h2.tools.RunScript
import java.io.FileReader
import java.sql.DriverManager
import java.sql.SQLException
import java.util.logging.Level
import java.util.logging.Logger

private val logger = Logger.getLogger(H2Manager::class.java.name)

class H2Manager {
    private val connection = DriverManager.getConnection("jdbc:h2:mem:")

    fun execute(path : String) {
        try {
            RunScript.execute(connection, FileReader(path))
        } catch (e: SQLException) {
            logger.log(Level.SEVERE, e.message, e)
        }
    }
}
