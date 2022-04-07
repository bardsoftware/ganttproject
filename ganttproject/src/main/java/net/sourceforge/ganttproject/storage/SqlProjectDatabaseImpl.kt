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
import com.google.common.base.Charsets
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.util.ColorConvertion
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.jooq.impl.SQLDataType.*
import java.math.BigDecimal
import java.net.URLEncoder
import java.sql.SQLException
import javax.sql.DataSource

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
          LOG.error("$errorMessage {}", e)
          throw ProjectDatabaseException(errorMessage())
        }
      }
    } catch (e: SQLException) {
      val message = "Failed to connect to the database {}"
      LOG.error(message, e)
      throw ProjectDatabaseException(message)
    }
  }

  @Throws(ProjectDatabaseException::class)
  override fun init(): Unit = withDSL({ "Failed to initialize the database" }) { dsl ->
    dsl
      .createTable(TASK)
      .column(TASK.ID, INTEGER)
      .column(TASK.NAME, VARCHAR.notNull())
      .column(TASK.COLOR, VARCHAR.null_())
      .column(TASK.SHAPE, VARCHAR.null_())
      .column(TASK.IS_MILESTONE, BOOLEAN.notNull())
      .column(TASK.IS_PROJECT_TASK, BOOLEAN.notNull())
      .column(TASK.START_DATE, TIMESTAMP.notNull())
      .column(TASK.DURATION, INTEGER.notNull())
      .column(TASK.COMPLETION, INTEGER.null_())
      .column(TASK.EARLIEST_START_DATE, TIMESTAMP.null_())
      .column(TASK.THIRD_DATE_CONSTRAINT, INTEGER.null_())
      .column(TASK.PRIORITY, VARCHAR.null_())
      .column(TASK.WEB_LINK, VARCHAR.null_())
      .column(TASK.COST_MANUAL_VALUE, VARCHAR.null_())
      .column(TASK.IS_COST_CALCULATED, BOOLEAN.null_())
      .column(TASK.NOTES, VARCHAR.null_())
      .constraints(primaryKey(TASK.ID))
      .execute()

    dsl.createTable(TASKDEPENDENCY)
      .column(TASKDEPENDENCY.DEPENDEE_ID, INTEGER.notNull())
      .column(TASKDEPENDENCY.DEPENDANT_ID, INTEGER.notNull())
      .column(TASKDEPENDENCY.TYPE, VARCHAR.notNull())
      .column(TASKDEPENDENCY.LAG, INTEGER.notNull())
      .column(TASKDEPENDENCY.HARDNESS, VARCHAR.notNull())
      .constraints(
        primaryKey(TASKDEPENDENCY.DEPENDEE_ID, TASKDEPENDENCY.DEPENDANT_ID),
        foreignKey(TASKDEPENDENCY.DEPENDEE_ID).references(TASK, TASK.ID),
        foreignKey(TASKDEPENDENCY.DEPENDANT_ID).references(TASK, TASK.ID),
        check(TASKDEPENDENCY.DEPENDEE_ID.notEqual(TASKDEPENDENCY.DEPENDANT_ID))
      )
      .execute()
  }

  @Throws(ProjectDatabaseException::class)
  override fun insertTask(task: Task): Unit = withDSL({ "Failed to insert task ${task.taskID}" }) { dsl ->
    val priority = task.priority.let { pr -> if (pr != Task.DEFAULT_PRIORITY) pr.persistentValue else null }
    val webLink = task.webLink.let { link ->
      if (!link.isNullOrBlank() && link != "http://") URLEncoder.encode(link, Charsets.UTF_8.name()) else null
    }
    var costManualValue: String? = null
    var isCostCalculated: Boolean? = null
    if (!(task.cost.isCalculated && task.cost.manualValue == BigDecimal.ZERO)) {
      costManualValue = task.cost.manualValue.toPlainString()
      isCostCalculated = task.cost.isCalculated
    }
    val notes = task.notes?.let { notes -> notes.replace("\\r\\n", "\\n").ifBlank { null } }
    dsl
      .insertInto(TASK)
      .set(TASK.ID, task.taskID)
      .set(TASK.NAME, task.name)
      .set(TASK.COLOR, task.color?.let { color -> ColorConvertion.getColor(color) })
      .set(TASK.SHAPE, task.shape?.array)
      .set(TASK.IS_MILESTONE, task.isMilestone)
      .set(TASK.IS_PROJECT_TASK, task.isProjectTask)
      .set(TASK.START_DATE, java.sql.Timestamp(task.start.timeInMillis))
      .set(TASK.DURATION, task.duration.length)
      .set(TASK.COMPLETION, task.completionPercentage)
      .set(TASK.EARLIEST_START_DATE, task.third?.let { calendar -> java.sql.Timestamp(calendar.timeInMillis)  })
      .set(TASK.THIRD_DATE_CONSTRAINT, task.third?.let { task.thirdDateConstraint })
      .set(TASK.PRIORITY, priority)
      .set(TASK.WEB_LINK, webLink)
      .set(TASK.COST_MANUAL_VALUE, costManualValue)
      .set(TASK.IS_COST_CALCULATED, isCostCalculated)
      .set(TASK.NOTES, notes)
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
      val message = "Failed to shutdown the database {}"
      LOG.error(message, e)
      throw ProjectDatabaseException(message)
    }
  }
}

private val LOG = GPLogger.create("SqlProjectDatabaseImpl")
private const val H2_IN_MEMORY_URL = "jdbc:h2:mem:gantt-project-state;DB_CLOSE_DELAY=-1"
