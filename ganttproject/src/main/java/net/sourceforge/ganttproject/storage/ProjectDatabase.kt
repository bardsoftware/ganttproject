/*
Copyright 2022 BarD Software s.r.o., Anastasiia Postnikova

This file is part of GanttProject, an open-source project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.sourceforge.ganttproject.storage

import net.sourceforge.ganttproject.storage.Tables.*
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.task.Task
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DSL.primaryKey
import org.jooq.impl.SQLDataType.*
import java.sql.SQLException
import javax.sql.DataSource

class ProjectDatabaseException(message: String) : Exception(message)

/**
 * Storage for holding the current state of a Gantt project.
 */
class ProjectDatabase internal constructor(private val dataSource: DataSource) {
  companion object Factory {
    fun createInMemoryDatabase(): ProjectDatabase {
      val dataSource = JdbcDataSource()
      dataSource.setURL(H2_IN_MEMORY_URL)
      return ProjectDatabase(dataSource)
    }
  }

  private fun <T> withExecutor(
    errorMessage: String = "Failed to execute query",
    body: (executor: DSLContext) -> T
  ): T {
    try {
      dataSource.connection.use { connection ->
        try {
          val executor = DSL.using(connection, SQLDialect.H2)
          return body(executor)
        } catch (e: Exception) {
          LOG.error("$errorMessage {}", e)
          throw ProjectDatabaseException(errorMessage)
        }
      }
    } catch (e: SQLException) {
      val message = "Failed to connect to the database"
      LOG.error(message, e)
      throw ProjectDatabaseException(message)
    }
  }

  fun init() = withExecutor("Failed to initialize the database") { executor ->
    executor
      .createTable(TASK)
      .column(TASK.ID, INTEGER)
      .column(TASK.NAME, VARCHAR.notNull())
      .column(TASK.COLOR, VARCHAR.null_())
      .constraints(primaryKey(TASK.ID))
      .execute()
  }

  fun insertTask(task: Task) = withExecutor("Failed to insert task ${task.taskID}") { executor ->
    executor
      .insertInto(TASK)
      .set(TASK.ID, task.taskID)
      .set(TASK.NAME, task.name)
      .set(TASK.COLOR, task.color.toString())
      .execute()
  }

  /** Close connections and release the resources. */
  fun shutdown() {
    try {
      dataSource.connection.use { conn ->
        conn.createStatement().execute("SHUTDOWN")
      }
    } catch (e: Exception) {
      val message = "Failed to shutdown the database {}"
      LOG.error(message, e)
      throw ProjectDatabaseException(message)
    }
  }
}

private val LOG = GPLogger.create("ProjectDatabase")
private const val H2_IN_MEMORY_URL = "jdbc:h2:mem:gantt-project-state;DB_CLOSE_DELAY=-1"
