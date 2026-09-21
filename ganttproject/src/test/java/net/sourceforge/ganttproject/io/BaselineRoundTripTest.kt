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
import net.sourceforge.ganttproject.GanttPreviousStateTask
import net.sourceforge.ganttproject.parser.BaselineSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.DateFormat
import java.util.Locale
import javax.xml.transform.stream.StreamResult

/**
 * A baseline on its way through a project file: the real HistorySaver writes it and the real
 * BaselineSerializer reads it back. Baselines are kept in memory; the project file is the only
 * serialization they ever go through, and nothing touches the temporary directory.
 */
class BaselineRoundTripTest : SaverBase() {

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
  fun `a baseline survives a save and load round trip`() {
    val start = CalendarFactory.createGanttCalendar(2026, 8, 11)
    val baseline = GanttPreviousState(
      "a baseline",
      listOf(
        GanttPreviousStateTask(1, start, 3, false, false),
        GanttPreviousStateTask(2, start, 0, true, false),
        GanttPreviousStateTask(3, start, 5, false, true)
      )
    )
    val xml = saveProject(baseline)

    val loaded = mutableListOf<GanttPreviousState>()
    BaselineSerializer().loadBaselines(parseXmlProject(xml), loaded)

    assertEquals(1, loaded.size, "exactly one baseline has to come back. The document was:\n$xml")
    assertEquals("a baseline", loaded.single().name)
    assertEquals(
      listOf(
        TaskRow(1, "2026-09-11", 3, false, false),
        TaskRow(2, "2026-09-11", 0, true, false),
        TaskRow(3, "2026-09-11", 5, false, true)
      ),
      loaded.single().tasks.map { TaskRow(it.id, it.start.toXMLString(), it.duration, it.isMilestone, it.hasNested()) },
      "every task of the baseline has to come back unchanged. The document was:\n$xml"
    )
  }

  @Test
  fun `loading a baseline touches no temporary file`() {
    val tmpDir = File(System.getProperty("java.io.tmpdir"))
    fun baselineFiles(): Set<String> =
      (tmpDir.list { _, name -> name.startsWith("_GanttProject_ps_") } ?: emptyArray()).toSet()

    val before = baselineFiles()
    val start = CalendarFactory.createGanttCalendar(2026, 8, 11)
    val loaded = mutableListOf<GanttPreviousState>()
    BaselineSerializer().loadBaselines(
      parseXmlProject(saveProject(GanttPreviousState("a baseline", listOf(GanttPreviousStateTask(1, start, 3, false, false))))),
      loaded
    )
    loaded.single().tasks

    assertEquals(before, baselineFiles(),
      "baselines live in memory: neither loading one nor reading its tasks may create a temporary file")
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

  private data class TaskRow(
    val id: Int, val start: String, val duration: Int, val isMilestone: Boolean, val hasNested: Boolean
  )
}
