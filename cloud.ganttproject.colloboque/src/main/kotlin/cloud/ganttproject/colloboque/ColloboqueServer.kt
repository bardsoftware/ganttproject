/*
Copyright 2022 BarD Software s.r.o., GanttProject Cloud OU

This file is part of GanttProject Cloud.

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

import biz.ganttproject.core.io.XmlTasks
import biz.ganttproject.core.io.parseXmlProject
import biz.ganttproject.core.io.walkTasksDepthFirst
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.lib.fx.SimpleTreeCollapseView
import kotlinx.coroutines.channels.Channel
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.parser.TaskLoader
import net.sourceforge.ganttproject.storage.buildInsertTaskQuery
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderNameCase
import org.jooq.impl.DSL
import java.text.DateFormat
import java.util.*
import javax.sql.DataSource
import net.sourceforge.ganttproject.storage.InitRecord
import net.sourceforge.ganttproject.storage.InputXlog

class ColloboqueServer(
  private val dataSourceFactory: (projectRefid: String) -> DataSource,
  private val initInputChannel: Channel<InitRecord>,
  private val updateInputChannel: Channel<InputXlog>) {

  fun init(projectRefid: String, debugCreateProject: Boolean) {
    dataSourceFactory(projectRefid).let { ds ->
      ds.connection.use {
        it.createStatement().executeQuery("SELECT uid FROM Task").use { rs ->
          while (rs.next()) {
            println(rs.getString(1))
          }
        }
        if (debugCreateProject) {
          DSL.using(it, SQLDialect.POSTGRES)
            .configuration()
            .deriveSettings { it.withRenderNameCase(RenderNameCase.LOWER) }
            .dsl().let { dsl ->

              loadProject("""
<?xml version="1.0" encoding="UTF-8"?>
<project name="" company="" webLink="" view-date="2022-01-01" view-index="0" gantt-divider-location="374" resource-divider-location="322" version="3.0.2906" locale="en">
    <tasks empty-milestones="true">
        <task id="0" uid="qwerty" name="Task1" color="#99ccff" meeting="false" start="2022-02-10" duration="25" complete="85" expand="true"/>
    </tasks>
</project>
          """.trimIndent(), dsl)
            }
        }
      }
    }
  }
}


private fun loadProject(xmlInput: String, dsl: DSLContext) {
  object : CalendarFactory() {
    init {
      setLocaleApi(object : LocaleApi {
        override fun getLocale(): Locale {
          return Locale.US
        }

        override fun getShortDateFormat(): DateFormat {
          return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        }
      })
    }
  }
  val bufferProject = GanttProjectImpl()
  val taskLoader = TaskLoader(bufferProject.taskManager, SimpleTreeCollapseView())
  parseXmlProject(xmlInput).let { xmlProject ->
    taskLoader.loadTaskCustomPropertyDefinitions(xmlProject)
    xmlProject.walkTasksDepthFirst { parent: XmlTasks.XmlTask?, child: XmlTasks.XmlTask ->
      taskLoader.loadTask(parent, child)
      true
    }
  }
  bufferProject.taskManager.tasks.forEach { task ->
    buildInsertTaskQuery(dsl, task).execute()
  }

}
