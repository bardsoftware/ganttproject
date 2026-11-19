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
import java.text.DateFormat
import java.util.Locale

/** The deadline and the wait only and date fixed flags of a task in the project file. */
class LevellingFieldsFileFormatTest {
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
  fun `deadline, wait only and date fixed survive a save and load`() {
    val saved = save(XmlProjectImporter().import(
      projectXml("""deadline="2026-12-24" wait-only="true" date-fixed="true"""")))

    val task = XmlProjectImporter().import(saved).taskManager.getTask(1)
    assertEquals("2026-12-24", task.deadline?.toXMLString(), "deadline has to be read back from the saved file:\n$saved")
    assertTrue(task.isWaitOnly, "wait only has to be read back from the saved file:\n$saved")
    assertTrue(task.isDateFixed, "date fixed has to be read back from the saved file:\n$saved")
  }

  @Test
  fun `the fields set through the mutator are saved`() {
    val project = XmlProjectImporter().import(projectXml())
    project.taskManager.getTask(1).createMutator().also {
      it.setDeadline(CalendarFactory.createGanttCalendar(2027, 0, 15))
      it.setWaitOnly(true)
      it.setDateFixed(true)
      it.commit()
    }
    val saved = save(project)
    assertTrue(saved.contains("""deadline="2027-01-15""""), saved)
    assertTrue(saved.contains("""wait-only="true""""), saved)
    assertTrue(saved.contains("""date-fixed="true""""), saved)
  }

  @Test
  fun `the mutator clears the deadline`() {
    val project = XmlProjectImporter().import(projectXml("""deadline="2026-12-24""""))
    val task = project.taskManager.getTask(1)
    task.createMutator().also {
      it.setDeadline(null)
      it.commit()
    }
    assertNull(task.deadline)
  }

  @Test
  fun `a file without the fields loads with empty values and saves none`() {
    val project = XmlProjectImporter().import(projectXml())
    val task = project.taskManager.getTask(1)
    assertNull(task.deadline)
    assertFalse(task.isWaitOnly)
    assertFalse(task.isDateFixed)

    val saved = save(project)
    assertFalse(saved.contains("deadline"), "no deadline was set, the document was:\n$saved")
    assertFalse(saved.contains("wait-only"), "the flag was not set, the document was:\n$saved")
    assertFalse(saved.contains("date-fixed"), "the flag was not set, the document was:\n$saved")
  }
}
