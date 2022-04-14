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

import biz.ganttproject.storage.db.Tables.*
import net.sourceforge.ganttproject.task.*
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.text.Charsets

class SqlProjectDatabaseImpl(private val dataSource: DataSource) : ProjectDatabase {
  companion object Factory {
    fun createInMemoryDatabase(): ProjectDatabase {
      val dataSource = JdbcDataSource()
      dataSource.setURL(H2_IN_MEMORY_URL)
      return SqlProjectDatabaseImpl(dataSource)
    }
  }

  private fun <T> withDSL(
    errorMessage: () -> String = { "Failed to execute query" },
    body: (dsl: DSLContext) -> T
  ): T {
    try {
      dataSource.connection.use { connection ->
        try {
          val dsl = DSL.using(connection, SQLDialect.H2)
          return body(dsl)
        } catch (e: Exception) {
          throw ProjectDatabaseException(errorMessage(), e)
        }
      }
    } catch (e: SQLException) {
      throw ProjectDatabaseException("Failed to connect to the database", e)
    }
  }

  @Throws(ProjectDatabaseException::class)
  override fun init() {
    val scriptStream = javaClass.getResourceAsStream(DB_INIT_SCRIPT_PATH) ?: throw ProjectDatabaseException("Init script not found")
    try {
      val queries = String(scriptStream.readAllBytes(), Charsets.UTF_8)

      dataSource.connection.use { it.createStatement().execute(queries) }
    } catch (e: Exception) {
      throw ProjectDatabaseException("Failed to init the database", e)
    }
  }

  @Throws(ProjectDatabaseException::class)
  override fun insertTask(task: Task): Unit = withDSL({ "Failed to insert task ${task.taskID}" }) { dsl ->
    dsl
      .insertInto(TASK)
      .set(TASK.ID, task.taskID)
      .set(TASK.NAME, task.name)
      .set(TASK.COLOR, task.externalizedColor())
      .set(TASK.SHAPE, task.externalizedShape())
      .set(TASK.IS_MILESTONE, task.externalizedIsMilestone())
      .set(TASK.IS_PROJECT_TASK, task.isProjectTask)
      .set(TASK.START_DATE, task.externalizedStartDate())
      .set(TASK.DURATION, task.externalizedDurationLength())
      .set(TASK.COMPLETION, task.completionPercentage)
      .set(TASK.EARLIEST_START_DATE, task.externalizedEarliestStartDate())
      .set(TASK.PRIORITY, task.priority)
      .set(TASK.WEB_LINK, task.externalizedWebLink())
      .set(TASK.COST_MANUAL_VALUE, task.externalizedCostManualValue())
      .set(TASK.IS_COST_CALCULATED, task.externalizedIsCostCalculated())
      .set(TASK.NOTES, task.externalizedNotes())
      .execute()
  }

  @Throws(ProjectDatabaseException::class)
  override fun insertTaskDependency(taskDependency: TaskDependency): Unit = withDSL(
    { "Failed to insert task dependency ${taskDependency.dependee.taskID} -> ${taskDependency.dependant.taskID}" }) { dsl ->
    dsl
      .insertInto(TASKDEPENDENCY)
      .set(TASKDEPENDENCY.DEPENDEE_ID, taskDependency.dependee.taskID)
      .set(TASKDEPENDENCY.DEPENDANT_ID, taskDependency.dependant.taskID)
      .set(TASKDEPENDENCY.TYPE, taskDependency.constraint.type.persistentValue)
      .set(TASKDEPENDENCY.LAG, taskDependency.difference)
      .set(TASKDEPENDENCY.HARDNESS, taskDependency.hardness.identifier)
      .execute()
  }

  @Throws(ProjectDatabaseException::class)
  override fun shutdown() {
    try {
      dataSource.connection.use { connection ->
        connection.createStatement().execute("shutdown")
      }
    } catch (e: Exception) {
      throw ProjectDatabaseException("Failed to shutdown the database", e)
    }
  }
}

private const val H2_IN_MEMORY_URL = "jdbc:h2:mem:gantt-project-state;DB_CLOSE_DELAY=-1"
private const val DB_INIT_SCRIPT_PATH = "/sql/init-project-database.sql"
