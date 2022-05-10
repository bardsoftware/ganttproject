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
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.ProjectDatabase.TaskUpdateBuilder
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskInfo
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.SelectSelectStep
import org.jooq.UpdateSetMoreStep
import org.jooq.UpdateSetStep
import org.jooq.conf.ParamType
import org.jooq.impl.DSL
import org.jooq.impl.DSL.field
import java.awt.Color
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

  private class Query(
    val errorMessage: () -> String,
    val sqlStatementH2: String,
    val sqlStatementPostgres: String
  )

  /** Queries which belong to the current transaction. Null if each statement should be committed separately. */
  private var currentTxn: MutableList<Query>? = null
  private var localTxnId: Int = 1

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

  /** Execute queries and save their logs as a transaction with the specified ID. */
  private fun executeAndLog(queries: List<Query>, localTxnId: Int): Unit = withDSL({ "Failed to commit transaction" }) { dsl ->
    dsl.transaction { config ->
      val context = DSL.using(config)
      queries.forEach {
        try {
          context.execute(it.sqlStatementH2)
          context
            .insertInto(LOGRECORD)
            .set(LOGRECORD.LOCAL_TXN_ID, localTxnId)
            .set(LOGRECORD.SQL_STATEMENT, it.sqlStatementPostgres)
            .execute()
        } catch (e: Exception) {
          LOG.error("Failed to execute or log txnId={}\n {}", localTxnId, it.sqlStatementH2)
          throw ProjectDatabaseException(it.errorMessage(), e)
        }
      }
    }
  }

  /** Add a query to the current txn. Executes immediately if no transaction started. */
  private fun withLog(errorMessage: () -> String, buildQuery: (dsl: DSLContext) -> String) {
    val query = Query(errorMessage, buildQuery(DSL.using(SQLDialect.H2)), buildQuery(DSL.using(SQLDialect.POSTGRES)))
    currentTxn?.add(query) ?: executeAndLog(listOf(query), localTxnId).also { localTxnId++ }
  }

  private fun withLog(errorMessage: () -> String, h2Query: String, postgresQuery: String) {
    val query = Query(errorMessage, h2Query, postgresQuery)
    currentTxn?.add(query) ?: executeAndLog(listOf(query), localTxnId).also { localTxnId++ }
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

  override fun createTaskUpdateBuilder(task: Task): TaskUpdateBuilder = SqlTaskUpdateBuilder(task, this::update)

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
  override fun startTransaction() {
    if (currentTxn != null) throw ProjectDatabaseException("Previous transaction not committed")
    currentTxn = mutableListOf()
  }

  @Throws(ProjectDatabaseException::class)
  override fun commitTransaction() {
    try {
      executeAndLog(currentTxn ?: throw ProjectDatabaseException("No transaction started"), localTxnId)
      localTxnId++  // Increment only on success.
    } finally {
      currentTxn = null
    }
  }

  @Throws(ProjectDatabaseException::class)
  override fun fetchTransactions(startLocalTxnId: Int, limit: Int): List<XlogRecord> = withDSL(
    { "Failed to fetch log records starting with $startLocalTxnId" }) { dsl ->
    dsl
      .selectFrom(LOGRECORD)
      .where(LOGRECORD.LOCAL_TXN_ID.ge(startLocalTxnId).and(LOGRECORD.LOCAL_TXN_ID.lt(startLocalTxnId + limit)))
      .orderBy(LOGRECORD.LOCAL_TXN_ID, LOGRECORD.ID)
      .fetchGroups(LOGRECORD.LOCAL_TXN_ID, LOGRECORD.SQL_STATEMENT)
      .values
      .map { XlogRecord(it) }
  }

  override fun findTasks(whereExpression: String, lookupById: (Int)->Task?): List<Task> {
    return withDSL({"Failed to execute query $whereExpression"}) { dsl ->
      dsl.select(TASK.NUM).from(TASK).where(whereExpression).mapNotNull {
        lookupById(it.value1())
      }
    }
  }

  fun <T> SelectSelectStep<org.jooq.Record>.select(col: ColumnConsumer?): SelectSelectStep<org.jooq.Record> =
    col?.let { this.select(field(it.first.selectExpression, it.first.resultClass)!!.`as`(col.first.propertyId))} ?: this

  override fun mapTasks(vararg columnConsumer: ColumnConsumer) {
    withDSL { dsl ->
      var q: SelectSelectStep<out org.jooq.Record> = dsl.select(TASK.NUM)
      columnConsumer.forEach {
        q = q.select(field(it.first.selectExpression, it.first.resultClass).`as`(it.first.propertyId))
      }
      q.from(TASK).forEach {row  ->
        val taskNum = row[TASK.NUM]
        columnConsumer.forEach {
          it.second(taskNum, row[it.first.propertyId])
        }
      }

    }
  }

  /** Add update query and save its xlog in the current transaction. */
  @Throws(ProjectDatabaseException::class)
  internal fun update(h2Query: String, postgresQuery: String) = withLog({ "Failed to execute update" }, h2Query, postgresQuery)
}


class SqlTaskUpdateBuilder(private val task: Task,
                           private val onCommit: (String, String) -> Unit): TaskUpdateBuilder {
  private var lastSetStepH2: UpdateSetMoreStep<TaskRecord>? = null
  private var lastSetStepPostgres: UpdateSetMoreStep<TaskRecord>? = null

  private fun nextStep(step: (lastStep: UpdateSetStep<TaskRecord>) -> UpdateSetMoreStep<TaskRecord>) {
    lastSetStepH2 = step(lastSetStepH2 ?: DSL.using(SQLDialect.H2).update(TASK))
    lastSetStepPostgres = step(lastSetStepPostgres ?: DSL.using(SQLDialect.POSTGRES).update(TASK))
  }

  @Throws(ProjectDatabaseException::class)
  override fun commit() {
    if (lastSetStepH2 == null && lastSetStepPostgres == null) return
    val finalH2 = lastSetStepH2?.where(TASK.UID.eq(task.uid))?.getSQL(ParamType.INLINED)
      ?: error("Update step for H2 is null")
    val finalPostgres = lastSetStepPostgres?.where(TASK.UID.eq(task.uid))?.getSQL(ParamType.INLINED)
      ?: error("Update step for PostgreSQL is null")
    onCommit(finalH2, finalPostgres)
  }

  override fun setName(name: String?) = nextStep { it.set(TASK.NAME, name) }

  override fun setMilestone(isMilestone: Boolean) = nextStep { it.set(TASK.IS_MILESTONE, isMilestone) }

  override fun setPriority(priority: Task.Priority?) {
    TODO("Not yet implemented")
  }

  override fun setStart(start: GanttCalendar) = nextStep { it.set(TASK.START_DATE, start.toLocalDate()) }

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

const val SQL_PROJECT_DATABASE_OPTIONS = ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true"
private const val H2_IN_MEMORY_URL = "jdbc:h2:mem:gantt-project-state$SQL_PROJECT_DATABASE_OPTIONS"
private const val DB_INIT_SCRIPT_PATH = "/sql/init-project-database.sql"

private val LOG = GPLogger.create("ProjectDatabase")