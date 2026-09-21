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
package net.sourceforge.ganttproject

import biz.ganttproject.core.io.XmlProjectImporter
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.io.HistorySaver
import net.sourceforge.ganttproject.io.SaverBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Locale
import javax.xml.transform.stream.StreamResult

/**
 * What a baseline promises: the dates it took stay its own.
 *
 * GanttCalendar inherits public mutators from GregorianCalendar, and TaskImpl.setStart
 * mutates the calendar it is given in place. A baseline that kept a reference to the live
 * start date would silently follow the schedule it was supposed to freeze.
 */
class BaselineSnapshotTest : SaverBase() {

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
  fun `a baseline does not follow the live start date of its task`() {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val start = CalendarFactory.createGanttCalendar(2026, 8, 11)
    val task = taskManager.newTaskBuilder().withName("t").withStartDate(start.time).build()

    val snapshot = GanttPreviousState.createTasks(taskManager).single { it.id == task.taskID }
    assertEquals("2026-09-11", snapshot.start.toXMLString(), "the baseline takes the task's start")

    // TaskImpl.setStart rewrites the given calendar in place; do the same to the task's
    // live start date here.
    task.start.setTime(CalendarFactory.createGanttCalendar(2027, 0, 1).time)

    assertEquals("2026-09-11", snapshot.start.toXMLString(),
      "the baseline has to keep the date it took, not follow the live task to 2027-01-01")
  }

  @Test
  fun `a baseline without tasks imports as an empty baseline and saves back`() {
    val project = XmlProjectImporter().import("""
      <project name="p">
        <previous>
          <previous-tasks name="empty baseline"/>
        </previous>
      </project>
    """.trimIndent())

    val baseline = project.baselines.single()
    assertEquals(emptyList<GanttPreviousStateTask>(), baseline.tasks,
      "a baseline with no <previous-task> elements has to come in as an empty list, not null")

    // Saving the project again must not throw on the empty baseline.
    val out = ByteArrayOutputStream()
    val handler = createHandler(StreamResult(out))
    handler.startDocument()
    HistorySaver().saveBaseline(baseline, handler)
    handler.endDocument()

    val xml = out.toString(Charsets.UTF_8)
    assertEquals(true, xml.contains("<previous-tasks name=\"empty baseline\"/>"),
      "the empty baseline has to be written back with no tasks, the document was:\n$xml")
  }
}
