/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.storage

import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.customproperty.*
import biz.ganttproject.storage.db.tables.Task
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.storage.*
import net.sourceforge.ganttproject.task.CostStub
import net.sourceforge.ganttproject.task.TaskManager
import org.h2.jdbcx.JdbcDataSource
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.w3c.util.DateParser
import java.math.BigDecimal
import java.time.LocalDate
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource

class CalculatedPropertyTest {
  private lateinit var dataSource: DataSource
  private lateinit var projectDatabase: ProjectDatabase
  private lateinit var taskManager: TaskManager
  private lateinit var customPropertyManager: CustomPropertyManager

  @BeforeEach
  fun init() {
    dataSource = JdbcDataSource().also {
      it.setURL("jdbc:h2:mem:test$SQL_PROJECT_DATABASE_OPTIONS")
    }
    projectDatabase = SqlProjectDatabaseImpl(dataSource)
    projectDatabase.init()

    taskManager = TestSetupHelper.newTaskManagerBuilder().also {
      it.setTaskUpdateBuilderFactory { task -> projectDatabase.createTaskUpdateBuilder(task) }
    }.build()

    customPropertyManager = taskManager.customPropertyManager
  }

  @AfterEach
  fun clear() {
    dataSource.connection.use { conn ->
      conn.createStatement().execute("shutdown")
    }
  }

