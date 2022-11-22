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
import kotlinx.serialization.json.Json
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.sql.DataSource

class SqlProjectDatabaseImpl(private val dataSource: DataSource) : ProjectDatabase {
  companion object Factory {
    fun createInMemoryDatabase(): ProjectDatabase {
      val dataSource = JdbcDataSource()
      dataSource.setURL(H2_IN_MEMORY_URL)
      return SqlProjectDatabaseImpl(dataSource)
    }
  }

  /** Queries which belong to the current transaction. Null if each statement should be committed separately. */
  private var currentTxn: TransactionImpl? = null
  private var localTxnId: Int = -1
  private var baseTxnId: String = ""
  /** For a range R of local txn ids [i_1, i_n) which were completed between a transition from a sync point s1 to s2,
   * maps s1 to R.
   */
  private val syncTxnMap = mutableMapOf<String, IntRange>()
  private var areEventsEnabled: Boolean = true

  private var externalUpdatesListener: ProjectDatabaseExternalUpdateListener = {}

  override fun addExternalUpdatesListener(listener: ProjectDatabaseExternalUpdateListener) {
    externalUpdatesListener = listener
  }

  /**
   * Applies updates from Colloboque
   */
  @Throws(ProjectDatabaseException::class)
  override fun applyUpdate(logRecords: List<XlogRecord>, baseTxnId: String, targetTxnId: String) {
    withDSL { dsl ->
      dsl.transaction { config ->
        val context = DSL.using(config)
        logRecords.forEach { logRecord ->
          val statements = logRecord.colloboqueOperations.map { generateSqlStatement(dsl, it) }
          statements.forEach {
            try {
              context.execute(it)
            } catch (e: Exception) {
              val errorMessage = "Failed to execute query from Colloboque: $it"
              LOG.error(errorMessage)
              throw ProjectDatabaseException(errorMessage, e)
            }
          }
        }
      }
    }
    syncTxnMap[targetTxnId] = syncTxnMap[baseTxnId]!!.endInclusive.let { IntRange(it, it) }
    this.baseTxnId = targetTxnId
    if (logRecords.isNotEmpty()) {
      externalUpdatesListener()
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
  private fun executeAndLog(queries: List<SqlQuery>, localTxnId: Int): Unit = withDSL({ "Failed to commit transaction" }) { dsl ->
    dsl.transaction { config ->
      val context = DSL.using(config)
      queries.forEach {
        try {
          LOG.debug("SQL: ${it.sqlStatementH2}")
          context.execute(it.sqlStatementH2)
          if (isLogStarted) {
            context
              .insertInto(LOGRECORD)
              .set(LOGRECORD.LOCAL_TXN_ID, localTxnId)
              .set(LOGRECORD.OPERATION_DTO_JSON, Json.encodeToString(OperationDto.serializer(), it.colloboqueOperationDto))
              .execute()
          }
        } catch (e: Exception) {
          val errorMessage = "Failed to execute or log txnId=$localTxnId\n ${it.sqlStatementH2}"
          LOG.error(errorMessage)
          throw ProjectDatabaseException(errorMessage, e)
        }
      }
    }
  }

  /** Add a query to the current txn. Executes immediately if no transaction started. */
  private fun withLog(buildQuery: (dsl: DSLContext) -> String,
                      buildUndoQuery: (dsl: DSLContext) -> String,
                      colloboqueOperationDto: OperationDto,
                      colloboqueUndoOperationDto: OperationDto) {
    val query = SqlQuery(buildQuery(DSL.using(SQLDialect.H2)), colloboqueOperationDto)
    val undoQuery = SqlQuery(buildUndoQuery(DSL.using(SQLDialect.H2)), colloboqueUndoOperationDto)
    currentTxn?.add(query, undoQuery) ?: executeAndLog(listOf(query), localTxnId).also {
      incrementLocalTxnId()
    }
  }

  private fun withLog(queries: List<SqlQuery>, undoQueries: List<SqlUndoQuery>) {
    currentTxn?.add(queries, undoQueries) ?: executeAndLog(queries, localTxnId).also {
      incrementLocalTxnId()
    }
  }

  @Throws(ProjectDatabaseException::class)
  override fun init() {
    runScript(DB_INIT_SCRIPT_PATH)
    runScript(DB_INIT_SCRIPT_PATH2)
  }

  private fun runScript(path: String) {
    val scriptStream = javaClass.getResourceAsStream(path) ?: throw ProjectDatabaseException("Init script not found")
    try {
      val queries = String(scriptStream.readAllBytes(), Charsets.UTF_8)

      dataSource.connection.use { it.createStatement().execute(queries) }
    } catch (e: Exception) {
      throw ProjectDatabaseException("Failed to init the database", e)
    }
  }

  override fun startLog(baseTxnId: String) {
    localTxnId = 0
    this.baseTxnId = baseTxnId
    syncTxnMap[baseTxnId] = 0..0
  }

  private val isLogStarted get() = localTxnId >= 0

  override fun createTaskUpdateBuilder(task: Task): TaskUpdateBuilder = SqlTaskUpdateBuilder(task, this::update)

  @Throws(ProjectDatabaseException::class)
  override fun insertTask(task: Task) {
    val queryBuilder = { dsl: DSLContext -> buildInsertTaskQuery(dsl, task).getSQL(ParamType.INLINED) }
    val undoQueryBuilder = { dsl: DSLContext ->
      dsl
        .deleteFrom(TASK)
        .where(TASK.UID.eq(task.uid))
        .getSQL(ParamType.INLINED)
    }
    val insertDto = buildInsertTaskDto(task)
    val deleteDto = OperationDto.DeleteOperationDto(
      TASK.name.lowercase(),
      listOf(Triple(TASK.UID.name, BinaryPred.EQ, task.uid))
    )
    withLog(queryBuilder, undoQueryBuilder, insertDto, deleteDto)
  }

  @Throws(ProjectDatabaseException::class)
  override fun insertTaskDependency(taskDependency: TaskDependency) {
    val queryBuilder = { dsl: DSLContext ->
      dsl
        .insertInto(TASKDEPENDENCY)
        .set(TASKDEPENDENCY.DEPENDEE_UID, taskDependency.dependee.uid)
        .set(TASKDEPENDENCY.DEPENDANT_UID, taskDependency.dependant.uid)
        .set(TASKDEPENDENCY.TYPE, taskDependency.constraint.type.persistentValue)
        .set(TASKDEPENDENCY.LAG, taskDependency.difference)
        .set(TASKDEPENDENCY.HARDNESS, taskDependency.hardness.identifier)
        .getSQL(ParamType.INLINED)
    }
    val undoQueryBuilder = { dsl: DSLContext ->
      dsl
        .deleteFrom(TASKDEPENDENCY)
        .where(TASKDEPENDENCY.DEPENDANT_UID.eq(taskDependency.dependant.uid)
          .and(TASKDEPENDENCY.DEPENDEE_UID.eq(taskDependency.dependee.uid)))
        .getSQL(ParamType.INLINED)

    }
    val insertDto = OperationDto.InsertOperationDto(
      TASKDEPENDENCY.name.lowercase(),
      mapOf(
        TASKDEPENDENCY.DEPENDEE_UID.name to taskDependency.dependee.uid,
        TASKDEPENDENCY.DEPENDANT_UID.name to taskDependency.dependant.uid,
        TASKDEPENDENCY.TYPE.name to taskDependency.constraint.type.persistentValue,
        TASKDEPENDENCY.LAG.name to taskDependency.difference.toString(),
        TASKDEPENDENCY.HARDNESS.name to taskDependency.hardness.identifier
      )
    )
    val deleteDto = OperationDto.DeleteOperationDto(
      TASKDEPENDENCY.name.lowercase(),
      listOf(
        Triple(TASKDEPENDENCY.DEPENDANT_UID.name, BinaryPred.EQ, taskDependency.dependant.uid),
        Triple(TASKDEPENDENCY.DEPENDEE_UID.name, BinaryPred.EQ, taskDependency.dependee.uid)
      )
    )
    withLog(queryBuilder, undoQueryBuilder, insertDto, deleteDto)
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
  internal fun commitTransaction(queries: List<SqlQuery>) {
    try {
      if (queries.isEmpty()) return
      executeAndLog(queries, localTxnId)
      // Increment only on success.
      incrementLocalTxnId()
    } finally {
      currentTxn = null
    }
  }

  private fun incrementLocalTxnId() {
    syncTxnMap[baseTxnId]?.let {
      localTxnId++
      syncTxnMap[baseTxnId] = it.start.rangeTo(it.endInclusive + 1)
    }
  }

  override val outgoingTransactions: List<XlogRecord> get() {
    if (baseTxnId.isBlank()) {
      return emptyList()
    }
    val outgoingRange = syncTxnMap[baseTxnId]!!
    LOG.debug("Outgoing txns: from base txn={} local range={}", baseTxnId, outgoingRange)
    return fetchTransactions(outgoingRange.start, outgoingRange.endInclusive - outgoingRange.start)
  }

  @Throws(ProjectDatabaseException::class)
  override fun fetchTransactions(startLocalTxnId: Int, limit: Int): List<XlogRecord> = withDSL(
    { "Failed to fetch log records starting with $startLocalTxnId" }) { dsl ->
    // println(dsl.selectFrom(LOGRECORD).toList())
    dsl
      .selectFrom(LOGRECORD)
      .where(LOGRECORD.LOCAL_TXN_ID.ge(startLocalTxnId).and(LOGRECORD.LOCAL_TXN_ID.lt(startLocalTxnId + limit)))
      .orderBy(LOGRECORD.LOCAL_TXN_ID, LOGRECORD.ID)
      .fetchGroups(LOGRECORD.LOCAL_TXN_ID, LOGRECORD.OPERATION_DTO_JSON)
      .values
      .map { XlogRecord(it.map { str -> Json.decodeFromString(OperationDto.serializer(), str) }) }
  }

  override fun findTasks(whereExpression: String, lookupById: (Int)->Task?): List<Task> {
    return withDSL({"Failed to execute query $whereExpression"}) { dsl ->
      dsl.select(TASK.NUM).from(TASK).where(whereExpression).mapNotNull {
        lookupById(it.value1())
      }
    }
  }

  @Throws(ProjectDatabaseException::class)
  override fun readAllTasks(): List<TaskRecord> {
    return withDSL { dsl ->
      dsl.selectFrom(TASK).fetch().toList()
    }
  }

  fun SelectSelectStep<Record>.select(col: ColumnConsumer?): SelectSelectStep<Record> =
    col?.let { this.select(field(it.first.selectExpression, it.first.resultClass)!!.`as`(col.first.propertyId))} ?: this

  override fun mapTasks(vararg columnConsumer: ColumnConsumer) {
    withDSL { dsl ->
      var q: SelectSelectStep<out Record> = dsl.select(TASKVIEWFORCOMPUTEDCOLUMNS.ID)
      columnConsumer.forEach {
        q = q.select(field(it.first.selectExpression, it.first.resultClass).`as`(it.first.propertyId))
      }
      q.from(TASKVIEWFORCOMPUTEDCOLUMNS).forEach { row  ->
        val taskNum = row[TASKVIEWFORCOMPUTEDCOLUMNS.ID]
        columnConsumer.forEach {
          it.second(taskNum, row[it.first.propertyId])
        }
      }

    }
  }

  override fun validateColumnConsumer(columnConsumer: ColumnConsumer) {
    withDSL { dsl ->
      dsl.select(field(columnConsumer.first.selectExpression).cast(columnConsumer.first.resultClass))
        .from(TASKVIEWFORCOMPUTEDCOLUMNS)
        .limit(1).also {
        it.execute()
      }
    }
  }

  /** Add update query and save its xlog in the current transaction. */
  @Throws(ProjectDatabaseException::class)
  internal fun update(queries: List<SqlQuery>, undoQueries: List<SqlUndoQuery>) = withLog(queries, undoQueries)
}

data class SqlQuery(
  val sqlStatementH2: String,
  val colloboqueOperationDto: OperationDto
)

typealias SqlUndoQuery = SqlQuery

class TransactionImpl(private val database: SqlProjectDatabaseImpl, private val title: String): ProjectDatabaseTxn {
  private val statements = mutableListOf<SqlQuery>()
  private val undoStatements = mutableListOf<SqlQuery>()

  private var isCommitted: Boolean = false

  override fun commit() {
    if (isCommitted) throw ProjectDatabaseException("Transaction is already committed")
    database.commitTransaction(statements)
    isCommitted = true
  }

  override fun undo() {
    if (!isCommitted) throw ProjectDatabaseException("Cannot undo uncommitted transaction")
    database.commitTransaction(undoStatements.reversed())
  }

  override fun redo() {
    if (!isCommitted) throw ProjectDatabaseException("Cannot redo uncommitted transaction")
    database.commitTransaction(statements)
  }

  internal fun add(query: SqlQuery, undoQuery: SqlQuery) {
    if (isCommitted) throw ProjectDatabaseException("Txn was already committed")
    statements.add(query)
    undoStatements.add(undoQuery)
  }

  internal fun add(queries: List<SqlQuery>, undoQueries: List<SqlUndoQuery>) {
    if (isCommitted) throw ProjectDatabaseException("Txn was already committed")
    statements.addAll(queries)
    undoStatements.addAll(undoQueries.reversed())
  }

  override fun toString(): String {
    return "TransactionImpl(title='$title', statements=$statements)\n\n"
  }
}

/**
 * Creates SQL statements for updating custom property records.
 */
internal class SqlTaskCustomPropertiesUpdateBuilder(
  private val task: Task, private val onCommit: (List<SqlQuery>, List<SqlUndoQuery>) -> Unit) {
  internal var commit: () -> Unit = {}

  private fun generateStatements(customProperties: CustomPropertyHolder, isUndoOperation: Boolean): List<SqlQuery> {
    val h2statements = mutableListOf<String>()
    val colloboqueUpdateDtos = mutableListOf<OperationDto>()

    val generateDeleteFnForH2 = {
      if (isUndoOperation) generateDeleteStatementAllColumns(DSL.using(SQLDialect.H2)) else generateDeleteStatement(DSL.using(SQLDialect.H2), customProperties)
    }
    val generateDeleteFnForColloboque = {
      if (isUndoOperation) generateDeleteDtoAllColumns() else generateDeleteDto(customProperties)
    }
    h2statements.add(generateDeleteFnForH2())
    colloboqueUpdateDtos.add(generateDeleteFnForColloboque())

    h2statements.addAll(generateMergeStatements(customProperties.customProperties) {DSL.using(SQLDialect.H2)})
    colloboqueUpdateDtos.addAll(generateMergeDtos(customProperties.customProperties))
    return h2statements.zip(colloboqueUpdateDtos).map { SqlQuery(it.first, it.second) }
  }

  internal fun setCustomProperties(oldCustomProperties: CustomPropertyHolder, newCustomProperties: CustomPropertyHolder) {
    commit = {
      onCommit(
        generateStatements(newCustomProperties, isUndoOperation = false),
        generateStatements(oldCustomProperties, isUndoOperation = true)
      )
    }
  }

  private fun generateDeleteStatement(dsl: DSLContext, customProperties: CustomPropertyHolder): String =
    dsl.deleteFrom(TASKCUSTOMCOLUMN)
      .where(TASKCUSTOMCOLUMN.UID.eq(task.uid))
      .and(TASKCUSTOMCOLUMN.COLUMN_ID.notIn(customProperties.customProperties.map { it.definition.id }))
      .getSQL(ParamType.INLINED)

  private fun generateDeleteDto(customProperties: CustomPropertyHolder): OperationDto.DeleteOperationDto =
    OperationDto.DeleteOperationDto(
      TASKCUSTOMCOLUMN.name.lowercase(),
      listOf(
        Triple(TASKCUSTOMCOLUMN.UID.name, BinaryPred.EQ, task.uid),
      ),
      listOf(
        Triple(TASKCUSTOMCOLUMN.COLUMN_ID.name, RangePred.NOT_IN, customProperties.customProperties.map { it.definition.id } )
      )
    )

  private fun generateDeleteStatementAllColumns(dsl: DSLContext): String =
    dsl.deleteFrom(TASKCUSTOMCOLUMN)
      .where(TASKCUSTOMCOLUMN.UID.eq(task.uid))
      .getSQL(ParamType.INLINED)

  private fun generateDeleteDtoAllColumns(): OperationDto.DeleteOperationDto =
    OperationDto.DeleteOperationDto(
      TASKCUSTOMCOLUMN.name.lowercase(),
      listOf(
        Triple(TASKCUSTOMCOLUMN.UID.name, BinaryPred.EQ, task.uid),
      )
    )

  private fun generateMergeStatements(customProperties: List<CustomProperty>, dsl: ()->DSLContext) =
    customProperties.map {
      dsl().mergeInto(TASKCUSTOMCOLUMN).using(DSL.selectOne())
        .on(TASKCUSTOMCOLUMN.UID.eq(task.uid)).and(TASKCUSTOMCOLUMN.COLUMN_ID.eq(it.definition.id))
        .whenMatchedThenUpdate().set(TASKCUSTOMCOLUMN.COLUMN_VALUE, it.valueAsString)
        .whenNotMatchedThenInsert(TASKCUSTOMCOLUMN.UID, TASKCUSTOMCOLUMN.COLUMN_ID, TASKCUSTOMCOLUMN.COLUMN_VALUE)
        .values(task.uid, it.definition.id, it.valueAsString)
        .getSQL(ParamType.INLINED)
    }

  private fun generateMergeDtos(customProperties: List<CustomProperty>) =
    customProperties.map {
      OperationDto.MergeOperationDto(
        TASKCUSTOMCOLUMN.name.lowercase(),
        listOf(
          Triple(TASKCUSTOMCOLUMN.UID.name, BinaryPred.EQ, task.uid),
          Triple(TASKCUSTOMCOLUMN.COLUMN_ID.name, BinaryPred.EQ, it.definition.id)
        ),
        listOf(),
        mapOf(
          TASKCUSTOMCOLUMN.COLUMN_VALUE.name to it.valueAsString
        ),
        mapOf(
          TASKCUSTOMCOLUMN.UID.name to task.uid,
          TASKCUSTOMCOLUMN.COLUMN_ID.name to it.definition.id,
          TASKCUSTOMCOLUMN.COLUMN_VALUE.name to it.valueAsString
        )
      )
    }
}


class SqlTaskUpdateBuilder(private val task: Task,
                           private val onCommit: (List<SqlQuery>, List<SqlUndoQuery>) -> Unit): TaskUpdateBuilder {
  private var lastSetStepH2: UpdateSetMoreStep<TaskRecord>? = null
  private var updateDtoColloboque: OperationDto.UpdateOperationDto? = null

  private var lastUndoSetStepH2: UpdateSetMoreStep<TaskRecord>? = null
  private var undoUpdateDtoColloboque: OperationDto.UpdateOperationDto? = null

  private val customPropertiesUpdater = SqlTaskCustomPropertiesUpdateBuilder(task, onCommit)

  private fun nextStep(stepH2: (lastStep: UpdateSetStep<TaskRecord>) -> UpdateSetMoreStep<TaskRecord>,
                       stepColloboque: (lastStep: OperationDto.UpdateOperationDto) -> OperationDto.UpdateOperationDto) {
    lastSetStepH2 = stepH2(lastSetStepH2 ?: DSL.using(SQLDialect.H2).update(TASK))
    updateDtoColloboque = stepColloboque(updateDtoColloboque ?: OperationDto.UpdateOperationDto(TASK.name, mutableListOf(), mutableListOf(), mutableMapOf()))
  }

  private fun nextUndoStep(stepH2: (lastStep: UpdateSetStep<TaskRecord>) -> UpdateSetMoreStep<TaskRecord>,
                           stepColloboque: (lastStep: OperationDto.UpdateOperationDto) -> OperationDto.UpdateOperationDto) {
    lastUndoSetStepH2 = stepH2(lastUndoSetStepH2 ?: DSL.using(SQLDialect.H2).update(TASK))
    undoUpdateDtoColloboque = stepColloboque(undoUpdateDtoColloboque ?: OperationDto.UpdateOperationDto(TASK.name, mutableListOf(), mutableListOf(), mutableMapOf()))
  }

  private fun <T> appendUpdate(field: TableField<TaskRecord, T>, oldValue: T?, newValue: T?) {
    nextStep(stepH2 = { it.set(field, newValue) },
      stepColloboque =  { dto ->
        dto.newValues[field.name] = newValue.toString()
        return@nextStep dto
      }
    )
    nextUndoStep(stepH2 = { it.set(field, oldValue) },
      stepColloboque = { dto ->
        dto.newValues[field.name] = oldValue.toString()
        return@nextUndoStep dto
      }
    )
  }


  @Throws(ProjectDatabaseException::class)
  override fun commit() {
    val finalH2 = lastSetStepH2?.where(TASK.UID.eq(task.uid))?.getSQL(ParamType.INLINED)
    updateDtoColloboque?.updateBinaryConditions?.add(Triple(TASK.UID.name, BinaryPred.EQ, task.uid))
    val finalDtoColloboque = updateDtoColloboque

    val finalUndoH2 = lastUndoSetStepH2?.where(TASK.UID.eq(task.uid))?.getSQL(ParamType.INLINED)
    undoUpdateDtoColloboque?.updateBinaryConditions?.add(Triple(TASK.UID.name, BinaryPred.EQ, task.uid))
    val finalUndoDtoColloboque = undoUpdateDtoColloboque

    if (finalH2 != null && finalDtoColloboque != null) {
      val undoQueries = if (finalUndoH2 != null && finalUndoDtoColloboque != null) {
        listOf(SqlUndoQuery(finalUndoH2, finalUndoDtoColloboque))
      } else {
        emptyList()
      }
      onCommit(listOf(SqlQuery(finalH2, finalDtoColloboque)), undoQueries)
    }
    customPropertiesUpdater.commit()
  }

  override fun setName(oldName: String?, newName: String?) = appendUpdate(TASK.NAME, oldName, newName)

  override fun setMilestone(oldValue: Boolean, newValue: Boolean) =
    appendUpdate(TASK.IS_MILESTONE, oldValue, newValue)

  override fun setPriority(oldValue: Task.Priority?, newValue: Task.Priority?) =
    appendUpdate(TASK.PRIORITY, oldValue?.persistentValue, newValue?.persistentValue)

  override fun setStart(oldValue: GanttCalendar, newValue: GanttCalendar) =
    appendUpdate(TASK.START_DATE, oldValue.toLocalDate(), newValue.toLocalDate())

  override fun setDuration(oldValue: TimeDuration, newValue: TimeDuration) =
    appendUpdate(TASK.DURATION, oldValue.length, newValue.length)

  override fun setCompletionPercentage(oldValue: Int, newValue: Int) =
    appendUpdate(TASK.COMPLETION, oldValue, newValue)

  override fun setShape(oldValue: ShapePaint?, newValue: ShapePaint?) =
    appendUpdate(TASK.SHAPE, oldValue?.array, newValue?.array)

  override fun setColor(oldValue: Color?, newValue: Color?) =
    appendUpdate(TASK.COLOR, oldValue?.let(ColorConvertion::getColor), newValue?.let(ColorConvertion::getColor))

  override fun setCost(oldValue: Task.Cost, newValue: Task.Cost) {
    appendUpdate(TASK.IS_COST_CALCULATED, oldValue.isCalculated, newValue.isCalculated)
    appendUpdate(TASK.COST_MANUAL_VALUE, oldValue.manualValue, newValue.manualValue)
  }

  override fun setCustomProperties(
    oldCustomProperties: CustomPropertyHolder,
    newCustomProperties: CustomPropertyHolder
  ) {
    customPropertiesUpdater.setCustomProperties(oldCustomProperties, newCustomProperties)
  }

  override fun setWebLink(oldValue: String?, newValue: String?) = appendUpdate(TASK.WEB_LINK, oldValue, newValue)


  override fun setNotes(oldValue: String?, newValue: String?) = appendUpdate(TASK.NOTES, oldValue, newValue)

  override fun setCritical(oldValue: Boolean, newValue: Boolean) {
    // TODO("Not yet implemented")
  }

  override fun setProjectTask(oldValue: Boolean, newValue: Boolean) =
    appendUpdate(TASK.IS_PROJECT_TASK, oldValue, newValue)
}

private fun Task.logId(): String = "${uid}:${taskID}"

const val SQL_PROJECT_DATABASE_OPTIONS = ";DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true"
private const val H2_IN_MEMORY_URL = "jdbc:h2:mem:gantt-project-state$SQL_PROJECT_DATABASE_OPTIONS"
private const val DB_INIT_SCRIPT_PATH = "/sql/init-project-database.sql"
private const val DB_INIT_SCRIPT_PATH2 = "/sql/init-project-database-step2.sql"
private val LOG = GPLogger.create("ProjectDatabase")
