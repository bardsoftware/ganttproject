/*
 * Copyright 2026 BarD Software s.r.o., Dmitry Barashev.
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
package biz.ganttproject.impex.csv

import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.time.TimeUnitStack
import com.google.common.base.Supplier
import junit.framework.TestCase
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.io.CSVOptions
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.RoleManager
import net.sourceforge.ganttproject.roles.RoleManagerImpl
import net.sourceforge.ganttproject.test.task.TaskTestCase
import org.apache.commons.csv.CSVFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.StringReader

class CriticalPathExportImportTest : TaskTestCase() {
  private var myHumanResourceManager: HumanResourceManager? = null
  private var myRoleManager: RoleManager? = null
  private var myTimeUnitStack: TimeUnitStack? = null

  @Throws(Exception::class)
  override fun setUp() {
    TaskDefaultColumn.setLocaleApi { key -> key }
    val builder = TestSetupHelper.newTaskManagerBuilder()
    taskManager = builder.build()
    myHumanResourceManager = builder.resourceManager
    myRoleManager = RoleManagerImpl()
    myTimeUnitStack = builder.timeUnitStack
  }

  @Throws(Exception::class)
  fun testExportCriticalPath() {
    val t1 = createTask()
    t1.setCritical(true)
    val t2 = createTask()
    t2.setCritical(false)

    val options = CSVOptions()
    // Ensure IS_CRITICAL is enabled (it should be by default now)
    assertTrue(
      "IS_CRITICAL option should be present",
      options.taskOptions.containsKey(TaskDefaultColumn.IS_CRITICAL.stub.getID())
    )
    options.taskOptions.get(TaskDefaultColumn.IS_CRITICAL.stub.getID())!!.setValue(true)

    val exporter = GanttCSVExport(taskManager, myHumanResourceManager, myRoleManager, options)
    val outputStream = ByteArrayOutputStream()
    exporter.createWriter(outputStream, SpreadsheetFormat.CSV).use { writer ->
      exporter.save(writer)
    }
    val csvOutput = outputStream.toString()
    val parser = CSVFormat.DEFAULT.withHeader().parse(StringReader(csvOutput))
    val records = parser.records

    TestCase.assertEquals(2, records.size)

    val r1 = records.get(0)
    TestCase.assertEquals("true", r1.get(TaskDefaultColumn.IS_CRITICAL.getName()))

    val r2 = records.get(1)
    TestCase.assertEquals("false", r2.get(TaskDefaultColumn.IS_CRITICAL.getName()))
  }

  @Throws(Exception::class)
  fun testImportIgnoresCriticalPath() {
    // Prepare data with translated header names if possible, or just the keys if i18n is mocked to return keys
    val csvData = ("tableColName,tableColIsCritical\n"
      + "Task1,true\n"
      + "Task2,false\n")

    TaskDefaultColumn.setLocaleApi(object : TaskDefaultColumn.LocaleApi {
      override fun i18n(key: String?): String? {
        return key
      }
    })

    val supplier: Supplier<InputStream?> = object : Supplier<InputStream?> {
      override fun get(): InputStream {
        return ByteArrayInputStream(csvData.toByteArray())
      }
    }
    val loader = GanttCSVOpen(
      supplier,
      SpreadsheetFormat.CSV,
      taskManager,
      myHumanResourceManager,
      myRoleManager,
      myTimeUnitStack
    )
    loader.load()

    val tasks = taskManager.getTasks()
    TestCase.assertEquals(2, tasks.size)

    // GanttProject's critical path is calculated, so it should be false by default if no dependencies exist
    // Even if we set it to true in CSV, it should be ignored.
    for (task in tasks) {
      assertFalse(
        "Task " + task.getName() + " should not be critical as CSV import should ignore it",
        task.isCritical()
      )
    }
  }
}
