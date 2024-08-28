/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.storage

import javax.sql.DataSource

/**
 * Runs the given statements in a single transaction.
 */
fun runStatements(dataSource: DataSource, statements: List<String>) {
  val sqlScript = """
    ${statements.joinToString(separator = ";\n")};
  """.trimIndent()
  println("Running \n $sqlScript")
  runScript(dataSource, sqlScript)
}

fun runScriptFromResource(dataSource: DataSource, path: String) {
  val scriptStream = object{}.javaClass.getResourceAsStream(path) ?: throw ProjectDatabaseException("Init script not found")
  runScript(dataSource, scriptStream.bufferedReader().use { it.readText() })
}

fun runScript(dataSource: DataSource, sqlText: String) {
  try {
    dataSource.connection.use { it.createStatement().use { it.execute(sqlText) } }
  } catch (e: Exception) {
    throw ProjectDatabaseException("Failed to run the script", e)
  }

}
