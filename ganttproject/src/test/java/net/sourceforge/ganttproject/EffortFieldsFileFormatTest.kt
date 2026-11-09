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
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.io.GanttXMLSaver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Locale

class EffortFieldsFileFormatTest {
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

  private fun projectXml(taskAttrs: String = "", resourceAttrs: String = "") = """
    <project name="p">
      <tasks>
        <task id="1" uid="u1" name="T" start="2026-11-09" duration="1" $taskAttrs/>
      </tasks>
      <resources>
        <resource id="0" name="R" function="Default:0" contacts="" phone="" $resourceAttrs/>
      </resources>
    </project>
  """.trimIndent()

  private fun save(project: IGanttProject): String {
    val out = ByteArrayOutputStream()
    GanttXMLSaver(project).save(out)
    return out.toString(Charsets.UTF_8)
  }

  @Test
  fun `effort, effort-driven flag and hours per day survive a save and load`() {
    val loaded = XmlProjectImporter().import(
      projectXml("""effort-hours="12.5" effort-driven="true"""", """hours-per-day="6.5""""))
    val saved = save(loaded)

    val reloaded = XmlProjectImporter().import(saved)
    val task = reloaded.taskManager.getTask(1)
    assertEquals(BigDecimal("12.5"), task.effort, "effort has to be read back from the saved file:\n$saved")
    assertTrue(task.isEffortDriven, "effort-driven flag has to be read back from the saved file:\n$saved")
    assertEquals(BigDecimal("6.5"), reloaded.humanResourceManager.getById(0).hoursPerDay,
      "hours per day have to be read back from the saved file:\n$saved")
  }

  @Test
  fun `loading the file saved before an edit brings the previous effort back`() {
    // Undo restores the project from a document saved before the edit, so what it needs is that
    // the saved document holds the effort as it was at that moment.
    val project = XmlProjectImporter().import(projectXml("""effort-hours="12.5""""))
    val before = save(project)

    val task = project.taskManager.getTask(1)
    task.createMutator().also {
      it.setEffort(BigDecimal("20"))
      it.commit()
    }
    assertEquals(BigDecimal("20"), task.effort)

    val restored = XmlProjectImporter().import(before)
    assertEquals(BigDecimal("12.5"), restored.taskManager.getTask(1).effort)
  }

  @Test
  fun `a file without the effort attributes loads with empty values`() {
    val project = XmlProjectImporter().import(projectXml())
    val task = project.taskManager.getTask(1)
    assertNull(task.effort)
    assertFalse(task.isEffortDriven)
    assertNull(project.humanResourceManager.getById(0).hoursPerDay)
  }

  @Test
  fun `attributes are not written when the values are not set`() {
    val saved = save(XmlProjectImporter().import(projectXml()))
    assertFalse(saved.contains("effort-hours"), "no effort was set, the document was:\n$saved")
    assertFalse(saved.contains("effort-driven"), "the flag was not set, the document was:\n$saved")
    assertFalse(saved.contains("hours-per-day"), "no hours per day were set, the document was:\n$saved")
  }
}
