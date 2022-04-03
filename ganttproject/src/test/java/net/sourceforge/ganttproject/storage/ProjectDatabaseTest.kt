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

import net.sourceforge.ganttproject.GanttTask
import net.sourceforge.ganttproject.storage.tables.Task.*
import org.easymock.EasyMock.*
import org.h2.jdbcx.JdbcDataSource
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Color
import javax.sql.DataSource

class ProjectDatabaseTest {
  private lateinit var dataSource: DataSource
  private lateinit var projectDatabase: ProjectDatabase

  @BeforeEach
  private fun init() {
    val ds = JdbcDataSource()
    ds.setURL(H2_IN_MEMORY_URL)
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

    val mockTask: GanttTask = niceMock(GanttTask::class.java)
    expect(mockTask.taskID).andReturn(2).anyTimes()
    expect(mockTask.name).andReturn("Task2 name").anyTimes()
    expect(mockTask.color).andReturn(Color.CYAN).anyTimes()
    replay(mockTask)
    projectDatabase.insertTask(mockTask)

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
    val mockTask: GanttTask = niceMock(GanttTask::class.java)
    expect(mockTask.taskID).andReturn(2).anyTimes()
    expect(mockTask.name).andReturn("Task2 name").anyTimes()
    expect(mockTask.color).andReturn(Color.CYAN).anyTimes()
    replay(mockTask)
    projectDatabase.insertTask(mockTask)
    projectDatabase.shutdown()

    projectDatabase.init()
    val tasks = DSL.using(dataSource, SQLDialect.H2)
      .selectFrom(TASK)
      .fetch()
    assert(tasks.isEmpty())
  }
}