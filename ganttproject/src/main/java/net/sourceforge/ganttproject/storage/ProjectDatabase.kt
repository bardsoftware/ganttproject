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

import biz.ganttproject.app.Localizer
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import biz.ganttproject.customproperty.CustomPropertyHolder
import biz.ganttproject.customproperty.CustomPropertyManager
import biz.ganttproject.customproperty.SimpleSelect
import biz.ganttproject.storage.db.tables.records.TaskRecord
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import org.h2.jdbc.JdbcException
import java.awt.Color

private val COLUMN_NOT_FOUND = "Column.(.*).not.found"
private val SYNTAX_ERROR_PREFIX = """Syntax error in SQL statement """"
private val DATA_CONVERSION_ERROR = """Data conversion error converting "(.*).to.(.*)""""
private val DATA_CONVERSION_ERROR_PREFIX = """Data conversion error converting""""
open class ProjectDatabaseException: Exception {
  constructor(message: String): super(message)
  constructor(message: String, cause: Throwable): super(message, cause)

  fun formatReason(i18n: Localizer): String {
    this.cause?.let {
      if (it.cause is JdbcException) {
        val message = (it.cause as Throwable).message!!
        COLUMN_NOT_FOUND.toRegex().find(message)?.let { matchResult ->
          return if (matchResult.groups.size > 1) {
            i18n.formatText("expression.validation.columnNotFound", matchResult.groups[1]?.value ?: "")
          } else i18n.formatText("expression.validation.columnNotFound")
        }
        if (message.startsWith(SYNTAX_ERROR_PREFIX)) {
          val posMarker = message.indexOf("[*]")
          return if (posMarker != -1) {
            val truncatedMsg = message.substring(SYNTAX_ERROR_PREFIX.length until posMarker)
            val posWhereExpressionStart = truncatedMsg.lastIndexOf('(')
            i18n.formatText("expression.validation.syntaxErrorAtPosition", truncatedMsg.length - posWhereExpressionStart)
          } else i18n.formatText("expression.validation.syntax", message.substring(SYNTAX_ERROR_PREFIX.length))
        }
        DATA_CONVERSION_ERROR.toRegex().find(message)?.let { matchResult ->
          return if (matchResult.groups.size >= 3) {
            i18n.formatText(
              "expression.validation.resultType", matchResult.groups[2]!!.value, matchResult.groups[1]!!.value
            )
          } else i18n.formatText("expression.validation.resultTypeNoArgs")
        }
        if (message.startsWith(DATA_CONVERSION_ERROR_PREFIX)) {
          return i18n.formatText("expression.validation.resultTypeNoArgs")
        }
      }
    }
    return i18n.formatText("expression.validation.invalidExpression")
  }
}

interface ProjectDatabaseTxn {
  @Throws(ProjectDatabaseException::class)
  fun commit()

  @Throws(ProjectDatabaseException::class)
  fun undo()

  @Throws(ProjectDatabaseException::class)
  fun redo()
  fun rollback()
}

class DummyTxn: ProjectDatabaseTxn {
  override fun commit() {}
  override fun undo() {}
  override fun redo() {}
  override fun rollback() {}
}

typealias ColumnConsumer = Pair<SimpleSelect, (Int, Any?)->Unit>
typealias ProjectDatabaseExternalUpdateListener = () -> Unit

/** Storage for holding the current state of a Gantt project. */
interface ProjectDatabase {
  /** Build and execute an update query. */
  interface TaskUpdateBuilder {
    /** Commit task update. */
    @Throws(ProjectDatabaseException::class)
    fun commit()

    fun setColor(oldValue: Color?, newValue: Color?)
    fun setCompletionPercentage(oldValue: Int, newValue: Int)
    fun setCost(oldValue: Task.Cost, newValue: Task.Cost)
    fun setCritical(oldValue: Boolean, newValue: Boolean)
    fun setCustomProperties(oldCustomProperties: CustomPropertyHolder, newCustomProperties: CustomPropertyHolder)
    fun setDuration(oldValue: TimeDuration, newValue: TimeDuration)
    fun setMilestone(oldValue: Boolean, newValue: Boolean)
    fun setName(oldName: String?, newName: String?)
    fun setNotes(oldValue: String?, newValue: String?)
    fun setPriority(oldValue: Task.Priority?, newValue: Task.Priority?)
    fun setProjectTask(oldValue: Boolean, newValue: Boolean)
    fun setShape(oldValue: ShapePaint?, newValue: ShapePaint?)
    fun setStart(oldValue: GanttCalendar, newValue: GanttCalendar)
    fun setEnd(oldValue: GanttCalendar?, newValue: GanttCalendar)
    fun setWebLink(oldValue: String?, newValue: String?)

    fun interface Factory {
      fun createTaskUpdateBuilder(task: Task): TaskUpdateBuilder
    }
  }

  /** Initialize the database. */
  @Throws(ProjectDatabaseException::class)
  fun init()

  @Throws(ProjectDatabaseException::class)
  fun startLog(baseTxnId: BaseTxnId)

  fun createTaskUpdateBuilder(task: Task): TaskUpdateBuilder

  /** Insert the task. */
  @Throws(ProjectDatabaseException::class)
  fun insertTask(task: Task)

  /** Insert the task dependency. */
  @Throws(ProjectDatabaseException::class)
  fun insertTaskDependency(taskDependency: TaskDependency)

  /** Close connections and release the resources. */
  @Throws(ProjectDatabaseException::class)
  fun shutdown()

  /**
   * Collect queries received after txn start and commit them all at once.
   * Once committed, undo & redo actions are supported by the transaction.
   */
  @Throws(ProjectDatabaseException::class)
  fun startTransaction(title: String = ""): ProjectDatabaseTxn

  /** Fetch transactions starting with the specified transaction id. */
  @Throws(ProjectDatabaseException::class)
  fun fetchTransactions(startLocalTxnId: Int = 0, limit: Int): List<XlogRecord>

  val outgoingTransactions: List<XlogRecord>
  /** Run a query with the given `whereExpression` against the Task table.
   * The query results are converted to Task instances with `lookupById`
   */
  @Throws(ProjectDatabaseException::class)
  fun findTasks(whereExpression: String, lookupById: (Int)->Task?): List<Task>

  @Throws(ProjectDatabaseException::class)
  fun mapTasks(vararg columnConsumer: ColumnConsumer)

  @Throws(ProjectDatabaseException::class)
  fun validateColumnConsumer(columnConsumer: ColumnConsumer)

  @Throws(ProjectDatabaseException::class)
  fun applyUpdate(logRecords: List<XlogRecord>, baseTxnId: BaseTxnId, targetTxnId: BaseTxnId)

  @Throws(ProjectDatabaseException::class)
  fun readAllTasks(): List<TaskRecord>

  fun addExternalUpdatesListener(listener: ProjectDatabaseExternalUpdateListener)

  /**
   * This method should be called whenever something changes in the custom properties.
   * It updates the internal structures and tables for storing custom values as necessary.
   *
   * @param customPropertyManager an instance of the CustomPropertyManager with the actual custom property definitions.
   */
  fun onCustomColumnChange(customPropertyManager: CustomPropertyManager)

  fun updateBuiltInCalculatedColumns()
}
