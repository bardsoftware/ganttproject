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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Locale

class OriginalEffortFileFormatTest {
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

  private fun projectXml(taskAttrs: String = "") = """
    <project name="p">
      <tasks>
        <task id="1" uid="u1" name="T" start="2026-11-09" duration="1" $taskAttrs/>
      </tasks>
    </project>
  """.trimIndent()

  private fun save(project: IGanttProject): String {
    val out = ByteArrayOutputStream()
    GanttXMLSaver(project).save(out)
    return out.toString(Charsets.UTF_8)
  }

  @Test
  fun `original effort survives a save and load next to the planned and the actual effort`() {
    val loaded = XmlProjectImporter().import(
      projectXml("""effort-hours="12.5" actual-effort-hours="3.25" original-effort-hours="9""""))
    val saved = save(loaded)

    val task = XmlProjectImporter().import(saved).taskManager.getTask(1)
    assertEquals(BigDecimal("9"), task.originalEffort, "original effort has to be read back from the saved file:\n$saved")
    assertEquals(BigDecimal("12.5"), task.effort)
    assertEquals(BigDecimal("3.25"), task.actualEffort)
  }

  @Test
  fun `loading the file saved before an edit brings the previous original effort back`() {
    val project = XmlProjectImporter().import(projectXml("""original-effort-hours="2""""))
    val before = save(project)

    val task = project.taskManager.getTask(1)
    task.createMutator().also {
      it.setOriginalEffort(BigDecimal("9"))
      it.commit()
    }
    assertEquals(BigDecimal("9"), task.originalEffort)

    assertEquals(BigDecimal("2"), XmlProjectImporter().import(before).taskManager.getTask(1).originalEffort)
  }

  @Test
  fun `a file without the attribute loads with no original effort and keeps it so after a save`() {
    val loaded = XmlProjectImporter().import(projectXml("""effort-hours="4""""))
    assertNull(loaded.taskManager.getTask(1).originalEffort, "loading must not invent an original effort")
    assertEquals(BigDecimal("4"), loaded.taskManager.getTask(1).effort)
    assertFalse(save(loaded).contains("original-effort-hours"))
  }

  @Test
  fun `the attribute is not written when there is no original effort`() {
    val saved = save(XmlProjectImporter().import(projectXml()))
    assertFalse(saved.contains("original-effort-hours"), "no original effort was set, the document was:\n$saved")
  }
}
