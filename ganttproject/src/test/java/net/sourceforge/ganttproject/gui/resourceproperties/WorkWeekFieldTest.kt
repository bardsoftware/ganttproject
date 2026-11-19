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
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.undo.UndoManagerImpl
import org.easymock.EasyMock.anyString
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.getCurrentArguments
import org.easymock.EasyMock.replay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.text.DateFormat
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.util.Locale

/**
 * The work week field of the resource dialog. It is written back when the dialog commits, which happens inside the
 * undoable edit of the dialog, so the tests save the panel inside such an edit. The week starts on Monday 2026-11-02;
 * the project calendar has the weekend free.
 */
class WorkWeekFieldTest {
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

  private fun date(day: LocalDate) = CalendarFactory.createGanttCalendar(day.year, day.monthValue - 1, day.dayOfMonth).time

  private fun resource(): HumanResource =
    project.humanResourceManager.create("anna", 0).also { it.hoursPerDay = BigDecimal(8) }

  private fun effortTask(effort: String, anna: HumanResource): Task =
    project.taskManager.newTaskBuilder().withName("t").withStartDate(date(monday))
      .withDuration(project.taskManager.createLength(1)).build().also { task ->
        task.assignmentCollection.addAssignment(anna).load = 100f
        task.createMutator().also {
          it.setEffort(BigDecimal(effort))
          it.setEffortDriven(true)
          it.commit()
        }
      }

  private fun days(taskId: Int) = project.taskManager.getTask(taskId).duration.length

  private fun workWeek() = project.humanResourceManager.getById(0).workWeek

  @Test
  fun `the field shows the stored work week in the notation of the project file`() {
    val anna = resource()
    anna.workWeek = listOf(WorkWeekChange(null, setOf(MONDAY, TUESDAY)), WorkWeekChange(monday, setOf()))
    assertEquals("1,2; 2026-11-02:", MainPropertiesPanel(anna).workWeekOption.value)
    assertEquals("", MainPropertiesPanel(resource()).workWeekOption.value)
  }

  @Test
  fun `a typed work week reaches the resource on commit and the task follows it`() {
    val anna = resource()
    val task = effortTask("24", anna)
    assertEquals(3, days(task.taskID))
    val panel = MainPropertiesPanel(anna)
    panel.workWeekOption.set("1,2")

    project.undoManager().undoableEdit("Resource properties changed") { panel.save() }
    assertEquals(listOf(WorkWeekChange(null, setOf(MONDAY, TUESDAY))), workWeek())
    assertEquals(6, days(task.taskID))
  }

  @Test
  fun `one undo gives back the previous work week and the longer duration`() {
    val anna = resource()
    anna.workWeek = listOf(WorkWeekChange(null, setOf(MONDAY, TUESDAY)))
    val task = effortTask("24", anna)
    assertEquals(6, days(task.taskID))
    val undoManager = project.undoManager()
    val panel = MainPropertiesPanel(anna)
    panel.workWeekOption.set("1,2,3,4,5")
    undoManager.undoableEdit("Resource properties changed") { panel.save() }
    assertEquals(3, days(task.taskID))

    undoManager.undo()
    assertEquals(listOf(WorkWeekChange(null, setOf(MONDAY, TUESDAY))), workWeek())
    assertEquals(6, days(task.taskID))
    assertFalse(undoManager.canUndo())
  }

  @Test
  fun `emptying the field gives the resource the project calendar back`() {
    val anna = resource()
    anna.workWeek = listOf(WorkWeekChange(null, setOf(MONDAY, TUESDAY)))
    val task = effortTask("24", anna)
    assertEquals(6, days(task.taskID))
    val panel = MainPropertiesPanel(anna)
    panel.workWeekOption.set("")

    project.undoManager().undoableEdit("Resource properties changed") { panel.save() }
    assertEquals(emptyList<WorkWeekChange>(), workWeek())
    assertEquals(3, days(task.taskID))
  }

  @Test
  fun `an unreadable work week is rejected by the field and keeps the stored one`() {
    val anna = resource()
    anna.workWeek = listOf(WorkWeekChange(null, setOf(MONDAY)))
    val panel = MainPropertiesPanel(anna)
    assertThrows<ValidationException> { panel.workWeekOption.validator.parse("1,8") }
    assertEquals("1, 2", panel.workWeekOption.validator.parse("1, 2"))
    panel.workWeekOption.set("1,8")

    panel.save()
    assertEquals(listOf(WorkWeekChange(null, setOf(MONDAY))), workWeek())
  }

  @Test
  fun `a work week without any working day is accepted with a warning`() {
    assertNull(workWeekWarning("1,2,3,4,5; 2027-05-01: 1,2"))
    assertNull(workWeekWarning(""))
    assertNull(workWeekWarning("1,8"))
    val fromDate = workWeekWarning("1,2; 2027-05-01:")
    val always = workWeekWarning(":")
    assertNotNull(fromDate)
    assertNotNull(always)
    assertNotEquals(fromDate, always)

    val anna = resource()
    val panel = MainPropertiesPanel(anna)
    panel.workWeekOption.set("1,2; 2027-05-01:")
    panel.save()
    assertEquals(listOf(WorkWeekChange(null, setOf(MONDAY, TUESDAY)), WorkWeekChange(LocalDate.of(2027, 5, 1), setOf())),
      workWeek())
  }

  @Test
  fun `a work week without a day in common with the project week is accepted with a warning`() {
    val projectWeek = projectWorkWeek(project.taskManager.calendar)
    assertEquals(setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY), projectWeek)
    assertNotNull(workWeekWarning("6,7", projectWeek))
    assertNotNull(workWeekWarning("1,2,3,4,5; 2027-05-01: 6", projectWeek))
    assertNull(workWeekWarning("1,6", projectWeek))
    assertNull(workWeekWarning("1,2,3,4,5,6", projectWeek))
    assertNull(workWeekWarning("", projectWeek))
    assertNull(workWeekWarning("1,8", projectWeek))
    // Without the week of the project nothing is known about it.
    assertNull(workWeekWarning("6,7"))
    // It is a different warning from the one for a week without any day.
    assertNotEquals(workWeekWarning(":", projectWeek), workWeekWarning("6,7", projectWeek))

    val anna = resource()
    val panel = MainPropertiesPanel(anna, project.taskManager.calendar)
    panel.workWeekOption.set("6,7")
    panel.save()
    assertEquals(listOf(WorkWeekChange(null, setOf(SATURDAY, SUNDAY))), anna.workWeek)
  }

  /**
   * A project which undo can restore: it has a document, forgets its tasks and resources when it is closed, and saves
   * and reads through the real project file format.
   */
  private class RestorableProject : GanttProjectImpl() {
    override var document: Document = FileDocument(Files.createTempFile("workweek", ".gan").toFile())
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
