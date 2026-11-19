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
import biz.ganttproject.core.model.task.RecurrenceOrigin
import biz.ganttproject.core.model.task.RecurrenceRule
import biz.ganttproject.core.model.task.RecurrenceUnit
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
import java.time.LocalDate
import java.util.Locale

class RecurrenceFileFormatTest {
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
  fun `a rule with an end date survives a save and load`() {
    val loaded = XmlProjectImporter().import(
      projectXml("""recurrence-unit="month" recurrence-interval="2" recurrence-until="2027-12-31""""))
    val saved = save(loaded)
    val task = XmlProjectImporter().import(saved).taskManager.getTask(1)
    assertEquals(RecurrenceRule(RecurrenceUnit.MONTH, 2, LocalDate.of(2027, 12, 31), null), task.recurrenceRule,
      "the rule has to be read back from the saved file:\n$saved")
    assertNull(task.recurrenceOrigin)
  }

  @Test
  fun `a rule with a count survives a save and load`() {
    val saved = save(XmlProjectImporter().import(
      projectXml("""recurrence-unit="week" recurrence-count="10"""")))
    val task = XmlProjectImporter().import(saved).taskManager.getTask(1)
    assertEquals(RecurrenceRule(RecurrenceUnit.WEEK, 1, null, 10), task.recurrenceRule, saved)
  }

  @Test
  fun `the origin of a repeated task and of a group survives a save and load`() {
    val withDate = save(XmlProjectImporter().import(projectXml("""recurrence-of="7" recurrence-date="2026-12-09"""")))
    assertEquals(RecurrenceOrigin(7, LocalDate.of(2026, 12, 9)),
      XmlProjectImporter().import(withDate).taskManager.getTask(1).recurrenceOrigin, withDate)

    val group = save(XmlProjectImporter().import(projectXml("""recurrence-of="7"""")))
    val origin = XmlProjectImporter().import(group).taskManager.getTask(1).recurrenceOrigin
    assertEquals(RecurrenceOrigin(7), origin, group)
    assertTrue(origin!!.isGroup)
  }

  @Test
  fun `a file without recurrence attributes loads without a rule and writes none`() {
    val project = XmlProjectImporter().import(projectXml())
    val task = project.taskManager.getTask(1)
    assertNull(task.recurrenceRule)
    assertNull(task.recurrenceOrigin)
    val saved = save(project)
    assertFalse(saved.contains("recurrence"), "nothing was set, the document was:\n$saved")
  }

  @Test
  fun `damaged recurrence attributes are ignored and do not stop the file from loading`() {
    val project = XmlProjectImporter().import(
      projectXml("""recurrence-unit="fortnight" recurrence-count="3" recurrence-of="x""""))
    val task = project.taskManager.getTask(1)
    assertEquals("T", task.name)
    assertNull(task.recurrenceRule)
  }

  @Test
  fun `loading the file saved before an edit brings the previous rule back`() {
    // Undo restores the project from a document saved before the edit.
    val project = XmlProjectImporter().import(
      projectXml("""recurrence-unit="week" recurrence-count="3""""))
    val before = save(project)
    project.taskManager.getTask(1).createMutator().also {
      it.setRecurrenceRule(RecurrenceRule(RecurrenceUnit.DAY, 1, null, 9))
      it.commit()
    }
    assertEquals(RecurrenceUnit.DAY, project.taskManager.getTask(1).recurrenceRule?.unit)
    assertEquals(RecurrenceUnit.WEEK,
      XmlProjectImporter().import(before).taskManager.getTask(1).recurrenceRule?.unit)
  }
}
