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

import org.h2.jdbcx.JdbcDataSource
import javax.sql.DataSource

/**
 * Storage for holding the current state of a Gantt project.
 */
class ProjectDatabase private constructor(private val dataSource: DataSource) {
  companion object Factory {
    fun createInMemoryDatabase(): ProjectDatabase {
      val dataSource = JdbcDataSource()
      dataSource.setURL(H2_IN_MEMORY_URL)
      return ProjectDatabase(dataSource)
    }
  }

  /** Release the resources. */
  fun shutdown() {
    try {
      dataSource.connection.use { conn ->
        conn.createStatement().execute("SHUTDOWN")
      }
    } catch (e: Exception) {
      // Ignore for now
    }
  }
}

private const val H2_IN_MEMORY_URL = "jdbc:h2:mem:gantt-project-state"
