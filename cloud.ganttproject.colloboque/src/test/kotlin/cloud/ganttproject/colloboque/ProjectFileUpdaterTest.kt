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
package cloud.ganttproject.colloboque

import cloud.ganttproject.colloboque.db.project_template.tables.records.ProjectfilesnapshotRecord
import kotlinx.coroutines.channels.Channel
import net.sourceforge.ganttproject.storage.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import biz.ganttproject.storage.db.Tables.TASK as TaskTable
import java.time.LocalDate

class ProjectFileUpdaterTest {

  @BeforeEach fun setUp() {
    localeApi  // This will set a static field in CalendarFactory
  }

  @Test fun `empty xlog`() {
    assertEquals(PROJECT_XML_TEMPLATE, updateProjectXml(PROJECT_XML_TEMPLATE, XlogRecord(emptyList())))
  }

  @Test fun `apply task name change`() {
    val changes = XlogRecord(
      listOf(
        OperationDto.UpdateOperationDto("task",
          updateBinaryConditions = mutableListOf(Triple("uid", BinaryPred.EQ, "qwerty")),
          updateRangeConditions = mutableListOf(),
          newValues = mutableMapOf("name" to "Task2")
        )
      )
    )
    val updatedXml = updateProjectXml(PROJECT_XML_TEMPLATE, changes)
    assertTrue(updatedXml.lines().any { it.matches(""".*<task.*name=.Task2.*>""".toRegex()) }) {
      """The result of applying updates:
        
        $updatedXml
        """.trimIndent()
    }
  }

  @Test fun `apply persistent log to project xml`() {
    val insert1 = OperationDto.InsertOperationDto(tableName = TaskTable.name, values = mapOf(
      TaskTable.NAME.name to "TaskA",
      TaskTable.NUM.name to "2",
      TaskTable.UID.name to "qwerty234",
      TaskTable.START_DATE.name to LocalDate.parse("2024-03-05").toString(),
      TaskTable.DURATION.name to "1"
    ))
    val insert2 = OperationDto.InsertOperationDto(tableName = TaskTable.name, values = mapOf(
      TaskTable.NAME.name to "TaskB",
      TaskTable.NUM.name to "2",
      TaskTable.UID.name to "asdfg",
      TaskTable.START_DATE.name to LocalDate.parse("2024-03-05").toString(),
      TaskTable.DURATION.name to "10"
    ))

    val storageApi = PluggableStorageApi(getTransactionLogs_ = { _, _ -> listOf(XlogRecord(listOf(insert1)), XlogRecord(listOf(insert2))) })
    val server = ColloboqueServer(connectionFactory = { error("Do not connect") }, storageApi = storageApi, updateInputChannel = Channel(), serverResponseChannel = Channel())
    val updatedXml = server.buildProjectXml("asdfg", baseSnapshot = ProjectfilesnapshotRecord().also {
      it.baseTxnId = 0L
      it.projectXml = PROJECT_XML_TEMPLATE
    }).projectXml
    assertTrue(updatedXml.lines().any { it.matches(""".*<task.*name=.TaskA.*>""".toRegex()) }) {
      """The result of applying updates:
        
        $updatedXml
        """.trimIndent()
    }
    assertTrue(updatedXml.lines().any { it.matches(""".*<task.*name=.TaskB.*>""".toRegex()) }) {
      """The result of applying updates:
        
        $updatedXml
        """.trimIndent()
    }

  }

