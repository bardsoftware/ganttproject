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

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.storage.db.Tables.LOGRECORD
import biz.ganttproject.storage.db.Tables.TASKDEPENDENCY
import biz.ganttproject.storage.db.tables.Task.*
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl
import net.sourceforge.ganttproject.util.ColorConvertion
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Color
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

class ProjectDatabaseTest {
  private lateinit var dataSource: DataSource
  private lateinit var projectDatabase: ProjectDatabase
  private lateinit var taskManager: TaskManager
  private lateinit var dsl: DSLContext

  @BeforeEach
  private fun init() {
    dataSource = JdbcDataSource().also {
      it.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
    }
    projectDatabase = SqlProjectDatabaseImpl(dataSource)
    val taskManagerBuilder = TestSetupHelper.newTaskManagerBuilder()
    taskManagerBuilder.setTaskUpdateBuilderFactory { task -> projectDatabase.createTaskUpdateBuilder(task) }
    taskManager = taskManagerBuilder.build()
    dsl = DSL.using(dataSource, SQLDialect.H2)
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
    val tasks = dsl.selectFrom(TASK).fetch()
    assert(tasks.isEmpty())
  }

  @Test
  fun `test insert task`() {
    projectDatabase.init()

    val task = taskManager.newTaskBuilder().withId(2).withUid("someuid").build()
    val shape = ShapePaint(4, 4, IntArray(16) { 0 }, Color.white, Color.CYAN)
    task.name = "Task2 name"
    task.color = Color.CYAN
    task.shape = shape
    task.isMilestone = false
    task.isProjectTask = true
    task.start = CalendarFactory.createGanttCalendar(2022, 12, 6)
    task.duration = taskManager.createLength(10)
    task.completionPercentage = 20
    task.setThirdDate(CalendarFactory.createGanttCalendar(2022, 4, 3))
    task.priority = Task.Priority.HIGH
    task.webLink = "love-testing.com"
    task.cost.value = BigDecimal.valueOf(666.7)
    task.cost.isCalculated = true
    task.notes = "abacaba"

    projectDatabase.insertTask(task)

    val tasks = dsl.selectFrom(TASK).fetch()
    assertEquals(tasks.size, 1)

    assertEquals(tasks[0].uid, "someuid")
    assertEquals(tasks[0].num, 2)
    assertEquals(tasks[0].name, "Task2 name")
    assertEquals(tasks[0].color, ColorConvertion.getColor(Color.CYAN))
    assertEquals(tasks[0].shape, shape.array)
    assertEquals(tasks[0].isMilestone, false)
    assertEquals(tasks[0].isProjectTask, true)
    assertEquals(tasks[0].startDate.toIsoNoHours(), task.start.toXMLString())
    assertEquals(tasks[0].duration, 10)
    assertEquals(tasks[0].completion, 20)
    assertEquals(tasks[0].earliestStartDate.toIsoNoHours(), task.third.toXMLString())
    assertEquals(tasks[0].priority, Task.Priority.HIGH.persistentValue)
    assertEquals(tasks[0].webLink, "love-testing.com")
    assertEquals(tasks[0].costManualValue.toDouble(), 666.7)
    assertEquals(tasks[0].isCostCalculated, true)
    assertEquals(tasks[0].notes, "abacaba")

    val logs = projectDatabase.fetchLogRecords(limit = 10)
    assertEquals(logs.size, 1)

    // Verify that executing the log record produces the same task.
    // Importantly, it checks that dates are converted identically.
    projectDatabase.shutdown()
    projectDatabase.init()

    dsl.execute(logs[0].sqlStatement)
    assertEquals(tasks[0], dsl.selectFrom(TASK).fetch()[0])
  }

  @Test
  fun `test insert task same uid throws`() {
    val task1 = taskManager.newTaskBuilder().withId(1).withUid("uid").build()
    val task2 = taskManager.newTaskBuilder().withId(2).withUid("uid").build()

    projectDatabase.init()
    projectDatabase.insertTask(task1)
    assertThrows<ProjectDatabaseException> {
      projectDatabase.insertTask(task2)
    }
  }

  @Test
  fun `test init after shutdown empty`() {
    projectDatabase.init()

    val task = taskManager.createTask(2)
    task.name = "Task2 name"
    projectDatabase.insertTask(task)
    projectDatabase.shutdown()

    projectDatabase.init()
    val tasks = dsl.selectFrom(TASK).fetch()
    assert(tasks.isEmpty())
  }

  @Test
  fun `test insert task dependency no dependee throws`() {
    val dependant = taskManager.createTask(1)
    val dependee = taskManager.createTask(2)
    val dependency = taskManager.dependencyCollection.createDependency(dependant, dependee)

    projectDatabase.init()
    projectDatabase.insertTask(dependant)
    assertThrows<ProjectDatabaseException> {
      projectDatabase.insertTaskDependency(dependency)
    }
  }

  @Test
  fun `test insert task dependency no dependant throws`() {
    val dependant = taskManager.createTask(1)
    val dependee = taskManager.createTask(2)
    val dependency = taskManager.dependencyCollection.createDependency(dependant, dependee)

    projectDatabase.init()
    projectDatabase.insertTask(dependee)
    assertThrows<ProjectDatabaseException> {
      projectDatabase.insertTaskDependency(dependency)
    }
  }

  @Test
  fun `test insert task dependency`() {
    val dependant = taskManager.newTaskBuilder().withUid("dependant_uid").withId(1).build()
    val dependee = taskManager.newTaskBuilder().withUid("dependee_uid").withId(2).build()

    val dependency = taskManager.dependencyCollection.createDependency(
      dependant,
      dependee,
      FinishStartConstraintImpl(),
      TaskDependency.Hardness.STRONG
    )
    dependency.difference = 10

    projectDatabase.init()
    projectDatabase.insertTask(dependant)
    projectDatabase.insertTask(dependee)
    projectDatabase.insertTaskDependency(dependency)

    val deps = dsl.selectFrom(TASKDEPENDENCY).fetch()
    assertEquals(deps.size, 1)

    assertEquals(deps[0].dependantUid, "dependant_uid")
    assertEquals(deps[0].dependeeUid, "dependee_uid")
    assertEquals(deps[0].type, FinishStartConstraintImpl().type.persistentValue)
    assertEquals(deps[0].lag, 10)
    assertEquals(deps[0].hardness, TaskDependency.Hardness.STRONG.identifier)
  }

  @Test
  fun `test update task`() {
    projectDatabase.init()
    val task = taskManager
      .newTaskBuilder()
      .withUid("someuid")
      .withId(1)
      .withName("Name1")
      .build()
    projectDatabase.insertTask(task)

    val mutator = task.createMutator()
    mutator.setName("Name2")
    mutator.commit()

    val tasks = dsl.selectFrom(TASK).fetch()
    assertEquals(tasks.size, 1)
    assertEquals(tasks[0].uid, "someuid")
    assertEquals(tasks[0].num, 1)
    assertEquals(tasks[0].name, "Name2")

    val logs = projectDatabase.fetchLogRecords(limit = 10)
    assertEquals(logs.size, 2)
    assert(logs[0].sqlStatement.contains("insert", ignoreCase = true))
    assert(logs[1].sqlStatement.contains("update", ignoreCase = true))
  }
}

private fun LocalDate.toIsoNoHours() = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
