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
import biz.ganttproject.core.option.DefaultStringOption
import biz.ganttproject.core.option.GPOption
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.ganttview.TaskFilterManager
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.chart.GanttChart
import net.sourceforge.ganttproject.gui.GPColorChooser
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.view.ViewProvider
import net.sourceforge.ganttproject.gui.zoom.ZoomManager
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.task.TaskView
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.xml.sax.helpers.AttributesImpl
import java.awt.Color
import java.io.ByteArrayOutputStream
import javax.xml.transform.stream.StreamResult

/**
 * The `<option>` elements of a `<view>` on their way through a project file: the real ViewSaver
 * writes them and the real reader reads them back.
 *
 * The reader keeps only the last uninterrupted run of `<option>` elements inside a `<view>`.
 * Everything standing before an element of a different kind is dropped without a word, so the order
 * in which the saver writes decides how much of a view survives a save and load round trip.
 */
class ViewOptionOrderTest : SaverBase() {

  @AfterEach
  fun tearDown() {
    GPColorChooser.setRecentColors(mutableListOf())
  }

  /**
   * Everything the saver puts into `<view id="gantt-chart">` has to come back when the file is read
   * again. With `color.recent` written after `<filters>` the recent colours form a run of their own
   * and every option written before them is lost.
   */
  @Test
  fun `in the order the saver writes today no option is lost`() {
    GPColorChooser.setRecentColors(mutableListOf(Color.RED))
    val xml = saveGanttView()

    val ganttView = parseXmlProject(xml).views.first { it.id == "gantt-chart" }
    val readBack = ganttView.options.orEmpty().map { it.id }

    assertEquals(
      listOf("taskLabelUp", "taskLabelDown", "divider", "filter.completedTasks", "color.recent"),
      readBack,
      "options written into the view did not come back. The document was:\n$xml"
    )
  }

  /**
   * The measurement behind the order above, kept so that the reason for it does not get lost: the
   * reader keeps only the last uninterrupted run of `<option>` elements. This one exercises the
   * reader alone and therefore holds whatever order the saver uses.
   */
  @Test
  fun `the reader drops every option that stands before another element`() {
    val out = ByteArrayOutputStream()
    val handler = createHandler(StreamResult(out))
    handler.startDocument()
    startElement("project", handler)
    val attrs = AttributesImpl()
    addAttribute("id", "gantt-chart", attrs)
    startElement("view", attrs, handler)
    OptionSaver().saveOptionList(handler, DefaultStringOption("divider", "0.5") as GPOption<*>)
    startElement("filters", AttributesImpl(), handler)
    endElement("filters", handler)
    OptionSaver().saveOptionList(handler, DefaultStringOption("color.recent", "#ff0066") as GPOption<*>)
    endElement("view", handler)
    endElement("project", handler)
    handler.endDocument()
    val xml = out.toString(Charsets.UTF_8)

    val ganttView = parseXmlProject(xml).views.first { it.id == "gantt-chart" }
    assertEquals(
      listOf("color.recent"), ganttView.options.orEmpty().map { it.id },
      "the reader now keeps options standing before another element; if that is really so, the " +
        "order in which ViewSaver writes them no longer matters. The document was:\n$xml"
    )
  }

  /** Runs the real ViewSaver and returns the document it writes. */
  private fun saveGanttView(): String {
    val out = ByteArrayOutputStream()
    val handler = createHandler(StreamResult(out))
    handler.startDocument()
    startElement("project", handler)
    ViewSaver().save(
      uiFacade(), ganttViewProvider(), ganttViewProvider(), emptyColumns(), taskFilterManager(),
      emptyColumns(), handler
    )
    endElement("project", handler)
    handler.endDocument()
    return out.toString(Charsets.UTF_8)
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
    val facade = createNiceMock<UIFacade>(UIFacade::class.java)
    expect(facade.zoomManager).andStubReturn(zoomManager)
    expect(facade.ganttChart).andStubReturn(ganttChart)
    expect(facade.currentTaskView).andStubReturn(TaskView())
    replay(zoomState, zoomManager, ganttChart, facade)
    return facade
  }

  private fun ganttViewProvider(): ViewProvider {
    val provider = createNiceMock<ViewProvider>(ViewProvider::class.java)
    expect(provider.options).andStubReturn(
      listOf(
        DefaultStringOption("divider", "0.5"),
        DefaultStringOption("filter.completedTasks", "false")
      )
    )
    replay(provider)
    return provider
  }

  private fun emptyColumns(): ColumnList {
    val columns = createNiceMock<ColumnList>(ColumnList::class.java)
    expect(columns.exportData()).andStubReturn(emptyList())
    replay(columns)
    return columns
  }

  private fun taskFilterManager() = TaskFilterManager(
    TestSetupHelper.newTaskManagerBuilder().build(), createNiceMock<ProjectDatabase>(ProjectDatabase::class.java)
  )
}