  @Test
  fun `create task data table`() {
    customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "foo")
    customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "bar").also {
      it.calculationMethod = SimpleSelect(it.id, "tpc0 + '--'", resultClass = CustomPropertyClass.TEXT.javaClass)
    }
    rebuildTaskDataTable(dataSource, customPropertyManager)

    val dsl = DSL.using(dataSource, SQLDialect.H2)
    val tasks = dsl.selectFrom(Task.TASK).fetch()
    assert(tasks.isEmpty())
  }

  @Test
  fun `calculated property value`() {
    val foo = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "foo")
    val bar = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "bar").also {
      it.calculationMethod = SimpleSelect(it.id, "duration + 1", resultClass = CustomPropertyClass.INTEGER.javaClass)
    }
    val baz = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "baz").also {
      it.calculationMethod = SimpleSelect(it.id, "tpc0 || '--'", resultClass = CustomPropertyClass.TEXT.javaClass)
    }
    rebuildTaskDataTable(dataSource, customPropertyManager)

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    task.customValues.setValue(foo, "foo")
    projectDatabase.insertTask(task)

    val dsl = DSL.using(dataSource, SQLDialect.H2)
    val tasks = dsl.selectFrom(Task.TASK).fetch()
    assertEquals(1, tasks.size)

    val propertyHolders = createPropertyHolders(taskManager)
    val updater = CalculatedPropertyUpdater(projectDatabase, {customPropertyManager}, {propertyHolders})
    updater.update()

    assertEquals(2, task.customValues.getValue(bar))
    assertEquals("foo--", task.customValues.getValue(baz))
  }

  @Test
  fun `builtin calculated property value`() {
    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    projectDatabase.insertTask(task)

    H2Functions.taskManager.set(taskManager)
    task.createMutator().let {
      it.setCost(CostStub(BigDecimal.ZERO, true))
      it.setDuration(taskManager.createLength(2))
      it.commit()
    }

    val updater = CalculatedPropertyUpdater(projectDatabase, {customPropertyManager}, { emptyMap() })
    updater.update()

    DSL.using(dataSource, SQLDialect.H2).also {
      val tasks = it.selectFrom(Task.TASK).fetch()
      assertEquals(1, tasks.size)
      assertEquals(task.end.time, DateParser.toJavaDate(tasks[0].endDate))
    }

    val humanResourceManager = HumanResourceManager(null, CustomColumnsManager())
    val resource = humanResourceManager.newResourceBuilder().withName("foo").withID(1).withStandardRate(BigDecimal.valueOf(100)).build()
    task.assignmentCollection.addAssignment(resource).also {
        it.load = 100f;
    }

    updater.update()
    DSL.using(dataSource, SQLDialect.H2).also {
      val tasks = it.selectFrom(Task.TASK).fetch()
      assertEquals(1, tasks.size)
      assertEquals(task.cost.value.toDouble(), tasks[0].cost.toDouble())
      assertEquals(200.0, tasks[0].cost.toDouble())
    }

  }

  @Test
  fun `calculated property depends on a custom column value`() {
    val foo = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "foo")
    val bar = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "bar").also {
      it.calculationMethod = SimpleSelect(it.id, "tpc0 * 2", resultClass = CustomPropertyClass.INTEGER.javaClass)
    }
    rebuildTaskDataTable(dataSource, customPropertyManager)

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    projectDatabase.insertTask(task)

    // Change the value of the stored custom property through the mutator, the way it happens in the UI.
    task.createMutator().let {
      it.setCustomProperties(task.customValues.copyOf().also { values -> values.setValue(foo, 21) })
      it.commit()
    }

    val propertyHolders = createPropertyHolders(taskManager)
    CalculatedPropertyUpdater(projectDatabase, { customPropertyManager }, { propertyHolders }).update()

    assertEquals(21, task.customValues.getValue(foo))
    assertEquals(42, task.customValues.getValue(bar))
  }

  @Test
  fun `calculated property depends on a custom column with a default value`() {
    val foo = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "foo", "10")
    val bar = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "bar").also {
      it.calculationMethod = SimpleSelect(it.id, "tpc0 * 2", resultClass = CustomPropertyClass.INTEGER.javaClass)
    }
    rebuildTaskDataTable(dataSource, customPropertyManager)

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    projectDatabase.insertTask(task)

    val propertyHolders = createPropertyHolders(taskManager)
    val updater = CalculatedPropertyUpdater(projectDatabase, { customPropertyManager }, { propertyHolders })

    // The task has no own value of `foo`, so the calculation is expected to use the default value.
    updater.update()
    assertEquals(20, task.customValues.getValue(bar))

    // Now let's write some other property through the mutator. The default value of `foo` shall survive it.
    task.createMutator().let {
      it.setName("task2")
      it.commit()
    }
    updater.update()
    assertEquals(20, task.customValues.getValue(bar))

    // Overriding the default value shall be reflected in the calculated property.
    task.createMutator().let {
      it.setCustomProperties(task.customValues.copyOf().also { values -> values.setValue(foo, 21) })
      it.commit()
    }
    updater.update()
    assertEquals(42, task.customValues.getValue(bar))

    // ... and clearing the own value shall bring the default value back.
    task.createMutator().let {
      it.setCustomProperties(task.customValues.copyOf().also { values -> values.setValue(foo, null) })
      it.commit()
    }
    updater.update()
    assertEquals(20, task.customValues.getValue(bar))
  }

  @Test
  fun `adding a calculated property updates its values immediately`() {
    val bar = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "bar", "10")
    projectDatabase.onCustomColumnChange(customPropertyManager, taskManager.tasks.toList())

    val taskWithDefaultValue = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    val taskWithOwnValue = taskManager.newTaskBuilder().withName("task2").withStartDate(Date()).build().also {
      it.customValues.setValue(bar, 21)
    }
    taskManager.tasks.forEach(projectDatabase::insertTask)

    // Now let's add a calculated property which uses the values of `bar`.
    val foo = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "foo").also {
      it.calculationMethod = SimpleSelect(it.id, "${bar.id} * 2", resultClass = CustomPropertyClass.INTEGER.javaClass)
    }
    projectDatabase.onCustomColumnChange(customPropertyManager, taskManager.tasks.toList())

    val propertyHolders = createPropertyHolders(taskManager)
    CalculatedPropertyUpdater(projectDatabase, { customPropertyManager }, { propertyHolders }).update()

    // Re-creating the custom columns must not lose the values of the stored properties, no matter if they are
    // the default ones or the task own ones.
    assertEquals(20, taskWithDefaultValue.customValues.getValue(foo))
    assertEquals(42, taskWithOwnValue.customValues.getValue(foo))
  }

  @Test
  fun `custom property values of all the supported types are written into the task table`() {
    val text = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "text")
    val int = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "int")
    val double = customPropertyManager.createDefinition(CustomPropertyClass.DOUBLE, "double")
    val boolean = customPropertyManager.createDefinition(CustomPropertyClass.BOOLEAN, "boolean")
    val date = customPropertyManager.createDefinition(CustomPropertyClass.DATE, "date")
    rebuildTaskDataTable(dataSource, customPropertyManager)

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    projectDatabase.insertTask(task)

    task.createMutator().let {
      it.setCustomProperties(task.customValues.copyOf().also { values ->
        values.setValue(text, "it's a text")
        values.setValue(int, 42)
        values.setValue(double, 3.14)
        values.setValue(boolean, true)
        values.setValue(date, CalendarFactory.createGanttCalendar(2024, 4, 1))
      })
      it.commit()
    }

    DSL.using(dataSource, SQLDialect.H2).fetchSingle("select * from Task").also { record ->
      assertEquals("it's a text", record.get(text.id, String::class.java))
      assertEquals(42, record.get(int.id, Int::class.java))
      assertEquals(3.14, record.get(double.id, Double::class.java))
      assertEquals(true, record.get(boolean.id, Boolean::class.java))
      assertEquals(LocalDate.of(2024, 5, 1), record.get(date.id, LocalDate::class.java))
    }

    // Now clear the values and make sure that the table columns are set to NULL.
    task.createMutator().let {
      it.setCustomProperties(CustomColumnsValues(customPropertyManager) {})
      it.commit()
    }
    DSL.using(dataSource, SQLDialect.H2).fetchSingle("select * from Task").also { record ->
      listOf(text, int, double, boolean, date).forEach { def -> assertNull(record.get(def.id)) }
    }
  }

  @Test
  fun `column used in a generated column can't be dropped`() {
    customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "bar").also {
      it.calculationMethod = SimpleSelect(it.id, "duration + 1", resultClass = CustomPropertyClass.INTEGER.javaClass)
    }
    rebuildTaskDataTable(dataSource, customPropertyManager)

    assertThrows<SQLException> {
      dataSource.connection.use { conn ->
        conn.createStatement().use {
          it.execute("ALTER TABLE Task DROP COLUMN duration;")
        }
        conn.commit()
      }
    }
  }

  @Test
  fun `custom column creation order`() {
    val manager = SqlCustomPropertyStorageManager(dataSource)
    customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "bar").also {
      it.calculationMethod = SimpleSelect(it.id, "tpc1 + 1", resultClass = it.type)
    }
    customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "foo")
    // We expect that the stored column tpc1 will be created first.
    manager.onCustomColumnChange(customPropertyManager)

    customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "baz").also {
      it.calculationMethod = SimpleSelect(it.id, "'#' || tpc1", resultClass = it.type)
    }
    manager.onCustomColumnChange(customPropertyManager)
  }

  @Test
  fun `insert writes the custom property values of all the supported types into the task table`() {
    val text = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "text")
    val int = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "int")
    val double = customPropertyManager.createDefinition(CustomPropertyClass.DOUBLE, "double")
    val boolean = customPropertyManager.createDefinition(CustomPropertyClass.BOOLEAN, "boolean")
    val date = customPropertyManager.createDefinition(CustomPropertyClass.DATE, "date")
    rebuildTaskDataTable(dataSource, customPropertyManager)

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    task.customValues.setValue(text, "it's a text")
    task.customValues.setValue(int, 42)
    task.customValues.setValue(double, 3.14)
    task.customValues.setValue(boolean, true)
    task.customValues.setValue(date, CalendarFactory.createGanttCalendar(2024, 4, 1))
    projectDatabase.insertTask(task)

    DSL.using(dataSource, SQLDialect.H2).fetchSingle("select * from Task").also { record ->
      assertEquals("it's a text", record.get(text.id, String::class.java))
      assertEquals(42, record.get(int.id, Int::class.java))
      assertEquals(3.14, record.get(double.id, Double::class.java))
      assertEquals(true, record.get(boolean.id, Boolean::class.java))
      assertEquals(LocalDate.of(2024, 5, 1), record.get(date.id, LocalDate::class.java))
    }
  }

  @Test
  fun `insert writes the default values of the unset stored properties`() {
    val int = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "int", "10")
    val text = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "text")
    rebuildTaskDataTable(dataSource, customPropertyManager)

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    projectDatabase.insertTask(task)

    DSL.using(dataSource, SQLDialect.H2).fetchSingle("select * from Task").also { record ->
      assertEquals(10, record.get(int.id, Int::class.java))
      assertNull(record.get(text.id))
    }
  }

  @Test
  fun `insert does not write into the generated columns and they evaluate from the stored values`() {
    val stored = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "stored")
    val calculated = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "calculated").also {
      it.calculationMethod = SimpleSelect(it.id, "${stored.id} * 2", resultClass = CustomPropertyClass.INTEGER.javaClass)
    }
    rebuildTaskDataTable(dataSource, customPropertyManager)

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    task.customValues.setValue(stored, 21)
    projectDatabase.insertTask(task)

    DSL.using(dataSource, SQLDialect.H2).fetchSingle("select * from Task").also { record ->
      assertEquals(21, record.get(stored.id, Int::class.java))
      assertEquals(42, record.get(calculated.id, Int::class.java))
    }
  }

  @Test
  fun `update resets the missing properties to the default values`() {
    val stored = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "stored", "the default")
    rebuildTaskDataTable(dataSource, customPropertyManager)

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    projectDatabase.insertTask(task)

    // Setting the own value writes it into the column.
    task.createMutator().let { mutator ->
      mutator.setCustomProperties(task.customValues.copyOf().also { it.setValue(stored, "own value") })
      mutator.commit()
    }
    DSL.using(dataSource, SQLDialect.H2).fetchSingle("select * from Task").also { record ->
      assertEquals("own value", record.get(stored.id, String::class.java))
    }

    // Clearing the own value brings the default value back, not NULL.
    task.createMutator().let { mutator ->
      mutator.setCustomProperties(CustomColumnsValues(customPropertyManager) {})
      mutator.commit()
    }
    DSL.using(dataSource, SQLDialect.H2).fetchSingle("select * from Task").also { record ->
      assertEquals("the default", record.get(stored.id, String::class.java))
    }
  }

  @Test
  fun `update writes all the changed properties in a single statement`() {
    val stored = customPropertyManager.createDefinition(CustomPropertyClass.TEXT, "stored")
    val withDefault = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "int", "10")
    val calculated = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "calculated").also {
      it.calculationMethod = SimpleSelect(it.id, "${withDefault.id} + 1", resultClass = CustomPropertyClass.INTEGER.javaClass)
    }

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    val statements = mutableListOf<String>()
    H2TaskUpdateBuilder(task, statements::addAll, SQLDialect.H2).also { builder ->
      builder.setName("task1", "task2")
      builder.setCustomProperties(task.customValues, CustomColumnsValues(customPropertyManager) {}.also {
        it.setValue(stored, "new value")
      })
      builder.commit()
    }

    assertEquals(1, statements.size)
    statements[0].lowercase().also { statement ->
      assertTrue(statement.contains(stored.id)) { "Statement: $statement" }
      assertTrue(statement.contains("'new value'")) { "Statement: $statement" }
      // The properties missing in the holder are reset to their default values.
      assertTrue(statement.contains(withDefault.id)) { "Statement: $statement" }
      // The calculated properties are not written.
      assertFalse(statement.contains(calculated.id)) { "Statement: $statement" }
      assertTrue(statement.contains("name")) { "Statement: $statement" }
      assertTrue(statement.contains("task2")) { "Statement: $statement" }
    }
  }

  @Test
  fun `custom columns recover after a rebuild which fails in the middle`() {
    val manager = SqlCustomPropertyStorageManager(dataSource)
    val stored = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "stored")
    val calculated = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "calculated").also {
      it.calculationMethod = SimpleSelect(it.id, "${stored.id} * 2", resultClass = it.type)
    }
    assertTrue(manager.onCustomColumnChange(customPropertyManager))

    // Deleting the stored property leaves the calculated one dangling: the drops succeed, but adding the generated
    // column fails because its expression refers to the column which has just been dropped.
    customPropertyManager.deleteDefinition(stored)
    assertThrows<ProjectDatabaseException> { manager.onCustomColumnChange(customPropertyManager) }

    // The rebuild which is triggered by adding another stored property adds its column and fails on the dangling
    // calculated one again. The added column must not be left behind as an orphan.
    val another = customPropertyManager.createDefinition(CustomPropertyClass.INTEGER, "another")
    assertThrows<ProjectDatabaseException> { manager.onCustomColumnChange(customPropertyManager) }

    // Deleting the dangling calculated property is expected to bring the columns back to the working state.
    customPropertyManager.deleteDefinition(calculated)
    assertTrue(manager.onCustomColumnChange(customPropertyManager))

    val task = taskManager.newTaskBuilder().withName("task1").withStartDate(Date()).build()
    task.customValues.setValue(another, 42)
    projectDatabase.insertTask(task)
    DSL.using(dataSource, SQLDialect.H2).fetchSingle("SELECT ${another.id} FROM Task").also {
      assertEquals(42, it.get(another.id))
    }
  }

}

private fun createPropertyHolders(taskManager: TaskManager) =
  HashMap<Int, CustomPropertyHolder>().also { mapping ->
    for (t in taskManager.getTasks()) {
      mapping[t.taskID] = t.customValues
    }
  }
