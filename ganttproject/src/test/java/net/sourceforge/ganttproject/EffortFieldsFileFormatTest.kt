/*
Copyright 2026 BarD Software s.r.o.

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
import biz.ganttproject.lib.fx.SimpleTreeCollapseView
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.parser.ResourceLoader
import net.sourceforge.ganttproject.parser.TaskTagHandler
import net.sourceforge.ganttproject.task.TaskManagerImpl
import net.sourceforge.ganttproject.undo.UndoableEditImpl
import net.sourceforge.ganttproject.undo.UndoableEditTxn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
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

  /**
   * An autosave document like the one the undo manager writes before and after each edit. Reading it
   * replaces the tasks and resources of the project with the saved ones, using the same loaders
   * as ProxyDocument does when undo restores a project.
   */
  private fun autosaveDocument(project: IGanttProject): Document {
    val file = File.createTempFile("effort-undo", ".gan").also { it.deleteOnExit() }
    return object : Document by FileDocument(file) {
      override fun write() {
        file.writeText(save(project))
      }

      override fun read() {
        val xmlProject = parseXmlProject(file.readText())
        (project.taskManager as TaskManagerImpl).projectListener.projectClosed()
        project.humanResourceManager.clear()
        ResourceLoader(project.humanResourceManager, project.roleManager, project.resourceCustomPropertyManager)
          .loadResources(xmlProject)
        TaskTagHandler(project.taskManager, SimpleTreeCollapseView()).process(xmlProject)
      }
    }
  }

  private val noTxn = object : UndoableEditTxn {
    override fun start(displayName: String) {}
    override fun commit() {}
    override fun rollback(ex: Throwable) {}
    override fun undo() {}
    override fun redo() {}
  }

  @Test
  fun `effort, effort-driven flag and hours per day survive a save and load`() {
    val loaded = XmlProjectImporter().import(
      projectXml("""effort="1.5" effort-driven="true"""", """hours-per-day="6.5""""))
    val saved = save(loaded)

    val reloaded = XmlProjectImporter().import(saved)
    val task = reloaded.taskManager.getTask(1)
    assertEquals(BigDecimal("1.5"), task.effort, "effort has to be read back from the saved file:\n$saved")
    assertTrue(task.isEffortDriven, "effort-driven flag has to be read back from the saved file:\n$saved")
    assertEquals(BigDecimal("6.5"), reloaded.humanResourceManager.getById(0).hoursPerDay,
      "hours per day have to be read back from the saved file:\n$saved")
  }

  @Test
  fun `effort of whole days is saved and loaded unchanged`() {
    val saved = save(XmlProjectImporter().import(projectXml("""effort="2"""")))
    assertTrue(saved.contains(""" effort="2""""), "effort has to be written as a number of days:\n$saved")

    val task = XmlProjectImporter().import(saved).taskManager.getTask(1)
    assertEquals(BigDecimal("2"), task.effort, "effort has to be read back from the saved file:\n$saved")
  }

  @Test
  fun `effort of half a day is saved and loaded unchanged`() {
    val saved = save(XmlProjectImporter().import(projectXml("""effort="1.5"""")))
    assertTrue(saved.contains(""" effort="1.5""""), "effort has to be written as a number of days:\n$saved")

    val task = XmlProjectImporter().import(saved).taskManager.getTask(1)
    assertEquals(BigDecimal("1.5"), task.effort, "effort has to be read back from the saved file:\n$saved")
  }

  @Test
  fun `undo of an edit brings the previous effort, flag and hours per day back`() {
    val project = XmlProjectImporter().import(
      projectXml("""effort="1.5" effort-driven="true"""", """hours-per-day="6.5""""))
    val edit = UndoableEditImpl(UndoableEditImpl.Args(
      displayName = "Edit effort",
      newAutosave = { autosaveDocument(project) },
      restore = { it.read() },
      txn = noTxn
    )) {
      project.taskManager.getTask(1).createMutator().also {
        it.setEffort(BigDecimal("2"))
        it.setEffortDriven(false)
        it.commit()
      }
      project.humanResourceManager.getById(0).hoursPerDay = BigDecimal("8")
    }
    assertEquals(BigDecimal("2"), project.taskManager.getTask(1).effort)

    edit.undo()
    val restored = project.taskManager.getTask(1)
    assertEquals(BigDecimal("1.5"), restored.effort)
    assertTrue(restored.isEffortDriven)
    assertEquals(BigDecimal("6.5"), project.humanResourceManager.getById(0).hoursPerDay)

    edit.redo()
    val redone = project.taskManager.getTask(1)
    assertEquals(BigDecimal("2"), redone.effort)
    assertFalse(redone.isEffortDriven)
    assertEquals(BigDecimal("8"), project.humanResourceManager.getById(0).hoursPerDay)
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
    assertFalse(saved.contains("effort="), "no effort was set, the document was:\n$saved")
    assertFalse(saved.contains("effort-driven"), "the flag was not set, the document was:\n$saved")
    assertFalse(saved.contains("hours-per-day"), "no hours per day were set, the document was:\n$saved")
  }
}
