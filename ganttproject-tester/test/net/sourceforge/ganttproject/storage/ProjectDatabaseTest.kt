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
import biz.ganttproject.storage.db.Tables.TASKDEPENDENCY
import biz.ganttproject.storage.db.tables.Task.*
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.storage.ProjectDatabase.TaskUpdateBuilder
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl
import net.sourceforge.ganttproject.util.ColorConvertion
import org.h2.jdbcx.JdbcDataSource
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Color
import java.math.BigDecimal
import javax.sql.DataSource

class ProjectDatabaseTest {
  private lateinit var dataSource: DataSource
  private lateinit var projectDatabase: ProjectDatabase
  private lateinit var taskManager: TaskManager

  @BeforeEach
  private fun init() {
    val ds = JdbcDataSource()
    ds.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
    dataSource = ds
    projectDatabase = SqlProjectDatabaseImpl(dataSource)
    val taskManagerBuilder = TestSetupHelper.newTaskManagerBuilder()
    taskManagerBuilder.setTaskUpdateBuilderFactory(
      object: TaskUpdateBuilder.Factory {
      override fun createTaskUpdateBuilder(task: Task): TaskUpdateBuilder {
       return projectDatabase.createTaskUpdateBuilder(task)
      }
    })
    taskManager = taskManagerBuilder.build()
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

    val task = taskManager.createTask(2)
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

    val tasks = DSL.using(dataSource, SQLDialect.H2)
      .selectFrom(TASK)
      .fetch()
    assertEquals(tasks.size, 1)

    assertEquals(tasks[0].id, 2)
    assertEquals(tasks[0].name, "Task2 name")
    assertEquals(tasks[0].color, ColorConvertion.getColor(Color.CYAN))
    assertEquals(tasks[0].shape, shape.array)
    assertEquals(tasks[0].isMilestone, false)
    assertEquals(tasks[0].isProjectTask, true)
    assertEquals(tasks[0].startDate.time, task.start.timeInMillis)
    assertEquals(tasks[0].duration, 10)
    assertEquals(tasks[0].completion, 20)
    assertEquals(tasks[0].earliestStartDate.time, task.third.timeInMillis)
    assertEquals(tasks[0].priority, Task.Priority.HIGH.persistentValue)
    assertEquals(tasks[0].webLink, "love-testing.com")
    assertEquals(tasks[0].costManualValue.toDouble(), 666.7)
    assertEquals(tasks[0].isCostCalculated, true)
    assertEquals(tasks[0].notes, "abacaba")
  }

  @Test
  fun `test insert task same id throws`() {
    val task = taskManager.createTask(2)

    projectDatabase.init()
    projectDatabase.insertTask(task)
    assertThrows<ProjectDatabaseException> {
      projectDatabase.insertTask(task)
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
    val tasks = DSL.using(dataSource, SQLDialect.H2)
      .selectFrom(TASK)
      .fetch()
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
    val dependant = taskManager.createTask(1)
    val dependee = taskManager.createTask(2)

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

    val deps = DSL.using(dataSource, SQLDialect.H2)
      .selectFrom(TASKDEPENDENCY)
      .fetch()
    assertEquals(deps.size, 1)

    assertEquals(deps[0].dependantId, 1)
    assertEquals(deps[0].dependeeId, 2)
    assertEquals(deps[0].type, FinishStartConstraintImpl().type.persistentValue)
    assertEquals(deps[0].lag, 10)
    assertEquals(deps[0].hardness, TaskDependency.Hardness.STRONG.identifier)
  }

  @Test
  fun `test update task`() {
    projectDatabase.init()
    val task = taskManager.createTask(1)
    task.name = "Name1"

    projectDatabase.insertTask(task)


    val tasks1 = DSL.using(dataSource, SQLDialect.H2)
      .selectFrom(TASK)
      .fetch()
    assertEquals(tasks1.size, 1)
    assertEquals(tasks1[0].id, 1)
    assertEquals(tasks1[0].name, "Name1")

    val mutator = task.createMutator()
    mutator.setName("Name2")
    mutator.commit()

    val tasks = DSL.using(dataSource, SQLDialect.H2)
      .selectFrom(TASK)
      .fetch()
    assertEquals(tasks.size, 1)
    assertEquals(tasks[0].id, 1)
    assertEquals(tasks[0].name, "Name2")
  }
}