  @Test
  fun `merge concurrent updates fails`() {
    val clientChanges = XlogRecord(
      listOf(
        OperationDto.UpdateOperationDto(
          "task",
          updateBinaryConditions = mutableListOf(Triple(TaskTable.UID.name, BinaryPred.EQ, "qwerty")),
          updateRangeConditions = mutableListOf(),
          newValues = mutableMapOf(TaskTable.NAME.name to "ClientTask")
        )
      )
    )
    val serverChanges = XlogRecord(
      listOf(
        OperationDto.UpdateOperationDto(
          TaskTable.name,
          updateBinaryConditions = mutableListOf(Triple(TaskTable.UID.name, BinaryPred.EQ, "qwerty")),
          updateRangeConditions = mutableListOf(),
          newValues = mutableMapOf(TaskTable.NAME.name to "SeverTask")
        )
      )
    )

    val connectionFactory = PostgresConnectionFactory("localhost", 5432, "postgres", "")
    val schemaName = "merge_wil_fail"
    PostgreXlogMerger(connectionFactory, schemaName).run {
      val dataSource = createProjectSnapshotDatabase(PROJECT_XML_TEMPLATE)
      assertFalse(tryMergeConcurrentUpdates(dataSource, listOf(serverChanges), listOf(clientChanges)).also {
        dataSource.shutdown()
      })
    }
    connectionFactory.close()
  }

  @Test
  fun `merge concurrent updates succeeds with column families`() {
    val clientChanges = XlogRecord(
      listOf(
        OperationDto.UpdateOperationDto(
          "task",
          updateBinaryConditions = mutableListOf(Triple(TaskTable.UID.name, BinaryPred.EQ, "qwerty")),
          updateRangeConditions = mutableListOf(),
          newValues = mutableMapOf(TaskTable.DURATION.name to "2")
        )
      )
    )
    val serverChanges = XlogRecord(
      listOf(
        OperationDto.UpdateOperationDto(
          TaskTable.name,
          updateBinaryConditions = mutableListOf(Triple(TaskTable.UID.name, BinaryPred.EQ, "qwerty")),
          updateRangeConditions = mutableListOf(),
          newValues = mutableMapOf(TaskTable.NAME.name to "New Name")
        )
      )
    )

    val connectionFactory = PostgresConnectionFactory("localhost", 5432, "postgres", "")
    val schemaName = "merge_will_succeed"
    PostgreXlogMerger(connectionFactory, schemaName).run {
      val dataSource = createProjectSnapshotDatabase(PROJECT_XML_TEMPLATE)
      assertTrue(tryMergeConcurrentUpdates(dataSource, listOf(serverChanges), listOf(clientChanges)).also {
        dataSource.shutdown()
      })
    }
    connectionFactory.close()
  }

  @Test
  fun `merge concurrent updates succeeds`() {
    val clientChanges = XlogRecord(
      listOf(
        OperationDto.UpdateOperationDto(
          "task",
          updateBinaryConditions = mutableListOf(Triple(TaskTable.UID.name, BinaryPred.EQ, "qwerty")),
          updateRangeConditions = mutableListOf(),
          newValues = mutableMapOf(TaskTable.NAME.name to "ClientTask")
        )
      )
    )
    val serverChanges = XlogRecord(
      listOf(
        OperationDto.InsertOperationDto(
          TaskTable.name,
          values = mutableMapOf(
            TaskTable.UID.name to "zxccvb",
            TaskTable.NAME.name to "SeverTask",
            TaskTable.NUM.name to "2",
            TaskTable.START_DATE.name to "2024-04-22",
            TaskTable.DURATION.name to "3"
          )
        )
      )
    )

    val connectionFactory = PostgresConnectionFactory("localhost", 5432, "postgres", "")
    val schemaName = "merge_will_succeed"
    PostgreXlogMerger(connectionFactory, schemaName).run {
      val dataSource = createProjectSnapshotDatabase(PROJECT_XML_TEMPLATE)
      assertTrue(tryMergeConcurrentUpdates(dataSource, listOf(serverChanges), listOf(clientChanges)).also {
        dataSource.shutdown()
      })
    }
    connectionFactory.close()
  }
}


private val PROJECT_XML_TEMPLATE = """
<?xml version="1.0" encoding="UTF-8"?>
<project name="" company="" webLink="" view-date="2022-01-01" view-index="0" gantt-divider-location="374" resource-divider-location="322" version="3.0.2906" locale="en">
  <tasks empty-milestones="true">
      <task id="0" uid="qwerty" name="Task1" color="#99ccff" meeting="false" start="2022-02-10" duration="25" complete="85" expand="true"/>
  </tasks>
</project>
        """.trimIndent()
