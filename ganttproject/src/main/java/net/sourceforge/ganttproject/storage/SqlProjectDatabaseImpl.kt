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

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.storage.db.Tables.*
import biz.ganttproject.storage.db.tables.records.TaskRecord
import net.sourceforge.ganttproject.storage.ProjectDatabase.*
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskInfo
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.UpdateSetMoreStep
import org.jooq.UpdateSetStep
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
import java.awt.Color
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

  /** Execute query and save its xlog. */
  private fun withLog(
    errorMessage: () -> String,
    buildQuery: (dsl: DSLContext) -> String
  ): Unit = withDSL(errorMessage) { dsl ->
    dsl.transaction { config ->
      val context = DSL.using(config)
      val query = buildQuery(context)
      context.execute(query)
      context
        .insertInto(LOGRECORD)
        .set(LOGRECORD.SQL_STATEMENT, query)
        .execute()
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

  override fun createTaskUpdateBuilder(task: Task): TaskUpdateBuilder = SqlTaskUpdateBuilder(task, this::executeUpdate)

  @Throws(ProjectDatabaseException::class)
  override fun insertTask(task: Task): Unit = withLog({ "Failed to insert task ${task.logId()}" }) { dsl ->
    buildInsertTaskQuery(dsl, task).getSQL(ParamType.INLINED)
  }

  @Throws(ProjectDatabaseException::class)
  override fun insertTaskDependency(taskDependency: TaskDependency): Unit = withLog(
    { "Failed to insert task dependency ${taskDependency.dependee.logId()} -> ${taskDependency.dependant.logId()}" }) { dsl ->
    dsl
      .insertInto(TASKDEPENDENCY)
      .set(TASKDEPENDENCY.DEPENDEE_UID, taskDependency.dependee.uid)
      .set(TASKDEPENDENCY.DEPENDANT_UID, taskDependency.dependant.uid)
      .set(TASKDEPENDENCY.TYPE, taskDependency.constraint.type.persistentValue)
      .set(TASKDEPENDENCY.LAG, taskDependency.difference)
      .set(TASKDEPENDENCY.HARDNESS, taskDependency.hardness.identifier)
      .getSQL(ParamType.INLINED)
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

  @Throws(ProjectDatabaseException::class)
  override fun fetchLogRecords(startId: Int, limit: Int): List<LogRecord> = withDSL({ "Failed to fetch log records" }) { dsl ->
    dsl
      .selectFrom(LOGRECORD)
      .where(LOGRECORD.ID.ge(startId))
      .orderBy(LOGRECORD.ID.asc())
      .limit(limit)
      .map { LogRecord(it.id, it.sqlStatement) }
  }

  /** Execute update query and save its xlog. */
  @Throws(ProjectDatabaseException::class)
  internal fun executeUpdate(query: String) = withLog({ "Failed to execute update" }) { query }
}


class SqlTaskUpdateBuilder(private val task: Task,
                           private val executeQuery: (String) -> Unit): TaskUpdateBuilder {
  private var lastSetStep: UpdateSetMoreStep<TaskRecord>? = null

  private fun nextStep(step: (lastStep: UpdateSetStep<TaskRecord>) -> UpdateSetMoreStep<TaskRecord>) {
    lastSetStep = step(lastSetStep ?: DSL.using(SQLDialect.H2).update(TASK))
  }

  @Throws(ProjectDatabaseException::class)
  override fun execute() {
    try {
      lastSetStep?.let { updateQuery ->
        executeQuery(updateQuery.where(TASK.UID.eq(task.uid)).getSQL(ParamType.INLINED))
      }
    } finally {
      lastSetStep = null
    }
  }

  override fun setName(name: String?) = nextStep { it.set(TASK.NAME, name) }

  override fun setMilestone(isMilestone: Boolean) = nextStep { it.set(TASK.IS_MILESTONE, isMilestone) }

  override fun setPriority(priority: Task.Priority?) {
    TODO("Not yet implemented")
  }

  override fun setStart(start: GanttCalendar?) {
    TODO("Not yet implemented")
  }

  override fun setEnd(end: GanttCalendar?) {
    TODO("Not yet implemented")
  }

  override fun setDuration(length: TimeDuration?) {
    TODO("Not yet implemented")
  }

  override fun shift(shift: TimeDuration?) {
    TODO("Not yet implemented")
  }

  override fun setCompletionPercentage(percentage: Int) {
    TODO("Not yet implemented")
  }

  override fun setShape(shape: ShapePaint?) {
    TODO("Not yet implemented")
  }

  override fun setColor(color: Color?) {
    TODO("Not yet implemented")
  }

  override fun setWebLink(webLink: String?) {
    TODO("Not yet implemented")
  }

  override fun setNotes(notes: String?) {
    TODO("Not yet implemented")
  }

  override fun addNotes(notes: String?) {
    TODO("Not yet implemented")
  }

  override fun setExpand(expand: Boolean) {
    TODO("Not yet implemented")
  }

  override fun setCritical(critical: Boolean) {
    TODO("Not yet implemented")
  }

  override fun setTaskInfo(taskInfo: TaskInfo?) {
    TODO("Not yet implemented")
  }

  override fun setProjectTask(projectTask: Boolean) {
    TODO("Not yet implemented")
  }
}

private fun Task.logId(): String = "${uid}:${taskID}"

private const val H2_IN_MEMORY_URL = "jdbc:h2:mem:gantt-project-state;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true"
private const val DB_INIT_SCRIPT_PATH = "/sql/init-project-database.sql"
