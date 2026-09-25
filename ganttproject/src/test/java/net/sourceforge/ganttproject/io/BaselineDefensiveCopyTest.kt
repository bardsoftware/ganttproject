/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.io

import biz.ganttproject.core.io.parseXmlProject
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.GanttPreviousState
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.parser.BaselineSerializer
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Calendar
import java.util.Locale
import javax.xml.transform.stream.StreamResult

/**
 * The other half of what a baseline promises: the dates it took stay its own even against the
 * code that reads it.
 *
 * BaselineSnapshotTest covers the direction from the task towards the baseline: createTasks
 * clones the live start date, so rescheduling a task leaves its baselines alone. These tests
 * cover the opposite direction. getTasks() hands out the list itself and each entry hands out
 * its GanttCalendar itself, and GanttCalendar inherits public mutators from GregorianCalendar
 * without overriding a single one. A consumer of a baseline can therefore move it, and because
 * baselines now live in memory there is no re-read from a file to undo that: the moved date is
 * what the project file gets.
 */
class BaselineDefensiveCopyTest : SaverBase() {

  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
  }

  @Test
  fun `a baseline keeps its date when a caller changes what getStart handed out`() {
    val fixture = newBaseline()

    fixture.startOfTask().add(Calendar.YEAR, 1)

    assertEquals("2026-09-11", fixture.startOfTask().toXMLString(),
      "the baseline has to keep the date it took, whatever a caller does to what it handed out")
  }

  @Test
  fun `a baseline keeps its tasks when a caller clears the list getTasks handed out`() {
    val fixture = newBaseline()
    assertEquals(1, fixture.baseline.tasks.count { it.id == fixture.taskId },
      "the baseline has to hold the task before anything touches it")

    val handedOut = fixture.baseline.tasks
    var refused = false
    try {
      handedOut.clear()
    } catch (e: UnsupportedOperationException) {
      refused = true
    }

    assertAll(
      {
        assertEquals(1, fixture.baseline.tasks.count { it.id == fixture.taskId },
          "the baseline has to keep its tasks, whatever a caller does to the list it handed out")
      },
      {
        assertEquals(true, refused,
          "clearing the list a baseline handed out has to be refused with UnsupportedOperationException")
      }
    )
  }

  @Test
  fun `the saved project keeps the date the baseline took`() {
    val fixture = newBaseline()

    fixture.startOfTask().add(Calendar.YEAR, 1)

    val xml = saveProject(fixture.baseline)
    val loaded = mutableListOf<GanttPreviousState>()
    BaselineSerializer().loadBaselines(parseXmlProject(xml), loaded)

    assertEquals("2026-09-11",
      loaded.single().tasks.single { it.id == fixture.taskId }.start.toXMLString(),
      "the saved project has to hold the date the baseline took. The document was:\n$xml")
  }

  /** A baseline built the way production builds it: over the real TaskManager and createTasks. */
  private fun newBaseline(): Fixture {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val start = CalendarFactory.createGanttCalendar(2026, 8, 11)
    val task = taskManager.newTaskBuilder().withName("t").withStartDate(start.time).build()
    return Fixture(GanttPreviousState("a baseline", GanttPreviousState.createTasks(taskManager)), task.taskID)
  }

  /** Runs the real HistorySaver over the given baselines and returns the document it writes. */
  private fun saveProject(vararg baselines: GanttPreviousState): String {
    val out = ByteArrayOutputStream()
    val handler = createHandler(StreamResult(out))
    handler.startDocument()
    startElement("project", handler)
    HistorySaver().save(baselines.toList(), handler)
    endElement("project", handler)
    handler.endDocument()
    return out.toString(Charsets.UTF_8)
  }

  private class Fixture(val baseline: GanttPreviousState, val taskId: Int) {
    fun startOfTask() = baseline.tasks.single { it.id == taskId }.start
  }
}
