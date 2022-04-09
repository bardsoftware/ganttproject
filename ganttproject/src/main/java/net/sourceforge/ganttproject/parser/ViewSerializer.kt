/*
Copyright 2022 BarD Software s.r.o, GanttProject Cloud OU, Dmitry Barashev

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
package net.sourceforge.ganttproject.parser

import biz.ganttproject.core.io.XmlView
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnList
import net.sourceforge.ganttproject.gui.zoom.ZoomManager

fun loadView(xmlView: XmlView, zoomManager: ZoomManager, columnList: ColumnList) {
  // Load zooming state
  xmlView.zoomingState?.let {
    zoomManager.setZoomState(it)
  }
  // Load displayed columns
  xmlView.fields?.map { xmlField ->
    val id: String = xmlField.id
    val defaultColumn = TaskDefaultColumn.find(id)
    val name = if (defaultColumn == null) id else defaultColumn.getName()
    val order = xmlField.order
    val width = xmlField.width
    ColumnList.ColumnStub(id, name, true, order, width)
  }?.let { stubs ->
    // Set orders for the columns which had no orders defined
    val countPositiveOrders = stubs.count { it.order >= 0 }
    stubs.filter { it.order == -1 }.forEachIndexed { idx, stub -> stub.order = idx + countPositiveOrders }
    columnList.importData(ColumnList.Immutable.fromList(stubs + TaskDefaultColumn.getColumnStubs().filter { defaultColumn ->
      stubs.firstOrNull { it.id == defaultColumn.id } == null
    }), false)
  }
}
