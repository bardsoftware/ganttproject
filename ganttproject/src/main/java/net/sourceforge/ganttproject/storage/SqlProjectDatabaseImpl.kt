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
import biz.ganttproject.customproperty.CustomProperty
import biz.ganttproject.customproperty.CustomPropertyHolder
import biz.ganttproject.storage.db.Tables.*
import biz.ganttproject.storage.db.tables.records.TaskRecord
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.ProjectDatabase.TaskUpdateBuilder
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.util.ColorConvertion
import org.h2.jdbcx.JdbcDataSource
import org.jooq.*
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

    fun createInMemoryDatabase(logUpdateCallback: () -> Unit): ProjectDatabase {
      val dataSource = JdbcDataSource()
      dataSource.setURL(H2_IN_MEMORY_URL)
      val database = SqlProjectDatabaseImpl(dataSource)
      database.addLogUpdateCallback(logUpdateCallback)
      return database
    }
  }

  /** Queries which belong to the current transaction. Null if each statement should be committed separately. */
  private var currentTxn: TransactionImpl? = null
  private var localTxnId: Int = 1

  private val logUpdateCallbacks: MutableList<() -> Unit> = mutableListOf()

  /** Log update callbacks are invoked when a new log record is added. */
  fun addLogUpdateCallback(listener: () -> Unit) = logUpdateCallbacks.add(listener)

  private fun onLogUpdate() = logUpdateCallbacks.forEach {
    try {
      it.invoke()
    } catch (e: Exception) {
      LOG.error("Failed to execute update callback", e)
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

  /** Execute queries and save their logs as a transaction with the specified ID. */
  internal fun executeAndLog(queries: List<Query>, localTxnId: Int): Unit = withDSL({ "Failed to commit transaction" }) { dsl ->
    dsl.transaction { config ->
      val context = DSL.using(config)
      queries.forEach {
        try {
          LOG.debug("SQL: ${it.sqlStatementH2}")
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
    onLogUpdate()
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
      dataSource.connection.use { it.createStatement().execute("shutdown") }
    } catch (e: Exception) {
      throw ProjectDatabaseException("Failed to shutdown the database", e)
    }
  }

  @Throws(ProjectDatabaseException::class)
  override fun startTransaction(title: String): ProjectDatabaseTxn {
    if (currentTxn != null) throw ProjectDatabaseException("Previous transaction not committed: $currentTxn")
    return TransactionImpl(this, title).also {
      currentTxn = it
    }
  }

  @Throws(ProjectDatabaseException::class)
  internal fun commitTransaction(txn: TransactionImpl) {
    try {
      if (txn.statements.isEmpty()) return
      executeAndLog(txn.statements, localTxnId)
      localTxnId++  // Increment only on success.
    } finally {
      currentTxn = null
    }
  }

  @Throws(ProjectDatabaseException::class)
  override fun fetchTransactions(startLocalTxnId: Int, limit: Int): List<XlogRecord> = withDSL(
    { "Failed to fetch log records starting with $startLocalTxnId" }) { dsl ->
    //println(dsl.selectFrom(LOGRECORD).toList())
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

  fun SelectSelectStep<Record>.select(col: ColumnConsumer?): SelectSelectStep<Record> =
    col?.let { this.select(field(it.first.selectExpression, it.first.resultClass)!!.`as`(col.first.propertyId))} ?: this

  override fun mapTasks(vararg columnConsumer: ColumnConsumer) {
    withDSL { dsl ->
      var q: SelectSelectStep<out Record> = dsl.select(TASK.NUM)
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
internal data class Query(
  val errorMessage: () -> String,
  val sqlStatementH2: String,
  val sqlStatementPostgres: String
)

class TransactionImpl(private val database: SqlProjectDatabaseImpl, private val title: String): ProjectDatabaseTxn {
  internal val statements = mutableListOf<Query>()

  override fun commit() {
    database.commitTransaction(this)
  }

  internal fun add(query: Query) {
    statements.add(query)
  }

  override fun toString(): String {
    return "TransactionImpl(title='$title', statements=$statements)\n\n"
  }


}

/**
 * Creates SQL statements for updating custom property records.
 */
internal class SqlTaskCustomPropertiesUpdateBuilder(
  private val task: Task, private val onCommit: (String, String) -> Unit) {
  internal var commit: () -> Unit = {}
  internal fun setCustomProperties(customProperties: CustomPropertyHolder) {
    val h2statements = mutableListOf<String>()
    val postgresStatements = mutableListOf<String>()

    h2statements.add(generateDeleteStatement(DSL.using(SQLDialect.H2), customProperties))
    postgresStatements.add(generateDeleteStatement(DSL.using(SQLDialect.POSTGRES), customProperties))

    h2statements.addAll(generateMergeStatements(customProperties.customProperties) {DSL.using(SQLDialect.H2)})
    postgresStatements.addAll(generateMergeStatements(customProperties.customProperties) {DSL.using(SQLDialect.POSTGRES)})
    commit = {
      h2statements.zip(postgresStatements).forEach { onCommit(it.first, it.second) }
      println(h2statements)
      println(postgresStatements)
    }
  }

  private fun generateDeleteStatement(dsl: DSLContext, customProperties: CustomPropertyHolder): String =
    dsl.deleteFrom(TASKCUSTOMCOLUMN)
      .where(TASKCUSTOMCOLUMN.UID.eq(task.uid))
      .and(TASKCUSTOMCOLUMN.COLUMN_ID.notIn(customProperties.customProperties.map { it.definition.id }))
      .getSQL(ParamType.INLINED)

  private fun generateMergeStatements(customProperties: List<CustomProperty>, dsl: ()->DSLContext) =
    customProperties.map {
      dsl().mergeInto(TASKCUSTOMCOLUMN).using(DSL.selectOne())
        .on(TASKCUSTOMCOLUMN.UID.eq(task.uid)).and(TASKCUSTOMCOLUMN.COLUMN_ID.eq(it.definition.id))
        .whenMatchedThenUpdate().set(TASKCUSTOMCOLUMN.COLUMN_VALUE, it.valueAsString)
        .whenNotMatchedThenInsert(TASKCUSTOMCOLUMN.UID, TASKCUSTOMCOLUMN.COLUMN_ID, TASKCUSTOMCOLUMN.COLUMN_VALUE)
        .values(task.uid, it.definition.id, it.valueAsString)
        .getSQL(ParamType.INLINED)
    }
}
class SqlTaskUpdateBuilder(private val task: Task,
                           private val onCommit: (String, String) -> Unit): TaskUpdateBuilder {
  private var lastSetStepH2: UpdateSetMoreStep<TaskRecord>? = null
  private var lastSetStepPostgres: UpdateSetMoreStep<TaskRecord>? = null

  private val customPropertiesUpdater = SqlTaskCustomPropertiesUpdateBuilder(task, onCommit)
  private fun nextStep(step: (lastStep: UpdateSetStep<TaskRecord>) -> UpdateSetMoreStep<TaskRecord>) {
    lastSetStepH2 = step(lastSetStepH2 ?: DSL.using(SQLDialect.H2).update(TASK))
    lastSetStepPostgres = step(lastSetStepPostgres ?: DSL.using(SQLDialect.POSTGRES).update(TASK))
  }

  @Throws(ProjectDatabaseException::class)
  override fun commit() {
    val finalH2 = lastSetStepH2?.where(TASK.UID.eq(task.uid))?.getSQL(ParamType.INLINED)
    val finalPostgres = lastSetStepPostgres?.where(TASK.UID.eq(task.uid))?.getSQL(ParamType.INLINED)
    if (finalH2 != null && finalPostgres != null) {
      onCommit(finalH2, finalPostgres)
    }
    customPropertiesUpdater.commit()
  }

  override fun setName(name: String?) = nextStep { it.set(TASK.NAME, name) }

  override fun setMilestone(isMilestone: Boolean) = nextStep { it.set(TASK.IS_MILESTONE, isMilestone) }

  override fun setPriority(priority: Task.Priority?) {
    if (priority != null) {
      nextStep { it.set(TASK.PRIORITY, priority.persistentValue) }
    }
  }

  override fun setStart(start: GanttCalendar) = nextStep { it.set(TASK.START_DATE, start.toLocalDate()) }

  override fun setEnd(end: GanttCalendar?) {
    // intentionally empty, we do not store the end date
  }

  override fun setDuration(length: TimeDuration) = nextStep { it.set(TASK.DURATION, length.length) }

  override fun setCompletionPercentage(percentage: Int) = nextStep { it.set(TASK.COMPLETION, percentage) }

  override fun setShape(shape: ShapePaint?) = nextStep { it.set(TASK.SHAPE, shape?.array) }

  override fun setColor(color: Color?) = nextStep { it.set(TASK.COLOR, color?.let {ColorConvertion.getColor(it)}) }

  override fun setCost(cost: Task.Cost) {
    nextStep { it.set(TASK.IS_COST_CALCULATED, cost.isCalculated) }
    nextStep { it.set(TASK.COST_MANUAL_VALUE, cost.manualValue) }
  }

  override fun setCustomProperties(customProperties: CustomPropertyHolder) {
    customPropertiesUpdater.setCustomProperties(customProperties)
  }

  override fun setWebLink(webLink: String?) = nextStep { it.set(TASK.WEB_LINK, webLink) }


  override fun setNotes(notes: String?) = nextStep { it.set(TASK.NOTES, notes) }

  override fun setExpand(expand: Boolean) {
    // intentionally empty: we do not keep the expansion state in the task properties
  }

  override fun setCritical(critical: Boolean) {
    // TODO("Not yet implemented")
  }

  override fun setProjectTask(projectTask: Boolean) = nextStep { it.set(TASK.IS_PROJECT_TASK, projectTask) }
}

private fun Task.logId(): String = "${uid}:${taskID}"

const val SQL_PROJECT_DATABASE_OPTIONS = ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true"
private const val H2_IN_MEMORY_URL = "jdbc:h2:mem:gantt-project-state$SQL_PROJECT_DATABASE_OPTIONS"
private const val DB_INIT_SCRIPT_PATH = "/sql/init-project-database.sql"

private val LOG = GPLogger.create("ProjectDatabase")