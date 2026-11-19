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

import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/**
 * Taking a day off away from a resource gives the effort driven tasks of that resource their shorter duration, in one
 * undoable edit, and undoing the edit gives the day off and the longer duration back. The edit is made the way the
 * person dialog makes it: the whole list of days off is written back at once.
 */
class DaysOffRemovalUndoTest {
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

  private fun dayOff(first: LocalDate) = GanttDaysOff(date(first), date(first.plusDays(1)))

  private fun resource(): HumanResource =
    project.humanResourceManager.create("anna", 0).also { it.hoursPerDay = BigDecimal(8) }

  private fun effortTask(effort: String, anna: HumanResource) =
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

  private fun daysOff() = project.humanResourceManager.getById(0).daysOff.map { it.start.toLocalDate() }

  @Test
  fun `removing one of two days off shortens the task by that day only`() {
    val anna = resource()
    anna.addDaysOff(dayOff(monday.plusDays(1)))
    anna.addDaysOff(dayOff(monday.plusDays(3)))
    val task = effortTask("24", anna)
    // Monday, Tuesday off, Wednesday, Thursday off, Friday.
    assertEquals(5, days(task.taskID))
    anna.removeDaysOff(anna.daysOff[0])
    // Monday, Tuesday, Wednesday: the second day off lies behind the end of the task.
    assertEquals(3, days(task.taskID))
    assertEquals(listOf(monday.plusDays(3)), daysOff())
  }

  @Test
  fun `one undo gives back the day off and the longer duration`() {
    val anna = resource()
    anna.addDaysOff(dayOff(monday.plusDays(1)))
    val task = effortTask("24", anna)
    assertEquals(4, days(task.taskID))
    val undoManager = project.undoManager()

    undoManager.undoableEdit("Resource properties changed") { anna.setDaysOff(emptyList()) }
    assertEquals(3, days(task.taskID))
    assertEquals(emptyList<LocalDate>(), daysOff())

    undoManager.undo()
    assertEquals(listOf(monday.plusDays(1)), daysOff())
    assertEquals(4, days(task.taskID))
    assertFalse(undoManager.canUndo())
  }

  /**
   * A project which undo can restore: it has a document, forgets its tasks and resources when it is closed, and saves
   * and reads through the real project file format.
   */
  private class RestorableProject : GanttProjectImpl() {
    override var document: Document = FileDocument(Files.createTempFile("daysoff", ".gan").toFile())
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
