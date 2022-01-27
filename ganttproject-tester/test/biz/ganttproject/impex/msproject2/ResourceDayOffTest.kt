/*
Copyright 2022 BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package biz.ganttproject.impex.msproject2

import junit.framework.TestCase
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.importer.ImporterFromGanttFile
import java.io.File
import java.text.SimpleDateFormat

/**
 * @author dbarashev@bardsoftware.com
 */
class ResourceDayOffTest : TestCase() {
  private val parser = SimpleDateFormat("yyyy-MM-dd")
  override fun setUp() {
    super.setUp()
    initLocale()
  }

  fun testImportResourceDayOffRange() {
    val project = GanttProjectImpl()
    val columns = ImporterFromGanttFile.VisibleFieldsImpl()
    val fileUrl = ProjectCalendarTest::class.java.getResource("/issue2000.xml") ?: error("Resource /issue2000.xml not found")
    val importer = ProjectFileImporter(project, columns, File(fileUrl.toURI()))
    importer.setPatchMspdi(false)
    importer.run()

    val worker = project.humanResourceManager.resources.firstOrNull { it.name == "Worker" } ?: error("Worker not found")
    assertEquals(2, worker.daysOff.size)
    assertEquals(parser.parse("2022-02-01"), worker.daysOff[0].start.time)
    assertEquals(parser.parse("2022-02-14"), worker.daysOff[1].start.time)
    assertEquals(parser.parse("2022-02-20"), worker.daysOff[1].finish.time)




  }

}
