/*
Copyright 2020 BarD Software s.r.o, Anastasiia Postnikova

This file is part of GanttProject, an opensource project management tool.

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

import biz.ganttproject.storage.db.tables.Task.*
import net.sourceforge.ganttproject.TestSetupHelper
import org.h2.jdbcx.JdbcDataSource
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.sql.DataSource

class ProjectDatabaseTest {
  private lateinit var dataSource: DataSource
  private lateinit var projectDatabase: ProjectDatabase

  @BeforeEach
  private fun init() {
    val ds = JdbcDataSource()
    ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
    dataSource = ds
    projectDatabase = SqlProjectDatabaseImpl(dataSource)
  }

  @AfterEach
  private fun clear() {
    dataSource.connection.use { conn ->
      conn.createStatement().execute("shutdown")
    }
  }

  @Test
  fun `test init creates tables`() {
    projectDatabase.init()
    val tasks = DSL.using(dataSource, SQLDialect.H2)
        .selectFrom(TASK)
        .fetch()
    assert(tasks.isEmpty())
  }

  @Test
  fun `test insert task`() {
    projectDatabase.init()

    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val task = taskManager.createTask(2)
    task.name = "Task2 name"
    projectDatabase.insertTask(task)

    val tasks = DSL.using(dataSource, SQLDialect.H2)
      .selectFrom(TASK)
      .fetch()
    assertEquals(tasks.size, 1)
    assertEquals(tasks[0].id, 2)
    assertEquals(tasks[0].name, "Task2 name")
  }

  @Test
  fun `test init after init throws`() {
    projectDatabase.init()
    assertThrows<ProjectDatabaseException> {
      projectDatabase.init()
    }
  }

  @Test
  fun `test init after shutdown legal`() {
    projectDatabase.init()
    projectDatabase.shutdown()
    projectDatabase.init()
  }

  @Test
  fun `test init after shutdown empty`() {
    projectDatabase.init()

    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val task = taskManager.createTask(2)
    task.name = "Task2 name"
    projectDatabase.insertTask(task)
    projectDatabase.shutdown()

    projectDatabase.init()
    val tasks = DSL.using(dataSource, SQLDialect.H2)
      .selectFrom(TASK)
      .fetch()
    assert(tasks.isEmpty())
  }
}
