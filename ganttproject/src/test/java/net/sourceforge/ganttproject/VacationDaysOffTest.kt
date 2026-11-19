/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

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
package net.sourceforge.ganttproject

import biz.ganttproject.core.io.XmlProjectImporter
import biz.ganttproject.core.io.parseXmlProject
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import biz.ganttproject.core.time.GanttCalendar
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.parser.ResourceLoader
import net.sourceforge.ganttproject.parser.daysOffFromFile
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.algorithm.daysOffRanges
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * The end of a vacation is exclusive. A vacation which ends on its start, or before it, covers no day, and both readers
 * of the file, the desktop one and the one of the cloud, read it as the single day of its start. A vacation which
 * covers days is read as it stands, and so are all the vacations of files written before.
 */
class VacationDaysOffTest {
  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
  }

  private fun xml(vararg vacations: Pair<String, String>) = """
    <project name="p">
      <resources>
        <resource id="0" name="R" function="Default:0"/>
      </resources>
      <vacations>
        ${vacations.joinToString("\n") { (start, end) -> """<vacation start="${start}" end="${end}" resourceid="0"/>""" }}
      </vacations>
    </project>
  """.trimIndent()

  private fun date(text: String) = GanttCalendar.parseXMLDate(text)

  private fun desktop(xml: String): HumanResource {
    val project = GanttProjectImpl()
    ResourceLoader(project.humanResourceManager, project.roleManager, project.resourceCustomPropertyManager)
      .loadResources(parseXmlProject(xml))
    return project.humanResourceManager.getById(0)
  }

  private fun cloud(xml: String): HumanResource = XmlProjectImporter().import(xml).humanResourceManager.getById(0)

  private fun ranges(resource: HumanResource) = resource.daysOffRanges()

  private fun day(day: Int) = LocalDate.of(2026, 11, day)

  @Test
  fun `a vacation which covers days is read as it stands`() {
    val file = xml("2026-11-03" to "2026-11-06")
    assertEquals(listOf(day(3) to day(6)), ranges(desktop(file)))
    assertEquals(listOf(day(3) to day(6)), ranges(cloud(file)))
  }

  @Test
  fun `a vacation with the same start and end is the single day of its start`() {
    val file = xml("2026-11-03" to "2026-11-03")
    assertEquals(listOf(day(3) to day(4)), ranges(desktop(file)))
    assertEquals(listOf(day(3) to day(4)), ranges(cloud(file)))
  }

  @Test
  fun `a vacation which ends before it starts is the single day of its start`() {
    val file = xml("2026-11-05" to "2026-11-02")
    assertEquals(listOf(day(5) to day(6)), ranges(desktop(file)))
    assertEquals(listOf(day(5) to day(6)), ranges(cloud(file)))
  }

  @Test
  fun `the single day at the end of a month ends on the first of the next one`() {
    val days = daysOffFromFile(date("2026-11-30"), date("2026-11-30"), 0)
    assertEquals(LocalDate.of(2026, 12, 1), days.finish.toLocalDate())
  }

  @Test
  fun `a corrected vacation is saved as the single day and loaded back unchanged`() {
    val project = XmlProjectImporter().import(xml("2026-11-03" to "2026-11-03"))
    val out = ByteArrayOutputStream()
    GanttXMLSaver(project).save(out)
    val saved = out.toString(Charsets.UTF_8)
    assertTrue(saved.contains("""end="2026-11-04""""), saved)
    assertEquals(listOf(day(3) to day(4)), ranges(cloud(saved)))
  }

  private fun reported(xml: String): List<Triple<String, LocalDate, LocalDate>> {
    val problems = mutableListOf<Triple<String, LocalDate, LocalDate>>()
    val project = GanttProjectImpl()
    ResourceLoader(project.humanResourceManager, project.roleManager, project.resourceCustomPropertyManager) { name, start, end ->
      problems.add(Triple(name, start, end))
    }.loadResources(parseXmlProject(xml))
    return problems
  }

  @Test
  fun `a vacation which ends before it starts is reported to the user`() {
    assertEquals(listOf(Triple("R", day(5), day(2))), reported(xml("2026-11-05" to "2026-11-02")))
  }

  @Test
  fun `a vacation with the same start and end is reported to the user`() {
    assertEquals(listOf(Triple("R", day(3), day(3))), reported(xml("2026-11-03" to "2026-11-03")))
  }

  @Test
  fun `a vacation which covers days is not reported`() {
    assertEquals(emptyList<Triple<String, LocalDate, LocalDate>>(), reported(xml("2026-11-03" to "2026-11-06")))
  }
}
