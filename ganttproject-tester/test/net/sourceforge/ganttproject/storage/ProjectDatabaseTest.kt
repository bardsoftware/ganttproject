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

import biz.ganttproject.core.chart.render.ShapeConstants
import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.customproperty.CustomColumnsValues
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.storage.db.Tables.TASKDEPENDENCY
import biz.ganttproject.storage.db.tables.Task.*
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.CostStub
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl
import net.sourceforge.ganttproject.util.ColorConvertion
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
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
      it.setURL("jdbc:h2:mem:test$SQL_PROJECT_DATABASE_OPTIONS")
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
    task.cost = CostStub(BigDecimal.valueOf(666.7), true)
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

    val txns = projectDatabase.fetchTransactions(limit = 10)
    assertEquals(1, txns.size)
    assertEquals(1, txns[0].sqlStatements.size)

    // Verify that executing the log record produces the same task.
    // Importantly, it checks that dates are converted identically.
    projectDatabase.shutdown()
    projectDatabase.init()

    dsl.execute(txns[0].sqlStatements[0])
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

    val task = taskManager.newTaskBuilder().withId(2).withName("Task2").build()
    projectDatabase.insertTask(task)
    projectDatabase.shutdown()

    projectDatabase.init()
    val tasks = dsl.selectFrom(TASK).fetch()
    assert(tasks.isEmpty())
  }

  @Test
  fun `test insert task dependency no dependee throws`() {
    val dependant = taskManager.newTaskBuilder().withId(1).withName("Task1").build()
    val dependee = taskManager.newTaskBuilder().withId(2).withName("Task2").build()
    val dependency = taskManager.dependencyCollection.createDependency(dependant, dependee)

    projectDatabase.init()
    projectDatabase.insertTask(dependant)
    assertThrows<ProjectDatabaseException> {
      projectDatabase.insertTaskDependency(dependency)
    }
  }

  @Test
  fun `test insert task dependency no dependant throws`() {
    val dependant = taskManager.newTaskBuilder().withId(1).withName("Task1").build()
    val dependee = taskManager.newTaskBuilder().withId(2).withName("Task2").build()
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
    val startDateBefore = CalendarFactory.createGanttCalendar(2022, 4, 3)
    val startDateAfter = CalendarFactory.createGanttCalendar(2025, 7, 13)
    val task = taskManager
      .newTaskBuilder()
      .withUid("someuid")
      .withId(1)
      .withName("Name1")
      .withStartDate(startDateBefore.time)
      .build()
    projectDatabase.insertTask(task)

    val mutator = task.createMutatorFixingDuration()
    mutator.setName("Name2")
    mutator.setStart(startDateAfter)
    mutator.commit()

    val tasks = dsl.selectFrom(TASK).fetch()
    assertEquals(tasks.size, 1)
    assertEquals(tasks[0].uid, "someuid")
    assertEquals(tasks[0].num, 1)
    assertEquals(tasks[0].name, "Name2")
    assertEquals(tasks[0].startDate.toIsoNoHours(), startDateAfter.toXMLString())
    assertNotEquals(tasks[0].startDate.toIsoNoHours(), startDateBefore.toXMLString())

    val txns = projectDatabase.fetchTransactions(limit = 10)
    assertEquals(txns.size, 2)
    txns.forEach {
      assertEquals(it.sqlStatements.size, 1)
    }
    assert(txns[0].sqlStatements[0].contains("insert", ignoreCase = true))
    assert(txns[1].sqlStatements[0].contains("update", ignoreCase = true))
  }

  @Test
  fun `test multi-statement transaction`() {
    projectDatabase.init()

    val task1 = taskManager
      .newTaskBuilder()
      .withUid("someuid1")
      .withId(1)
      .withName("Name1")
      .build()
    val task2 = taskManager
      .newTaskBuilder()
      .withUid("someuid2")
      .withId(2)
      .withName("Name2")
      .build()

    val txn = projectDatabase.startTransaction()
    projectDatabase.insertTask(task1)
    projectDatabase.insertTask(task2)
    val mutator = task1.createMutator()
    mutator.setName("Name3")
    mutator.commit()
    txn.commit()

    val txns = projectDatabase.fetchTransactions(limit = 2)
    assertEquals(txns.size, 1)
    assertEquals(txns[0].sqlStatements.size, 3)
    assert(txns[0].sqlStatements[0].contains("insert", ignoreCase = true))
    assert(txns[0].sqlStatements[1].contains("insert", ignoreCase = true))
    assert(txns[0].sqlStatements[2].contains("update", ignoreCase = true))
    assert(txns[0].sqlStatements[0].contains("Name1"))
    assert(txns[0].sqlStatements[1].contains("Name2"))
    assert(txns[0].sqlStatements[2].contains("Name3"))
  }

  @Test
  fun `test task search`() {
    projectDatabase.init()

    val task1 = taskManager
      .newTaskBuilder()
      .withUid("someuid1")
      .withId(1)
      .withName("Name1")
      .build()
    val task2 = taskManager
      .newTaskBuilder()
      .withUid("someuid2")
      .withId(2)
      .withName("Name2")
      .build()

    val txn = projectDatabase.startTransaction()
    projectDatabase.insertTask(task1)
    projectDatabase.insertTask(task2)
    txn.commit()

    assertEquals(task1, projectDatabase.findTasks("name = 'Name1'", taskManager::getTask)[0])
    assertTrue(projectDatabase.findTasks("completion = 100", taskManager::getTask).isEmpty())
    assertEquals(2, projectDatabase.findTasks("true", taskManager::getTask).size)
  }

  @Test fun `change task end updates duration`() {
    projectDatabase.init()

    val task1 = taskManager
      .newTaskBuilder()
      .withUid("someuid1")
      .withId(1)
      .withName("Name1")
      .withStartDate(TestSetupHelper.newMonday().time)
      .build()
    val txn = projectDatabase.startTransaction()
    task1.createMutator().also {
      it.setEnd(TestSetupHelper.newWendesday())
      it.commit()
    }
    txn.commit()
    val txns = projectDatabase.fetchTransactions(limit = 2)
    assertEquals(1, txns.size)

    assertEquals(1, txns[0].sqlStatements.size)
    val stmt = txns[0].sqlStatements[0]
    assert(stmt.contains("update", ignoreCase = true)) {"Statement text: $stmt"}
    assert(stmt.contains("duration", ignoreCase = true)) {"Statement text: $stmt"}
  }

  @Test fun `update task properties`() {
    projectDatabase.init()

    val task1 = taskManager
      .newTaskBuilder()
      .withUid("someuid1")
      .withId(1)
      .withName("Name1")
      .withStartDate(TestSetupHelper.newMonday().time)
      .build()
    val txn = projectDatabase.startTransaction()
    task1.createMutator().also {
      it.completionPercentage = 50
      it.setColor(Color.RED)
      it.setCost(CostStub(BigDecimal.TEN, false))
      it.setNotes("lorem ipsum")
      it.setWebLink("https://google.com")
      it.setExpand(true)
      it.setName("task1")
      it.setShape(ShapeConstants.BACKSLASH)
      it.setPriority(Task.Priority.HIGH)
      it.setProjectTask(true)
      it.commit()
    }
    txn.commit()
    val txns = projectDatabase.fetchTransactions(limit = 2)
    assertEquals(1, txns.size)

    assertEquals(1, txns[0].sqlStatements.size) { "Recorded statements: ${txns[0].sqlStatements}"}
    val stmt = txns[0].sqlStatements[0]
    assert(stmt.contains("update", ignoreCase = true)) {"Statement text: $stmt"}
    assert(stmt.matches(""".*where.*"uid".=.'someuid1'.*""".toRegex())) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"color".=.'#ff0000'.*""".toRegex())) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"notes".=.'lorem.ipsum'.*""".toRegex())) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"web_link".=.'https://google.com'.*""".toRegex())) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"name".=.'task1'.*""".toRegex())) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"priority".=.'2'.*""".toRegex())) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"is_project_task".=.true.*""".toRegex())) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"completion".=.50.*""".toRegex())) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"is_cost_calculated".=.false.*""".toRegex())) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"cost_manual_value".=.10.*""".toRegex())) {"Statement text: $stmt"}
    assertFalse(stmt.contains("expand"))
    assertFalse(stmt.contains("start_date"))
    assertFalse(stmt.contains("duration"))
  }

  @Test fun `add custom property`() {
    projectDatabase.init()

    val task1 = taskManager
      .newTaskBuilder()
      .withUid("someuid1")
      .withId(1)
      .withName("Name1")
      .withStartDate(TestSetupHelper.newMonday().time)
      .build()
    projectDatabase.insertTask(task1)
    val def = taskManager.customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "foo", "'")
    val txn = projectDatabase.startTransaction()
    task1.createMutator().let {mutator ->
      mutator.setCustomProperties(CustomColumnsValues(taskManager.customPropertyManager).also {
        it.addCustomProperty(def, "foovalue")
      })
      mutator.commit()
    }
    txn.commit()
    val txns = projectDatabase.fetchTransactions(startTxnId = 2, limit = 2)
    assertEquals(1, txns.size)

    assertEquals(2, txns[0].sqlStatements.size) { "Recorded statements: ${txns[0].sqlStatements}"}
    txns[0].sqlStatements[0].also { stmt ->
      assert(stmt.matches(""".*delete.from..taskcustomcolumn.*where.*uid..=..someuid1.*and.*not.in.*tpc0.*""".toRegex()))
      { "Statement text: $stmt" }
    }
    txns[0].sqlStatements[1].also { stmt ->
      assert(stmt.contains("merge", ignoreCase = true)) { "Statement text: $stmt" }
      assert(stmt.matches(""".*on.*uid..=.'someuid1'.*""".toRegex())) { "Statement text: $stmt" }
      assert(stmt.matches(""".*"column_value".=.'foovalue'.*""".toRegex())) { "Statement text: $stmt" }
    }
  }
  @Test fun `delete last custom property`() {
    projectDatabase.init()

    val task1 = taskManager
      .newTaskBuilder()
      .withUid("someuid1")
      .withId(1)
      .withName("Name1")
      .withStartDate(TestSetupHelper.newMonday().time)
      .build()
    projectDatabase.insertTask(task1)
    val def = taskManager.customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "foo", null)
    task1.customValues.addCustomProperty(def, "foovalue")

    val txn = projectDatabase.startTransaction()
    task1.createMutator().let {mutator ->
      mutator.setCustomProperties(CustomColumnsValues(taskManager.customPropertyManager))
      mutator.commit()
    }
    txn.commit()
    val txns = projectDatabase.fetchTransactions(startTxnId = 2, limit = 2)
    assertEquals(1, txns.size)

    assertEquals(1, txns[0].sqlStatements.size) { "Recorded statements: ${txns[0].sqlStatements}"}
    txns[0].sqlStatements[0].also { stmt ->
      assert(stmt.matches(""".*delete.from..taskcustomcolumn.*where.*uid..=..someuid1.*""".toRegex()))
      { "Statement text: $stmt" }
      assertFalse(stmt.matches(""".*and.*not.in.*tpc0.*""".toRegex()))
    }
  }

  @Test fun `shift task`() {
    projectDatabase.init()

    val task1 = taskManager
      .newTaskBuilder()
      .withUid("someuid1")
      .withId(1)
      .withName("Name1")
      .withStartDate(TestSetupHelper.newMonday().time)
      .build()
    val txn = projectDatabase.startTransaction()
    task1.createShiftMutator().also {
      it.shift(taskManager.createLength(GPTimeUnitStack.DAY, 1.0f))
      it.commit()
    }
    txn.commit()
    val txns = projectDatabase.fetchTransactions(limit = 2)
    assertEquals(1, txns.size)

    assertEquals(1, txns[0].sqlStatements.size) { "Recorded statements: ${txns[0].sqlStatements}"}
    val stmt = txns[0].sqlStatements[0]
    assert(stmt.contains("update", ignoreCase = true)) {"Statement text: $stmt"}
    assert(stmt.matches(""".*"start_date".=.date.'${TestSetupHelper.newTuesday().toXMLString()}'.*""".toRegex())) {"Statement text: $stmt"}
  }

  @Test fun `task tree shifts in a single transaction`() {
    projectDatabase.init()

    val task1 = taskManager
      .newTaskBuilder()
      .withUid("someuid1")
      .withId(1)
      .withName("Name1")
      .withStartDate(TestSetupHelper.newMonday().time)
      .build()
    val task2 = taskManager
      .newTaskBuilder()
      .withUid("someuid2")
      .withId(2)
      .withName("Name2")
      .withParent(task1)
      .withStartDate(TestSetupHelper.newMonday().time)
      .build()
    taskManager
      .newTaskBuilder()
      .withUid("someuid3")
      .withId(3)
      .withName("Name3")
      .withParent(task2)
      .withStartDate(TestSetupHelper.newMonday().time)
      .build()

    val txn = projectDatabase.startTransaction()
    task1.createShiftMutator().also {
      it.shift(taskManager.createLength(GPTimeUnitStack.DAY, 1.0f))
      it.commit()
    }
    txn.commit()
    val txns = projectDatabase.fetchTransactions(limit = 2)
    assertEquals(1, txns.size)

    assertEquals(3, txns[0].sqlStatements.size) { "Recorded statements: ${txns[0].sqlStatements}"}

    // Due to the depth-first post-order traversal, the deepmost task updates go first.
    txns[0].sqlStatements[0].let { stmt ->
      assert(stmt.contains("update", ignoreCase = true)) {"Statement text: $stmt"}
      assert(stmt.matches(""".*"start_date".=.date.'${TestSetupHelper.newTuesday().toXMLString()}'.*where.*someuid3.*""".toRegex())) {"Statement text: $stmt"}
    }
    txns[0].sqlStatements[1].let { stmt ->
      assert(stmt.contains("update", ignoreCase = true)) {"Statement text: $stmt"}
      assert(stmt.matches(""".*"start_date".=.date.'${TestSetupHelper.newTuesday().toXMLString()}'.*where.*someuid2.*""".toRegex())) {"Statement text: $stmt"}
    }
    txns[0].sqlStatements[2].let { stmt ->
      assert(stmt.contains("update", ignoreCase = true)) {"Statement text: $stmt"}
      assert(stmt.matches(""".*"start_date".=.date.'${TestSetupHelper.newTuesday().toXMLString()}'.*where.*someuid1.*""".toRegex())) {"Statement text: $stmt"}
    }
  }

}

private fun LocalDate.toIsoNoHours() = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
