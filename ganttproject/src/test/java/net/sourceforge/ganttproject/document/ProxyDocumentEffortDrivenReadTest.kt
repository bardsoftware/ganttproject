/*
Copyright 2026 BarD Software s.r.o., GanttProject Team

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
package net.sourceforge.ganttproject.document

import biz.ganttproject.core.option.DefaultStringOption
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.lib.fx.TreeCollapseView
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.PrjInfos
import net.sourceforge.ganttproject.chart.GanttChart
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.view.ViewProvider
import net.sourceforge.ganttproject.gui.zoom.ZoomManager
import net.sourceforge.ganttproject.io.GanttXMLOpen
import net.sourceforge.ganttproject.parser.ParserFactory
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.text.DateFormat
import java.util.Locale

/**
 * Reads a project file through the real [ProxyDocument.read] and the real parser. The derivation
 * of the durations of effort driven tasks happens after the parsing, in `read()` itself, so only
 * this path can tell whether that call is there.
 */
class ProxyDocumentEffortDrivenReadTest {
  private lateinit var project: GanttProjectImpl

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
    project = GanttProjectImpl()
  }

  @Test
  fun `reading a document replaces the stored duration of an effort driven task by the derived one`() {
    // The stored duration (1) does not match the formula: 20 hours at 4 hours a day are 5 days.
    read(projectXml(effortDriven = true))
    assertEquals(5, project.taskManager.getTask(1).duration.length)
  }

  @Test
  fun `reading a document keeps the stored duration of a task without the flag`() {
    read(projectXml(effortDriven = false))
    assertEquals(1, project.taskManager.getTask(1).duration.length)
  }

  private fun projectXml(effortDriven: Boolean) = """
    <project name="p">
      <tasks>
        <task id="1" uid="u1" name="T" start="2026-11-09" duration="1" effort-hours="20" effort-driven="$effortDriven"/>
      </tasks>
      <resources>
        <resource id="0" name="R" function="Default:0" contacts="" phone="" hours-per-day="4"/>
      </resources>
      <allocations>
        <allocation task-id="1" resource-id="0" function="Default:0" responsible="true" load="100.0"/>
      </allocations>
    </project>""".trimIndent()

  private fun read(xml: String) {
    val facade = uiFacade()
    val parserFactory = object : ParserFactory {
      override fun newParser() = GanttXMLOpen(PrjInfos(), project.taskManager, facade)
      override fun newSaver() = throw UnsupportedOperationException()
    }
    val physical = createNiceMock<Document>(Document::class.java)
    expect(physical.inputStream).andStubAnswer { ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)) }
    replay(physical)
    ProxyDocument(null, physical, project, facade, emptyColumns(), emptyColumns(), parserFactory).read()
  }

  private fun uiFacade(): UIFacade {
    val zoomState = createNiceMock<ZoomManager.ZoomState>(ZoomManager.ZoomState::class.java)
    expect(zoomState.persistentName).andStubReturn("default:8")
    val zoomManager = createNiceMock<ZoomManager>(ZoomManager::class.java)
    expect(zoomManager.zoomState).andStubReturn(zoomState)
    val ganttChart = createNiceMock<GanttChart>(GanttChart::class.java)
    expect(ganttChart.taskLabelOptions).andStubReturn(
      GPOptionGroup(
        "taskLabels",
        DefaultStringOption("taskLabelUp", "name"),
        DefaultStringOption("taskLabelDown", "duration")
      )
    )
    val provider = createNiceMock<ViewProvider>(ViewProvider::class.java)
    expect(provider.options).andStubReturn(listOf(DefaultStringOption("divider", "0.5")))
    val facade = createNiceMock<UIFacade>(UIFacade::class.java)
    expect(facade.zoomManager).andStubReturn(zoomManager)
    expect(facade.ganttChart).andStubReturn(ganttChart)
    expect(facade.currentTaskView).andStubReturn(TaskView())
    expect(facade.ganttViewProvider).andStubReturn(provider)
    @Suppress("UNCHECKED_CAST")
    val collapse = createNiceMock<TreeCollapseView<*>>(TreeCollapseView::class.java) as TreeCollapseView<Task>
    expect(facade.taskCollapseView).andStubReturn(collapse)
    replay(zoomState, zoomManager, ganttChart, provider, collapse, facade)
    return facade
  }

  private fun emptyColumns(): ColumnList {
    val columns = createNiceMock<ColumnList>(ColumnList::class.java)
    expect(columns.exportData()).andStubReturn(emptyList())
    replay(columns)
    return columns
  }
}
