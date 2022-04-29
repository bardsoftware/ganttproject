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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class PostgresDataSourceFactory(
  private val pgHost: String, private val pgPort: Int, private val pgSuperUser: String, private val pgSuperAuth: String
) {
  // TODO: allow for using one database per project
  private val superConfig = HikariConfig().apply {
    username = pgSuperUser
    password = pgSuperAuth
    jdbcUrl = "jdbc:postgresql://${pgHost}:${pgPort}/dev_all_projects"
  }
  private val superDataSource = HikariDataSource(superConfig)
  private val cachedDataSources: MutableMap<ProjectRefid, DataSource> = mutableMapOf()

  init {
    superDataSource.connection.use {
      it.createStatement().executeQuery("SELECT version()").use { rs ->
        if (rs.next()) {
          LoggerFactory.getLogger("Startup").info("Connected to the database. {}", rs.getString(1))
        }
      }
    }
  }

  fun createDataSource(projectRefid: String): DataSource {
    cachedDataSources[projectRefid]?.let { return it }
    // TODO: escape projectRefid
    val schema = "project_$projectRefid"
    superDataSource.connection.use {
      it.prepareCall("SELECT clone_schema(?, ?, ?)").use { stmt ->
        stmt.setString(1, "project_template")
        stmt.setString(2, schema)
        stmt.setBoolean(3, false)
        stmt.execute()
      }
    }
    return HikariDataSource(HikariConfig().apply {
      username = pgSuperUser
      password = pgSuperAuth
      jdbcUrl = "jdbc:postgresql://${pgHost}:${pgPort}/dev_all_projects"
      this.schema = schema
    }).also { cachedDataSources[projectRefid] = it }
  }
}