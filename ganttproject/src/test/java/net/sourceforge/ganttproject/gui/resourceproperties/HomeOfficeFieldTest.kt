/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui.resourceproperties

import biz.ganttproject.core.model.task.HomeOfficePeriod
import biz.ganttproject.core.model.task.WorkWeekChange
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.PrjInfos
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.document.testProxyDocument
import net.sourceforge.ganttproject.document.testUiFacade
import net.sourceforge.ganttproject.io.GanttXMLOpen
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.parser.ParserFactory
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.undo.UndoManagerImpl
import org.easymock.EasyMock.anyString
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.getCurrentArguments
import org.easymock.EasyMock.replay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.text.DateFormat
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.FRIDAY
import java.time.LocalDate
import java.util.Locale

/**
 * The fields for the days at home in the resource dialog, written back when the dialog commits inside its undoable
 * edit, like the work week field. The week starts on Monday 2026-11-02.
 */
class HomeOfficeFieldTest {
  private lateinit var project: RestorableProject
  private val monday = LocalDate.of(2026, 11, 2)

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
    project = RestorableProject()
  }

  private fun resource(): HumanResource =
    project.humanResourceManager.create("anna", 0).also { it.hoursPerDay = BigDecimal(8) }

  private fun anna() = project.humanResourceManager.getById(0)

  @Test
  fun `the fields show what is stored in the notation of the project file`() {
    val anna = resource()
    anna.homeOfficeWeek = listOf(WorkWeekChange(null, setOf(FRIDAY)), WorkWeekChange(monday, setOf()))
    anna.homeOfficePeriods = listOf(HomeOfficePeriod(monday, monday.plusDays(3)))
    val panel = MainPropertiesPanel(anna)
    assertEquals("5; 2026-11-02:", panel.homeOfficeWeekOption.value)
    assertEquals("2026-11-02..2026-11-04", panel.homeOfficePeriodsOption.value)
    assertEquals("", MainPropertiesPanel(resource()).homeOfficePeriodsOption.value)
  }

  @Test
  fun `typed days at home reach the resource on commit and one undo takes them back`() {
    val anna = resource()
    val undoManager = project.undoManager()
    val panel = MainPropertiesPanel(anna)
    panel.homeOfficeWeekOption.set("1,5")
    panel.homeOfficePeriodsOption.set("2026-11-09..2026-11-10")
    undoManager.undoableEdit("Resource properties changed") { panel.save() }
    assertEquals(listOf(WorkWeekChange(null, setOf(MONDAY, FRIDAY))), anna().homeOfficeWeek)
    assertEquals(listOf(HomeOfficePeriod(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 11))), anna().homeOfficePeriods)
    assertTrue(anna().daysOff.isEmpty())

    undoManager.undo()
    assertEquals(emptyList<WorkWeekChange>(), anna().homeOfficeWeek)
    assertEquals(emptyList<HomeOfficePeriod>(), anna().homeOfficePeriods)
    assertFalse(undoManager.canUndo())
  }

  @Test
  fun `unreadable days at home are rejected by the fields and keep the stored ones`() {
    val anna = resource()
    anna.homeOfficeWeek = listOf(WorkWeekChange(null, setOf(MONDAY)))
    anna.homeOfficePeriods = listOf(HomeOfficePeriod(monday, monday.plusDays(1)))
    val panel = MainPropertiesPanel(anna)
    assertThrows<ValidationException> { panel.homeOfficeWeekOption.validator.parse("1,8") }
    assertThrows<ValidationException> { panel.homeOfficePeriodsOption.validator.parse("2026-11-04..2026-11-02") }
    assertEquals("2026-11-02", panel.homeOfficePeriodsOption.validator.parse("2026-11-02"))
    panel.homeOfficeWeekOption.set("1,8")
    panel.homeOfficePeriodsOption.set("soon")

    panel.save()
    assertEquals(listOf(WorkWeekChange(null, setOf(MONDAY))), anna().homeOfficeWeek)
    assertEquals(listOf(HomeOfficePeriod(monday, monday.plusDays(1))), anna().homeOfficePeriods)
  }

  @Test
  fun `emptying the fields ends the days at home`() {
    val anna = resource()
    anna.homeOfficeWeek = listOf(WorkWeekChange(null, setOf(MONDAY)))
    anna.homeOfficePeriods = listOf(HomeOfficePeriod(monday, monday.plusDays(1)))
    val panel = MainPropertiesPanel(anna)
    panel.homeOfficeWeekOption.set("")
    panel.homeOfficePeriodsOption.set("")
    panel.save()
    assertTrue(anna().homeOffice.isEmpty)
  }

  /**
   * A project which undo can restore: it has a document, forgets its tasks and resources when it is closed, and saves
   * and reads through the real project file format.
   */
  private class RestorableProject : GanttProjectImpl() {
    override var document: Document = FileDocument(Files.createTempFile("homeoffice", ".gan").toFile())
    private val uiFacade = testUiFacade()
    private val parserFactory = object : ParserFactory {
      override fun newParser() = GanttXMLOpen(PrjInfos(), taskManager, uiFacade)
      override fun newSaver() = GanttXMLSaver(this@RestorableProject)
    }

    init {
      addProjectEventListener(taskManager.projectListener)
      addProjectEventListener(humanResourceManager.projectListener)
    }

    override fun close() = fireProjectClosed()

    fun undoManager(): UndoManagerImpl {
      val documentManager = createNiceMock<DocumentManager>(DocumentManager::class.java)
      expect(documentManager.getDocument(anyString())).andStubAnswer {
        testProxyDocument(FileDocument(File(getCurrentArguments()[0] as String)), this, uiFacade, parserFactory)
      }
      replay(documentManager)
      return UndoManagerImpl(this, parserFactory, documentManager)
    }
  }
}
