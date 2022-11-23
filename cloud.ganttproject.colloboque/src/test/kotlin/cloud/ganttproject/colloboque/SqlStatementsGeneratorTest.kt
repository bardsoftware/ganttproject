/*
Copyright 2022 BarD Software s.r.o, Edgar Zhavoronkov

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
package cloud.ganttproject.colloboque

import biz.ganttproject.core.chart.render.ShapePaint
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.storage.db.Tables.TASK
import net.sourceforge.ganttproject.storage.*
import net.sourceforge.ganttproject.task.CostStub
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.util.ColorConvertion
import org.h2.jdbcx.JdbcDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.awt.Color
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

class SqlStatementsGeneratorTest {
  private lateinit var dataSource: DataSource
  private lateinit var projectDatabase: ProjectDatabase
  private lateinit var taskManager: TaskManager
  private lateinit var dsl: DSLContext

  @BeforeEach
  fun init() {
    dataSource = JdbcDataSource().also {
      it.setURL("jdbc:h2:mem:test$SQL_PROJECT_DATABASE_OPTIONS")
    }
    projectDatabase = SqlProjectDatabaseImpl(dataSource).also {
      it.startLog("0")
    }
    val taskManagerBuilder = TestSetupHelper.newTaskManagerBuilder()
    taskManagerBuilder.setTaskUpdateBuilderFactory { task -> projectDatabase.createTaskUpdateBuilder(task) }
    taskManager = taskManagerBuilder.build()
    dsl = DSL.using(dataSource, SQLDialect.POSTGRES)
  }

  @AfterEach
  fun clear() {
    dataSource.connection.use { conn ->
      conn.createStatement().execute("shutdown")
    }
  }

  @Test
  fun `simple insert dto translates to simple insert operator`() {
    val insertDto = OperationDto.InsertOperationDto(
      tableName = "foo",
      values = mapOf(
        "column_name" to "column_value",
        "another_column_name" to "42",
        "yet_another_column_name" to "false"
      )
    )
    val sql = generateSqlStatement(dsl, insertDto)
    assert(sql.matches("""insert.into.foo.\(column_name,.another_column_name,.yet_another_column_name\).values.\('column_value',.'42',.'false'\)""".toRegex())) { "Generated query: $sql" }
  }

  @Test
  fun `insert dto with null translates to valid sql with null`() {
    val insertDto = OperationDto.InsertOperationDto(
      tableName = "foo",
      values = mapOf(
        "column_name" to "column_value",
        "another_column_name" to "42",
        "column_name_with_null" to null
      )
    )
    val sql = generateSqlStatement(dsl, insertDto)
    assert(sql.matches("""insert.into.foo.\(column_name,.another_column_name,.column_name_with_null\).values.\('column_value',.'42',.null\)""".toRegex())) { "Generated query: $sql" }
  }

  @Test
  fun `inserting task gives valid transaction`() {
    // this one is moved from ProjectDatabaseTest
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

    val tasks = dsl.selectFrom(biz.ganttproject.storage.db.tables.Task.TASK).fetch()
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
    assertEquals(1, txns[0].colloboqueOperations.size)

    // Verify that executing the log record produces the same task.
    // Importantly, it checks that dates are converted identically.
    projectDatabase.shutdown()
    projectDatabase.init()

    val sqlString = generateSqlStatement(dsl, txns[0].colloboqueOperations[0])
    dsl.execute(sqlString)
    assertEquals(tasks[0], dsl.selectFrom(TASK).fetch()[0])
  }

  @Test
  fun `UpdateOperationDto translates to postgres update`() {
    val updateDto = OperationDto.UpdateOperationDto(
      tableName = "heck",
      updateBinaryConditions = mutableListOf(Triple("id_column", BinaryPred.EQ, "some_id")),
      updateRangeConditions = mutableListOf(),
      newValues = mutableMapOf(
        "some_column" to "some_value"
      )
    )

    val sql = generateSqlStatement(dsl, updateDto)
    assert(sql.matches("""update.heck.set."some_column".=.'some_value'.where.\(.*id_column.=.'some_id'.*\)""".toRegex())) { "Generated query: $sql" }
  }

  @Test
  fun `DeleteOperationDto translates to postgres delete`() {
    val deleteAllColumnsDto = OperationDto.DeleteOperationDto(
      tableName = "table_name",
      deleteBinaryConditions = listOf(),
      deleteRangeConditions = listOf()
    )

    val deleteAllColumnsSql = generateSqlStatement(dsl, deleteAllColumnsDto)
    assert(deleteAllColumnsSql.matches("""delete.from.table_name.*""".toRegex())) { "Generated query: $deleteAllColumnsSql" }

    val deleteColumnsWithPredicateDto = OperationDto.DeleteOperationDto(
      tableName = "table_name",
      deleteBinaryConditions = listOf(Triple("id_column", BinaryPred.EQ, "soem_id")),
      deleteRangeConditions = listOf()
    )
    val deleteColumnsWithPredicateSql = generateSqlStatement(dsl, deleteColumnsWithPredicateDto)
    assert(deleteColumnsWithPredicateSql.matches("""delete.from.table_name.where.\(.*id_column.=.'soem_id'.*\)""".toRegex())) { "Generated query: $deleteColumnsWithPredicateSql" }
  }

  @Test
  fun `MergeOperationDto translates to postgres merge`() {
    val mergeDto = OperationDto.MergeOperationDto(
      tableName = "table_name",
      mergeBinaryConditions = listOf(
        Triple("id_column", BinaryPred.EQ, "some_id")
      ),
      mergeRangeConditions = listOf(
        Triple("ranged_column", RangePred.IN, listOf("foo", "bar"))
      ),
      whenMatchedThenUpdate = mapOf(
        "column_name" to "42"
      ),
      whenNotMatchedThenInsert = mapOf(
        "another_column_name" to "false"
      )
    )

    val mergeSql = generateSqlStatement(dsl, mergeDto)
    assert(mergeSql.matches("""merge.into.table_name.using.\(select.1.as."one"\).on.\(.*.and.id_column.=.'some_id'.*.and.ranged_column.in.\('foo',.'bar'\)\).when.matched.then.update.set."column_name".=.'42'.when.not.matched.then.insert.\("another_column_name"\).values.\('false'\)""".toRegex())) { "Generated query $mergeSql" }
  }

  private fun LocalDate.toIsoNoHours() = this.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}
