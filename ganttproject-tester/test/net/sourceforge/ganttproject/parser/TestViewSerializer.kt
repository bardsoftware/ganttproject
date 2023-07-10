/*
Copyright 2023 BarD Software s.r.o, Dmitry Barashev

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
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.lib.fx.BuiltinColumns
import biz.ganttproject.lib.fx.ColumnListImpl
import biz.ganttproject.lib.fx.copyOf
import net.sourceforge.ganttproject.gui.zoom.ZoomManager
import net.sourceforge.ganttproject.task.CustomColumnsManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestViewSerializer {
    private val builtinColumns = BuiltinColumns(
        isZeroWidth = {
            when (TaskDefaultColumn.find(it)) {
                TaskDefaultColumn.COLOR, TaskDefaultColumn.INFO -> true
                else -> false
            }
        },
        allColumns = {
            ColumnList.Immutable.fromList(TaskDefaultColumn.getColumnStubs()).copyOf()
        }
    )
    @Test
    fun `default columns remain hidden - issue 2306`() {
        val storage = mutableListOf<ColumnList.Column>()
        val columnList = ColumnListImpl(columnList = storage, customPropertyManager = CustomColumnsManager(), tableColumns = { emptyList() }, builtinColumns = builtinColumns)
        val xmlView = XmlView(fields = listOf(
            XmlView.XmlField(id = TaskDefaultColumn.NAME.stub.id, name = "Name", width = 50, order = 0),
            XmlView.XmlField(id = TaskDefaultColumn.BEGIN_DATE.stub.id, name = "Begin date", width = 70, order = 1)
        ))
        loadView(xmlView, ZoomManager(GPTimeUnitStack()), columnList)
        columnList.columns().first { it.id == TaskDefaultColumn.BEGIN_DATE.stub.id }.also {
            assertTrue(it.isVisible)
            assertEquals(70, it.width)
            assertEquals(1, it.order)
        }
        columnList.columns().first { it.id == TaskDefaultColumn.NAME.stub.id }.also {
            assertTrue(it.isVisible)
            assertEquals(50, it.width)
            assertEquals(0, it.order)
        }
        columnList.columns().first { it.id == TaskDefaultColumn.END_DATE.stub.id }.also {
            assertFalse(it.isVisible)
        }
    }
}