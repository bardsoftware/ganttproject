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
import org.jooq.impl.DSL
import java.sql.Connection
import javax.sql.DataSource

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

  data class TemporaryDataSource(val dataSource: DataSource, val shutdown:()->Unit)
  fun createTemporaryDataSource(): TemporaryDataSource {
    val randomDatabase = randomDatabaseName()
    val dsl = DSL.using(superDataSource.connection)
    val result = dsl.createDatabase(randomDatabase).execute()
    if (result != 0) {
      throw RuntimeException("Can't create a temporary database")
    }
    val dataSource = HikariDataSource(HikariConfig().apply {
      username = pgSuperUser
      password = pgSuperAuth
      jdbcUrl = "jdbc:postgresql://$pgHost:$pgPort/$randomDatabase"
      addDataSourceProperty("DB_CLOSE_DELAY", "-1")
      addDataSourceProperty("DATABASE_TO_LOWER", "true")
    })
    return TemporaryDataSource(dataSource, shutdown = {
      DSL.using(dataSource.connection).dropDatabase(randomDatabase).execute()
    })
  }

  // TODO: escape projectRefid
  companion object {
    fun getSchema(projectRefid: String) =
      "project_${Hashing.murmur3_128().hashBytes(projectRefid.toByteArray(Charsets.UTF_8))}"
  }
}

private val alphabet: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
private fun randomDatabaseName() = List(20) { alphabet.random() }.joinToString("")


private val STARTUP_LOG = GPLogger.create("Startup")
private val LOG = GPLogger.create("Postgres")