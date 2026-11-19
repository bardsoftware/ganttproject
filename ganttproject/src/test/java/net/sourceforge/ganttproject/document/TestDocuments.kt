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
package net.sourceforge.ganttproject.document

import biz.ganttproject.core.option.DefaultStringOption
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.lib.fx.TreeCollapseView
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.chart.GanttChart
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.view.ViewProvider
import net.sourceforge.ganttproject.gui.zoom.ZoomManager
import net.sourceforge.ganttproject.parser.ParserFactory
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskView
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.replay

/** A project document which reads and writes [physical] through the real project file format. */
fun testProxyDocument(physical: Document, project: IGanttProject, uiFacade: UIFacade, parserFactory: ParserFactory) =
  ProxyDocument(null, physical, project, uiFacade, emptyColumns(), emptyColumns(), parserFactory)

/** The parts of the UI which reading and writing a project document ask for. */
fun testUiFacade(): UIFacade {
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
