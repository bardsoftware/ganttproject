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
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.parser.AllocationTagHandler
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

class ActualEffortFileFormatTest {
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
  fun `actual effort survives a save and load next to the planned effort`() {
    val loaded = XmlProjectImporter().import(projectXml("""effort-hours="12.5" actual-effort-hours="3.25""""))
    val saved = save(loaded)

    val task = XmlProjectImporter().import(saved).taskManager.getTask(1)
    assertEquals(BigDecimal("3.25"), task.actualEffort, "actual effort has to be read back from the saved file:\n$saved")
    assertEquals(BigDecimal("12.5"), task.effort)
  }

  @Test
  fun `loading the file saved before an edit brings the previous actual effort back`() {
    // Undo restores the project from a document saved before the edit.
    val project = XmlProjectImporter().import(projectXml("""actual-effort-hours="2""""))
    val before = save(project)

    val task = project.taskManager.getTask(1)
    task.createMutator().also {
      it.setActualEffort(BigDecimal("9"))
      it.commit()
    }
    assertEquals(BigDecimal("9"), task.actualEffort)

    assertEquals(BigDecimal("2"), XmlProjectImporter().import(before).taskManager.getTask(1).actualEffort)
  }

  @Test
  fun `a file without the attribute loads with no actual effort`() {
    val task = XmlProjectImporter().import(projectXml("""effort-hours="4"""")).taskManager.getTask(1)
    assertNull(task.actualEffort)
    assertEquals(BigDecimal("4"), task.effort)
  }

  @Test
  fun `the attribute is not written when there is no actual effort`() {
    val saved = save(XmlProjectImporter().import(projectXml("""effort-hours="4"""")))
    assertFalse(saved.contains("actual-effort-hours"), "no actual effort was set, the document was:\n$saved")
  }

  private fun assignedProjectXml(allocationAttrs: String = "", withAllocation: Boolean = true) = """
    <project name="p">
      <tasks>
        <task id="1" uid="u1" name="T" start="2026-11-09" duration="1"/>
      </tasks>
      <resources>
        <resource id="0" name="R" function="Default:0" contacts="" phone=""/>
      </resources>
      <allocations>
        ${if (withAllocation) """<allocation task-id="1" resource-id="0" function="Default:0" responsible="true" load="100.0" $allocationAttrs/>""" else ""}
      </allocations>
    </project>
  """.trimIndent()

  private fun IGanttProject.assignment() = taskManager.getTask(1).assignmentCollection.getAssignment(humanResourceManager.getById(0))

  @Test
  fun `actual effort of an assignment survives a save and load`() {
    val saved = save(XmlProjectImporter().import(assignedProjectXml("""actual-effort-hours="2.75"""")))
    assertTrue(Regex("<allocation [^>]*actual-effort-hours=\"2.75\"").containsMatchIn(saved), "the allocation has to carry the value:\n$saved")
    assertEquals(BigDecimal("2.75"), XmlProjectImporter().import(saved).assignment().actualEffort)
  }

  @Test
  fun `the file reader of the application loads the actual effort of an assignment`() {
    // Documents are opened through the tag handlers, not through XmlProjectImporter.
    val project = XmlProjectImporter().import(assignedProjectXml(withAllocation = false))
    AllocationTagHandler(project.humanResourceManager, project.taskManager, project.roleManager)
      .process(parseXmlProject(assignedProjectXml("""actual-effort-hours="1.5"""")))
    assertEquals(BigDecimal("1.5"), project.assignment().actualEffort)
  }

  @Test
  fun `an allocation without the attribute loads with no actual effort and none is written`() {
    val project = XmlProjectImporter().import(assignedProjectXml())
    assertNull(project.assignment().actualEffort)
    val saved = save(project)
    assertFalse(saved.contains("actual-effort-hours"), "no actual effort was set, the document was:\n$saved")
  }
}
