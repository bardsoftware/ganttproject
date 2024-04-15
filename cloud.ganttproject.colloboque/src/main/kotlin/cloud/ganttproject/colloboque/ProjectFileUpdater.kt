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

import biz.ganttproject.core.calendar.WeekendCalendarImpl
import biz.ganttproject.core.io.XmlProjectImporter
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.TaskManagerConfigImpl
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.RoleManager
import net.sourceforge.ganttproject.storage.*
import net.sourceforge.ganttproject.task.*
import org.h2.jdbcx.JdbcDataSource
import java.io.ByteArrayOutputStream
import java.util.*

private var databaseCounter: Long = 0
private fun createInMemoryDatabase(): ProjectDatabase {
  val dataSource = JdbcDataSource()
  dataSource.setURL("jdbc:h2:mem:update${databaseCounter++};DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true")
  return SqlProjectDatabaseImpl(dataSource)
}

/**
 * This function applies the `updates` to the contents of `projectXml` and returns the updated XML as a String.
 */
fun updateProjectXml(projectXml: String, updates: XlogRecord): String {
  val calendar = WeekendCalendarImpl()
  val humanResourceManager = HumanResourceManager(
    RoleManager.Access.getInstance().defaultRole,
    CustomColumnsManager()
  )
  val taskManagerConfig = TaskManagerConfigImpl(humanResourceManager, calendar)
  val taskManager = TaskManagerImpl(null, taskManagerConfig)
  val projectDatabase = LazyProjectDatabaseProxy(
    databaseFactory = {  createInMemoryDatabase() },
    taskManager = {taskManager}
  ).also {
    it.startLog(0)
  }
  val project = GanttProjectImpl(taskManager, projectDatabase)
  XmlProjectImporter(project).import(projectXml)
  taskManager.tasks.forEach(projectDatabase::insertTask)

  if (updates.colloboqueOperations.isEmpty()) {
    return projectXml
  }

  projectDatabase.addExternalUpdatesListener {
    val hierarchy = taskManager.taskHierarchy.export()
    taskManager.reset()
    val databaseTasks = projectDatabase.readAllTasks()
    taskManager.importFromDatabase(databaseTasks, hierarchy)
  }
  projectDatabase.applyUpdate(listOf(updates), 0, 1)
  val output = ByteArrayOutputStream()
  GanttXMLSaver(project).save(output)
  return output.toString(Charsets.UTF_8)
}

fun projectFromXml(projectXml: String, baseTxnId: BaseTxnId, databaseFactory: () -> ProjectDatabase): ProjectDatabase {
  val calendar = WeekendCalendarImpl()
  val humanResourceManager = HumanResourceManager(
    RoleManager.Access.getInstance().defaultRole,
    CustomColumnsManager()
  )
  val taskManagerConfig = TaskManagerConfigImpl(humanResourceManager, calendar)
  val taskManager = TaskManagerImpl(null, taskManagerConfig)
  val projectDatabase = LazyProjectDatabaseProxy(
    databaseFactory = databaseFactory,
    taskManager = {taskManager}
  ).also {
    it.startLog(baseTxnId)
  }
  val project = GanttProjectImpl(taskManager, projectDatabase)
  XmlProjectImporter(project).import(projectXml)
  taskManager.tasks.forEach(projectDatabase::insertTask)
  return projectDatabase
}

internal val PROJECT_XML_TEMPLATE = """
<?xml version="1.0" encoding="UTF-8"?>
<project name="" company="" webLink="" view-date="2022-01-01" view-index="0" gantt-divider-location="374" resource-divider-location="322" version="3.0.2906" locale="en">
  <tasks empty-milestones="true">
      <task id="0" uid="qwerty" name="Task1" color="#99ccff" meeting="false" start="2022-02-10" duration="25" complete="85" expand="true"/>
  </tasks>
</project>
        """.trimIndent()
internal fun String.toBase64() = Base64.getEncoder().encodeToString(this.toByteArray())
