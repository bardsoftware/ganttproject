/*
Copyright 2024 BarD Software s.r.o., Dmitry Barashev, Veronika Sirotkina

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

import kotlinx.coroutines.*
import net.sourceforge.ganttproject.storage.XlogRecord
import net.sourceforge.ganttproject.storage.generateSqlStatement
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.util.concurrent.Executors

/**
 * This class checks if applying two concurrent transaction logs to the same base snapshot produces any conflicts.
 */
class PostgreXlogMerger(private val connectionFactory: PostgresConnectionFactory, private val projectRefid: ProjectRefid) {
  /**
   * Creates a base snapshot from the given XML string
   */
  fun createProjectSnapshotDatabase(projectXml: String): PostgresConnectionFactory.TemporaryDataSource {
    val tempDataSource = connectionFactory.createTemporaryDataSource()
    val postgresStorage = PostgreStorageApi(tempDataSource.connectionFactory)
    postgresStorage.getOrCreateProjectSchema(projectRefid)
    loadProject(projectRefid, projectXml, postgresStorage)
    return tempDataSource
  }


  /**
   * This function applies the transaction logs stored on the server and those received from a client.
   * The logs are executed in two concurrent transactions and if they both complete successfully, we believe that there
   * are no conflicts, and we can merge the client's changes. Otherwise, client's changes conflict with the server's and must be
   * rejected.
   */
  fun tryMergeConcurrentUpdates(
    temporaryDataSource: PostgresConnectionFactory.TemporaryDataSource,
    serverTransaction: List<XlogRecord>,
    clientTransaction: List<XlogRecord>
  ): Boolean {
    val serverConnection = temporaryDataSource.connectionFactory.createConnection(projectRefid)
    val clientConnection = temporaryDataSource.connectionFactory.createConnection(projectRefid)
    serverConnection.transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ
    clientConnection.transactionIsolation = Connection.TRANSACTION_REPEATABLE_READ
    return try {
      tryMergeConcurrentUpdates(serverConnection, clientConnection, serverTransaction, clientTransaction)
    } finally {
      serverConnection.close()
      clientConnection.close()
    }
  }

  fun tryMergeConcurrentUpdates(
      serverConnection: Connection,
      clientConnection: Connection,
      serverTransaction: List<XlogRecord>,
      clientTransaction: List<XlogRecord>
    ): Boolean {
    val mergeScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    val serverDsl = dsl(serverConnection)
    val clientDsl = dsl(clientConnection)
    serverDsl.startTransaction().execute()
    clientDsl.startTransaction().execute()
    try {
      val serverJob = mergeScope.async {
        serverTransaction.forEach {
          it.colloboqueOperations.forEach {
            LOG.debug("... applying operation={}", it)
            serverDsl.execute(generateSqlStatement(serverDsl, it).also { println(it) })
          }
        }
      }
      val clientJob = mergeScope.async {
        clientTransaction.forEach {
          it.colloboqueOperations.forEach {
            LOG.debug("... applying operation={}", it)
            clientDsl.execute(generateSqlStatement(clientDsl, it).also { println(it) })
          }
        }
      }
      val readyCommit = try {
        runBlocking {
          withTimeout(MERGE_TXN_TIMEOUT_MS) { serverJob.await() }
          LOG.debug("...server job completed!")
          withTimeout(MERGE_TXN_TIMEOUT_MS) { clientJob.await() }
          LOG.debug("...client job completed!")
          true
        }
      } catch (ex: Exception) {
        LOG.error("Failed to complete one of the transactions", ex)
        false
      }
      if (!readyCommit) {
        return false
      }
      LOG.debug("... committing server's transaction")
      if (serverDsl.commit().execute() != 0) {
        clientDsl.rollback().execute()
        return false
      }
      LOG.debug("... committing client's transaction")
      return clientDsl.commit().execute() == 0
    } catch (e: Exception) {
      LOG.info("Failed to execute transactions in parallel! Reason: ${e.localizedMessage}")
    }
    return false
  }
}

// How long we will wait until a merge transaction commits. It may block because of waiting for a lock held by the concurrent
// transaction, so we use the timeout to detect it.
private val MERGE_TXN_TIMEOUT_MS = 1000L
private val LOG = LoggerFactory.getLogger("Postgres.XlogMerger")