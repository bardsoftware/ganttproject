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
import net.sourceforge.ganttproject.resource.ResourceEvent
import net.sourceforge.ganttproject.resource.ResourceView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Locale

/** The two flags of a resource assignment, blocking and no effort, in the project file. */
class AssignmentAxesFileFormatTest {
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

  private fun projectXml(allocationAttrs: String = "") = """
    <project name="p">
      <tasks>
        <task id="1" uid="u1" name="T" start="2026-11-09" duration="5"/>
      </tasks>
      <resources>
        <resource id="0" name="R" function="Default:0"/>
      </resources>
      <allocations>
        <allocation task-id="1" resource-id="0" function="Default:0" responsible="true" load="100.0" $allocationAttrs/>
      </allocations>
    </project>
  """.trimIndent()

  private fun save(project: IGanttProject): String {
    val out = ByteArrayOutputStream()
    GanttXMLSaver(project).save(out)
    return out.toString(Charsets.UTF_8)
  }

  private fun assignmentOf(project: IGanttProject) = project.taskManager.getTask(1).assignments.single()

  @Test
  fun `both flags survive a save and load`() {
    val saved = save(XmlProjectImporter().import(projectXml("""blocking="true" no-effort="true"""")))

    val assignment = assignmentOf(XmlProjectImporter().import(saved))
    assertTrue(assignment.isBlocking, "blocking has to be read back from the saved file:\n$saved")
    assertTrue(assignment.isNoEffort, "no effort has to be read back from the saved file:\n$saved")
    assertEquals(100f, assignment.load)
  }

  @Test
  fun `the flags are independent of each other`() {
    val onlyBlocking = assignmentOf(XmlProjectImporter().import(projectXml("""blocking="true"""")))
    assertTrue(onlyBlocking.isBlocking)
    assertFalse(onlyBlocking.isNoEffort)

    val onlyNoEffort = assignmentOf(XmlProjectImporter().import(projectXml("""no-effort="true"""")))
    assertFalse(onlyNoEffort.isBlocking)
    assertTrue(onlyNoEffort.isNoEffort)
  }

  @Test
  fun `the flags set on an assignment are saved`() {
    val project = XmlProjectImporter().import(projectXml())
    assignmentOf(project).also {
      it.isBlocking = true
      it.isNoEffort = true
    }
    val saved = save(project)
    assertTrue(saved.contains("""blocking="true""""), saved)
    assertTrue(saved.contains("""no-effort="true""""), saved)
  }

  @Test
  fun `the allocation tag handler reads the flags`() {
    val project = XmlProjectImporter().import(projectXml())
    val target = XmlProjectImporter().import(projectXml())
    assignmentOf(target).delete()
    AllocationTagHandler(target.humanResourceManager, target.taskManager, target.roleManager)
      .process(parseXmlProject(projectXml("""blocking="true" no-effort="true"""")))
    val assignment = assignmentOf(target)
    assertTrue(assignment.isBlocking)
    assertTrue(assignment.isNoEffort)
    assertFalse(assignmentOf(project).isBlocking)
  }

  @Test
  fun `a file without the flags loads with both off and saves none`() {
    val project = XmlProjectImporter().import(projectXml())
    val assignment = assignmentOf(project)
    assertFalse(assignment.isBlocking)
    assertFalse(assignment.isNoEffort)

    val saved = save(project)
    assertFalse(saved.contains("blocking"), "the flag was not set, the document was:\n$saved")
    assertFalse(saved.contains("no-effort"), "the flag was not set, the document was:\n$saved")
  }

  @Test
  fun `explicit false values load like a file without the flags`() {
    val assignment = assignmentOf(XmlProjectImporter().import(projectXml("""blocking="false" no-effort="false"""")))
    assertFalse(assignment.isBlocking)
    assertFalse(assignment.isNoEffort)
  }

  // An older GanttProject reads a file with the flags because neither reader rejects an attribute it does not know,
  // and saves it back without them. This is the same mechanism with an attribute which the current version does not
  // know.
  @Test
  fun `an unknown attribute of an allocation is ignored by both readers`() {
    val xml = projectXml("""future-flag="true"""")
    assertEquals(100f, assignmentOf(XmlProjectImporter().import(xml)).load)

    val target = XmlProjectImporter().import(projectXml())
    assignmentOf(target).delete()
    AllocationTagHandler(target.humanResourceManager, target.taskManager, target.roleManager)
      .process(parseXmlProject(xml))
    assertEquals(100f, assignmentOf(target).load)
    assertFalse(save(target).contains("future-flag"))
  }

  @Test
  fun `a new assignment has both flags off`() {
    val project = XmlProjectImporter().import(projectXml())
    val task = project.taskManager.getTask(1)
    assignmentOf(project).delete()
    val assignment = task.assignmentCollection.addAssignment(project.humanResourceManager.getById(0))
    assertFalse(assignment.isBlocking)
    assertFalse(assignment.isNoEffort)
  }

  @Test
  fun `the flags of an assignment added through the mutator survive the commit`() {
    val project = XmlProjectImporter().import(projectXml())
    val task = project.taskManager.getTask(1)
    assignmentOf(project).delete()
    task.assignmentCollection.createMutator().also {
      it.addAssignment(project.humanResourceManager.getById(0)).apply {
        isBlocking = true
        isNoEffort = true
      }
      it.commit()
    }
    val assignment = task.assignments.single()
    assertTrue(assignment.isBlocking)
    assertTrue(assignment.isNoEffort)
  }

  @Test
  fun `changing a flag tells the resource manager`() {
    val project = XmlProjectImporter().import(projectXml())
    var events = 0
    project.humanResourceManager.addView(object : ResourceView {
      override fun resourceAdded(event: ResourceEvent) {}
      override fun resourcesRemoved(event: ResourceEvent) {}
      override fun resourceChanged(event: ResourceEvent) {}
      override fun resourceAssignmentsChanged(event: ResourceEvent) { events++ }
      override fun resourceStructureChanged() {}
      override fun resourceModelReset() {}
    })
    val assignment = assignmentOf(project)
    assignment.isBlocking = true
    assignment.isNoEffort = true
    assertEquals(2, events)
    // Writing the same value again is not a change.
    assignment.isBlocking = true
    assignment.isNoEffort = true
    assertEquals(2, events)
  }
}
