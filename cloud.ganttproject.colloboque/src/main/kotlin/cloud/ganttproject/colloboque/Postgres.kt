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

import com.google.common.hash.Hashing
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.XlogRecord
import net.sourceforge.ganttproject.storage.buildInsertTaskQuery
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderNameCase
import org.jooq.impl.DSL
import java.sql.Connection

class PostgresConnectionFactory(
  private val pgHost: String, private val pgPort: Int, private val pgSuperUser: String, private val pgSuperAuth: String
) {
  // TODO: allow for using one database per project
  private val superConfig = HikariConfig().apply {
    username = pgSuperUser
    password = pgSuperAuth
    jdbcUrl = "jdbc:postgresql://${pgHost}:${pgPort}/dev_all_projects"
  }
  private val superDataSource = HikariDataSource(superConfig)
  // TODO: replace the user
  private val regularConfig = HikariConfig().apply {
    username = pgSuperUser
    password = pgSuperAuth
    jdbcUrl = "jdbc:postgresql://${pgHost}:${pgPort}/dev_all_projects"
  }
  private val regularDataSource = HikariDataSource(regularConfig)

  init {
    superDataSource.connection.use {
      it.createStatement().executeQuery("SELECT version()").use { rs ->
        if (rs.next()) {
          STARTUP_LOG.debug("Connected to the database. {}", rs.getString(1))
        }
      }
    }
  }

  fun createConnection(projectRefid: String): Connection =
    regularDataSource.connection.also { it.schema = getSchema(projectRefid) }

  fun createSuperConnection(projectRefid: String) = superDataSource.connection

  // TODO: escape projectRefid
  companion object {
    fun getSchema(projectRefid: String) =
      "project_${Hashing.murmur3_128().hashBytes(projectRefid.toByteArray(Charsets.UTF_8))}"
  }
}

typealias ConnectionFactory = (projectRefid: String) -> Connection
fun createPostgresStorage(factory: ConnectionFactory, superFactory: ConnectionFactory): StorageApi {
  fun <T> txn(projectRefid: ProjectRefid, code: (DSLContext)->T): T {
    return factory(projectRefid).use {cxn ->
      DSL.using(cxn, SQLDialect.POSTGRES)
        .configuration().deriveSettings { it.withRenderNameCase(RenderNameCase.LOWER) }
        .dsl().transactionResult { it -> code(it.dsl()) }
    }
  }

  return StorageApi(
    initProject = {projectRefid ->
      val schema = PostgresConnectionFactory.getSchema(projectRefid)
      superFactory(projectRefid).use {
        it.prepareCall("SELECT clone_schema(?, ?, ?)").use { stmt ->
          stmt.setString(1, "project_template")
          stmt.setString(2, schema)
          stmt.setBoolean(3, false)
          stmt.execute()
        }
      }
    },
    getTransactionLogs = { projectRefid, baseTxnId ->
      txn(projectRefid) { context ->
        val realTransactionLog = TransactionLogTable(projectRefid)
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
        transactions.map { result ->
          val stringLog = result[0] as String
          Json.decodeFromString<XlogRecord>(stringLog)
        }
      }
    },
    insertTask = { projectRefid, task -> txn(projectRefid) {
      buildInsertTaskQuery(it, task).execute()
    }}
  )
}

private val NULL_TXN_ID = "0"
private val STARTUP_LOG = GPLogger.create("Startup")
private val LOG = GPLogger.create("Postgres")