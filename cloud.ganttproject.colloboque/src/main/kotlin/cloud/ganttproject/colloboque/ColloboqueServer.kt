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
import cloud.ganttproject.colloboque.db.project_template.tables.records.ProjectfilesnapshotRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.parser.TaskLoader
import net.sourceforge.ganttproject.storage.BaseTxnId
import net.sourceforge.ganttproject.storage.InputXlog
import net.sourceforge.ganttproject.storage.ServerResponse
import net.sourceforge.ganttproject.storage.XlogRecord
import net.sourceforge.ganttproject.storage.generateSqlStatement
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

class ColloboqueServer(
  private val connectionFactory: (projectRefid: String) -> Connection,
  private val storageApi: StorageApi,
  private val updateInputChannel: Channel<InputXlog>,
  private val serverResponseChannel: Channel<ServerResponse>) {

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

  fun init(projectRefid: ProjectRefid, projectXml: String): BaseTxnId {
    try {
      storageApi.initProject(projectRefid)
      storageApi.insertActualSnapshot(projectRefid, NULL_TXN_ID, projectXml)
      loadProject(projectRefid, projectXml)
      return NULL_TXN_ID
    } catch (e: Exception) {
      throw ColloboqueServerException("Failed to init project $projectRefid", e)
    }
  }

  private fun loadProject(projectRefid: ProjectRefid, xmlInput: String) {
    loadProject(projectRefid, xmlInput, storageApi)
  }

  private suspend fun processUpdatesLoop() {
    for (inputXlog in updateInputChannel) {
      LOG.debug("Next xlog: $inputXlog")
      try {
        val projectRefid = inputXlog.projectRefid
        val baseTxnId = inputXlog.baseTxnId

        val actualSnapshot = storageApi.getProjectSnapshot(projectRefid) ?: throw ColloboqueServerException("Project $projectRefid is not yet initialized")
        val expectedBaseTxnId = actualSnapshot.baseTxnId
        if (expectedBaseTxnId != baseTxnId) {
          throw ColloboqueServerException("Base txn ID mismatch. Expected: $expectedBaseTxnId. Received: $baseTxnId")
        }

        // TODO: we are inserting and applying xlog records, so we need to lock the base txn ID, to prevent its
        // concurrent updates.
        storageApi.insertXlogs(inputXlog.projectRefid, inputXlog.baseTxnId, inputXlog.transactions)

        val newBaseTxnId = applyXlog(inputXlog)
        val newProjectXml = buildProjectXml(projectRefid, actualSnapshot)
        storageApi.insertActualSnapshot(projectRefid, newBaseTxnId, newProjectXml.projectXml)

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

  private fun applyXlog(xlog: InputXlog): BaseTxnId {
    return xlog.transactions.fold(xlog.baseTxnId) { txnId: BaseTxnId, xlogRecord: XlogRecord ->
      applyXlog(xlog.projectRefid, txnId, xlogRecord)
    }
  }
  /**
   * Performs transaction commit if `baseTxnId` corresponds to the value hold by the server.
   * Returns new baseTxnId on success.
   */
  private fun applyXlog(projectRefid: ProjectRefid, baseTxnId: BaseTxnId, xlog: XlogRecord): BaseTxnId {
    LOG.debug(">> applyXlog project={} baseTxnId={}", projectRefid, baseTxnId)
    if (xlog.colloboqueOperations.isEmpty()) {
      return baseTxnId
    }
    return try {
      txn(projectRefid) { context ->
        xlog.colloboqueOperations.forEach {
          LOG.debug("... applying operation={}", it)
          context.execute(generateSqlStatement(context, it))
        }
        generateNextTxnId(projectRefid, baseTxnId, xlog)
      }
    } catch (e: Exception) {
      throw ColloboqueServerException("Failed to commit transaction", e)
    } finally {
      LOG.debug("<< applyXlog")
    }
  }

  private fun getTransactionLogs(projectRefid: ProjectRefid, baseTxnId: BaseTxnId = NULL_TXN_ID) =
    storageApi.getTransactionLogs(projectRefid, baseTxnId)


  data class BuildProjectXmlResult(val projectXml: String, val txnId: BaseTxnId)

  /**
   * Takes the actual project snapshot, applies the recorded update logs and builds a new XML
   */
  fun buildProjectXml(projectRefid: ProjectRefid, baseSnapshot: ProjectfilesnapshotRecord): BuildProjectXmlResult {
    val baseTxnId = baseSnapshot.baseTxnId!!
    LOG.debug(">> buildProjectXml refid={} baseTxnId={}", projectRefid, baseTxnId)
    val transactionLogs = getTransactionLogs(projectRefid, baseTxnId)
    val updatedXml = transactionLogs.fold(baseSnapshot.projectXml!!) { xml, xlog -> updateProjectXml(xml, xlog) }
    LOG.debug("..result: {}", updatedXml)
    LOG.debug("<< buildProjectXml")
    return BuildProjectXmlResult(updatedXml, baseTxnId)
  }

  // TODO
  private fun generateNextTxnId(projectRefid: ProjectRefid, oldTxnId: BaseTxnId, transaction: XlogRecord): BaseTxnId {
    return oldTxnId + 1
  }

  fun getProjectXml(projectRefid: String): ProjectfilesnapshotRecord =
    storageApi.getProjectSnapshot(projectRefid) ?: run {
      val baseTxnId = init(projectRefid, PROJECT_XML_TEMPLATE)
      ProjectfilesnapshotRecord().apply {
        this.baseTxnId = baseTxnId
        this.projectXml = PROJECT_XML_TEMPLATE
      }
    }

}


internal fun loadProject(projectRefid: ProjectRefid, xmlInput: String, storageApi: StorageApi) {
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

private val LOG = GPLogger.create("ColloboqueServer")
private val NULL_TXN_ID = 0L
