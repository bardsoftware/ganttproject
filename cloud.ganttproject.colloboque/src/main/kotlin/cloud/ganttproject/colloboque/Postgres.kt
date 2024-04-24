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
import net.sourceforge.ganttproject.GPLogger
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderNameCase
import org.jooq.impl.DSL
import java.sql.Connection

class PostgresConnectionFactory(
  private val pgHost: String, private val pgPort: Int, private val pgSuperUser: String, private val pgSuperAuth: String,
  pgDatabase: String = "dev_all_projects"
) {
  // TODO: allow for using one database per project
  private val superConfig = HikariConfig().apply {
    username = pgSuperUser
    password = pgSuperAuth
    jdbcUrl = "jdbc:postgresql://${pgHost}:${pgPort}/${pgDatabase}"
    maximumPoolSize = 5
  }
  private val superDataSource = HikariDataSource(superConfig)
  // TODO: replace the user
  private val regularConfig = HikariConfig().apply {
    username = pgSuperUser
    password = pgSuperAuth
    jdbcUrl = "jdbc:postgresql://${pgHost}:${pgPort}/${pgDatabase}"
    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    maximumPoolSize = 5
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

  private fun switchDatabase(database: String) = PostgresConnectionFactory(this.pgHost, this.pgPort, this.pgSuperUser, this.pgSuperAuth, database)

  fun createConnection(projectRefid: String): Connection =
    regularDataSource.connection.also { it.schema = getSchema(projectRefid) }

  fun createSuperConnection(): Connection = superDataSource.connection

  data class TemporaryDataSource(val connectionFactory: PostgresConnectionFactory, val shutdown:()->Unit)

  fun close() {
    superDataSource.close()
    regularDataSource.close()
  }

  fun createTemporaryDataSource(): TemporaryDataSource {
    val randomDatabase = randomDatabaseName().lowercase()
    val result = superDataSource.connection.use { dsl(it).execute("""
      CREATE DATABASE $randomDatabase OWNER postgres TEMPLATE project_database_template
    """.trimIndent())
    }
    if (result != 0) {
      throw RuntimeException("Can't create a temporary database")
    }
    val temporaryFactory = this.switchDatabase(randomDatabase)
    return TemporaryDataSource(connectionFactory = temporaryFactory, shutdown = {
      temporaryFactory.close()
      superDataSource.connection.use { dsl(it).dropDatabase(randomDatabase).execute() }
    })
  }

  // TODO: escape projectRefid
  companion object {
    fun getSchema(projectRefid: String) =
      "project_${Hashing.murmur3_128().hashBytes(projectRefid.toByteArray(Charsets.UTF_8))}"
  }
}

fun dsl(cxn: Connection): DSLContext = DSL.using(cxn, SQLDialect.POSTGRES)
  .configuration().deriveSettings { it.withRenderNameCase(RenderNameCase.LOWER) }
  .dsl()

private val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
private fun randomDatabaseName() = "db" + List(20) { alphabet.random() }.joinToString("")


private val STARTUP_LOG = GPLogger.create("Startup")
private val LOG = GPLogger.create("Postgres")