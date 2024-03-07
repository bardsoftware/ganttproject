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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.parser.TaskLoader
import net.sourceforge.ganttproject.storage.InputXlog
import net.sourceforge.ganttproject.storage.ServerResponse
import net.sourceforge.ganttproject.storage.XlogRecord
import net.sourceforge.ganttproject.storage.generateSqlStatement
import net.sourceforge.ganttproject.task.Task
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderNameCase
import org.jooq.impl.DSL
import java.sql.Connection
import java.text.DateFormat
import java.util.*
import java.util.concurrent.Executors

internal typealias ProjectRefid = String
internal typealias BaseTxnId = Long

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

data class StorageApi(
  val initProject: (projectRefid: String) -> Unit = {},
  val getTransactionLogs: (projectRefid: ProjectRefid, baseTxnId: BaseTxnId)->List<XlogRecord> = { _, _ ->
    error("Not implemented")},
  val insertXlogs: (projectRefid: ProjectRefid, baseTxnId: BaseTxnId, xlog: List<XlogRecord>) -> Unit = {_, _, _ ->
    error("Not implemented")
  },
  val insertTask: (projectRefid: String, task: Task) -> Unit = {_, _ ->}
)


class ColloboqueServer(
  private val connectionFactory: (projectRefid: String) -> Connection,
  private val storageApi: StorageApi,
  private val updateInputChannel: Channel<InputXlog>,
  private val serverResponseChannel: Channel<ServerResponse>) {
  private val refidToBaseTxnId: MutableMap<ProjectRefid, BaseTxnId> = mutableMapOf()

  private val wsCommunicationScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

  init {
    wsCommunicationScope.launch {
      processUpdatesLoop()
    }
  }

  private fun <T> txn(projectRefid: ProjectRefid, code: (DSLContext)->T): T {
    return connectionFactory(projectRefid).use {cxn ->
      DSL.using(cxn, SQLDialect.POSTGRES)
        .configuration().deriveSettings { it.withRenderNameCase(RenderNameCase.LOWER) }
        .dsl().transactionResult { it -> code(it.dsl()) }
    }
  }

  fun init(projectRefid: ProjectRefid, projectXml: String? = null): BaseTxnId {
    try {
      storageApi.initProject(projectRefid)
      if (projectXml != null) {
        loadProject(projectRefid, projectXml)
      }
      // TODO: get from the database
      return NULL_TXN_ID.also {
        refidToBaseTxnId[projectRefid] = it
      }
    } catch (e: Exception) {
      throw ColloboqueServerException("Failed to init project $projectRefid", e)
    }
  }

  private fun loadProject(projectRefid: ProjectRefid, xmlInput: String) {
    val bufferProject = GanttProjectImpl()
    val taskLoader = TaskLoader(bufferProject.taskManager, SimpleTreeCollapseView())
    parseXmlProject(xmlInput).let { xmlProject ->
      taskLoader.loadTaskCustomPropertyDefinitions(xmlProject)
      xmlProject.walkTasksDepthFirst { parent: XmlTasks.XmlTask?, child: XmlTasks.XmlTask ->
        taskLoader.loadTask(parent, child)
        true
      }
    }
    bufferProject.taskManager.tasks.forEach { task -> storageApi.insertTask(projectRefid, task) }
  }

  private suspend fun processUpdatesLoop() {
    for (inputXlog in updateInputChannel) {
      LOG.debug("Next xlog: $inputXlog")
      try {
        storageApi.insertXlogs(inputXlog.projectRefid, inputXlog.baseTxnId, inputXlog.transactions)
        val newBaseTxnId = applyXlog(inputXlog.projectRefid, inputXlog.baseTxnId, inputXlog.transactions[0])
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
  private fun applyXlog(projectRefid: ProjectRefid, baseTxnId: BaseTxnId, xlog: XlogRecord): BaseTxnId {
    LOG.debug(">> applyXlog project={} baseTxnId={}", projectRefid, baseTxnId)
    if (xlog.colloboqueOperations.isEmpty()) {
      throw ColloboqueServerException("Empty transactions are not allowed")
    }
    val expectedBaseTxnId = getBaseTxnId(projectRefid)
    if (expectedBaseTxnId != baseTxnId) {
      throw ColloboqueServerException("Base txn ID mismatch. Expected: $expectedBaseTxnId. Received: $baseTxnId")
    }
    return try {
      txn(projectRefid) { context ->
        xlog.colloboqueOperations.forEach {
          LOG.debug("... applying operation={}", it)
          context.execute(generateSqlStatement(context, it))
        }
        val nextTxnId = generateNextTxnId(projectRefid, baseTxnId, xlog)
        val xlogJson = Json.encodeToString(xlog)
        LOG.debug("... persisting log record={}", xlogJson)
        nextTxnId.also { refidToBaseTxnId[projectRefid] = it }
      }
    } catch (e: Exception) {
      throw ColloboqueServerException("Failed to commit transaction", e)
    } finally {
      LOG.debug("<< applyXlog")
    }
  }


  private fun getBaseTxnId(projectRefid: ProjectRefid) = refidToBaseTxnId[projectRefid]

  private fun getTransactionLogs(projectRefid: ProjectRefid, baseTxnId: BaseTxnId = NULL_TXN_ID) =
    storageApi.getTransactionLogs(projectRefid, baseTxnId)


  data class BuildProjectXmlResult(val projectXml: String, val txnId: BaseTxnId)
  fun buildProjectXml(projectRefid: ProjectRefid): BuildProjectXmlResult {
    val baseTxnId = getBaseTxnId(projectRefid) ?: run {
      init(projectRefid, PROJECT_XML_TEMPLATE)
    }
    LOG.debug(">> buildProjectXml refid={} baseTxnId={}", projectRefid, baseTxnId)
    val transactionLogs = getTransactionLogs(projectRefid)
    var project = PROJECT_XML_TEMPLATE
    for (xlog in transactionLogs) {
      LOG.debug("..applying xlog record={}", xlog)
      project = updateProjectXml(project, xlog)
    }
    LOG.debug("..result: {}", project)
    LOG.debug("<< buildProjectXml")
    return BuildProjectXmlResult(project, baseTxnId)
  }

  // TODO
  private fun generateNextTxnId(projectRefid: ProjectRefid, oldTxnId: BaseTxnId, transaction: XlogRecord): BaseTxnId {
    return oldTxnId + 1
  }
}


private val LOG = GPLogger.create("ColloboqueServer")
private val NULL_TXN_ID = 0L
