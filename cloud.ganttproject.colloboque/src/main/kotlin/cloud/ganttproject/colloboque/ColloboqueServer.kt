/*
Copyright 2022 BarD Software s.r.o., GanttProject Cloud OU

This file is part of GanttProject Cloud.

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
package cloud.ganttproject.colloboque

import biz.ganttproject.core.io.XmlTasks
import biz.ganttproject.core.io.parseXmlProject
import biz.ganttproject.core.io.walkTasksDepthFirst
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.lib.fx.SimpleTreeCollapseView
import cloud.ganttproject.colloboque.db.project_template.tables.Transactionlog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.parser.TaskLoader
import net.sourceforge.ganttproject.storage.*
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.Schema
import org.jooq.conf.RenderNameCase
import org.jooq.impl.DSL
import org.jooq.impl.SchemaImpl
import java.sql.Connection
import java.text.DateFormat
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors

internal typealias ProjectRefid = String
internal typealias BaseTxnId = String

internal class TransactionLogTable(val projectRefid: ProjectRefid) : Transactionlog() {
  private val schemaName = PostgresConnectionFactory.getSchema(projectRefid)
  override fun getSchema(): Schema? {
    return SchemaImpl(schemaName)
  }
}

class ColloboqueServerException: Exception {
  constructor(message: String): super(message)
  constructor(message: String, cause: Throwable): super(message, cause)
}

val localeApi = object : CalendarFactory() {
    init {
      setLocaleApi(object : LocaleApi {
        override fun getLocale(): Locale {
          return Locale.US
        }

        override fun getShortDateFormat(): DateFormat {
          return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        }
      })
    }
  }


class ColloboqueServer(
  private val initProject: (projectRefid: String) -> Unit,
  private val connectionFactory: (projectRefid: String) -> Connection,
  private val initInputChannel: Channel<InitRecord>,
  private val updateInputChannel: Channel<InputXlog>,
  private val serverResponseChannel: Channel<ServerResponse>) {
  private val refidToBaseTxnId: MutableMap<ProjectRefid, ProjectRefid> = mutableMapOf()

  private val wsCommunicationScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

  init {
    wsCommunicationScope.launch {
      processUpdatesLoop()
    }
  }

  fun init(projectRefid: ProjectRefid, projectXml: String? = null): BaseTxnId {
    try {
      initProject(projectRefid)
      connectionFactory(projectRefid).use {
        if (projectXml != null) {
          DSL.using(it, SQLDialect.POSTGRES)
            .configuration()
            .deriveSettings { it.withRenderNameCase(RenderNameCase.LOWER) }
            .dsl().let { dsl ->
              loadProject(projectXml, dsl)
            }
        }
        // TODO: get from the database
        return NULL_TXN_ID.also {
          refidToBaseTxnId[projectRefid] = it
        }
      }
    } catch (e: Exception) {
      throw ColloboqueServerException("Failed to init project $projectRefid", e)
    }
  }

  private suspend fun processUpdatesLoop() {
    for (inputXlog in updateInputChannel) {
      LOG.debug("Next xlog: $inputXlog")
      try {
        val newBaseTxnId = applyXlog(inputXlog.projectRefid, inputXlog.baseTxnId, inputXlog.transactions[0])
          ?: continue
        val response = ServerResponse.CommitResponse(
          inputXlog.baseTxnId,
          newBaseTxnId,
          inputXlog.projectRefid,
          inputXlog.transactions,
          inputXlog.clientTrackingCode
        )
        serverResponseChannel.send(response)
      } catch (e: Exception) {
        LOG.error("Failed to commit\n {}", inputXlog, exception = e)
        val errorResponse = ServerResponse.ErrorResponse(
          inputXlog.baseTxnId,
          inputXlog.projectRefid,
          e.message.orEmpty()
        )
        serverResponseChannel.send(errorResponse)
      }
    }
  }

  /**
   * Performs transaction commit if `baseTxnId` corresponds to the value hold by the server.
   * Returns new baseTxnId on success.
   */
  private fun applyXlog(projectRefid: ProjectRefid, baseTxnId: String, transaction: XlogRecord): BaseTxnId? {
    if (transaction.colloboqueOperations.isEmpty()) throw ColloboqueServerException("Empty transactions not allowed")
    val expectedBaseTxnId = getBaseTxnId(projectRefid)
    if (expectedBaseTxnId != baseTxnId) throw ColloboqueServerException("Invalid transaction id $baseTxnId, expected $expectedBaseTxnId")
    try {
      val realTransactionLog = TransactionLogTable(projectRefid)
      connectionFactory(projectRefid).use { connection ->
        return DSL
          .using(connection, SQLDialect.POSTGRES)
          .transactionResult { config ->
            val context: DSLContext = config.dsl()
            transaction.colloboqueOperations.forEach { context.execute(generateSqlStatement(context, it)) }
            val nextTxnId = generateNextTxnId(projectRefid, baseTxnId, transaction)
            val xlogJson = Json.encodeToString(transaction)
            LOG.debug("Inserting into $realTransactionLog: ($nextTxnId, $xlogJson)")
            context
              .insertInto(realTransactionLog, realTransactionLog.UID, realTransactionLog.LOG)
              .values(nextTxnId, xlogJson).execute()
            nextTxnId
          }.also { refidToBaseTxnId[projectRefid] = it }
      }
    } catch (e: Exception) {
      throw ColloboqueServerException("Failed to commit transaction", e)
    }
  }


  fun getBaseTxnId(projectRefid: ProjectRefid) = refidToBaseTxnId[projectRefid]

  fun getTransactionLogs(projectRefid: ProjectRefid, baseTxnId: BaseTxnId = NULL_TXN_ID): List<XlogRecord> {
    connectionFactory(projectRefid).use { connection ->
      val realTransactionLog = TransactionLogTable(projectRefid)
      val context: DSLContext = DSL.using(connection, SQLDialect.POSTGRES)
      val transactions = if (baseTxnId != NULL_TXN_ID) {
        context
          .select(realTransactionLog.LOG)
          .from(realTransactionLog)
          .where("DATE(?) > DATE(?)", realTransactionLog.UID, baseTxnId)
          .orderBy(realTransactionLog.UID).fetch()
      } else {
        context
          .select(realTransactionLog.LOG)
          .from(realTransactionLog)
          .orderBy(realTransactionLog.UID).fetch()
      }
      return transactions.map { result ->
        val stringLog = result[0] as String
        Json.decodeFromString<XlogRecord>(stringLog)
      }
    }
  }

  // TODO
  private fun generateNextTxnId(projectRefid: ProjectRefid, oldTxnId: BaseTxnId, transaction: XlogRecord): BaseTxnId {
    return LocalDateTime.now().toString()
  }
}


private fun loadProject(xmlInput: String, dsl: DSLContext) {
  val bufferProject = GanttProjectImpl()
  val taskLoader = TaskLoader(bufferProject.taskManager, SimpleTreeCollapseView())
  parseXmlProject(xmlInput).let { xmlProject ->
    taskLoader.loadTaskCustomPropertyDefinitions(xmlProject)
    xmlProject.walkTasksDepthFirst { parent: XmlTasks.XmlTask?, child: XmlTasks.XmlTask ->
      taskLoader.loadTask(parent, child)
      true
    }
  }
  bufferProject.taskManager.tasks.forEach { task ->
    buildInsertTaskQuery(dsl, task).execute()
  }

}

private val LOG = GPLogger.create("ColloboqueServer")
private val NULL_TXN_ID = "0"
