/*
Copyright 2021 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.mxgraph

interface Driver {
  fun paintRectangle(leftX: Int, topY: Int, width: Int, height: Int, style: Map<String, Any?>,
                     attributes: Map<String, String>)

  fun paintLine(startX: Int, startY: Int, finishX: Int, finishY: Int, style: Map<String, Any?>,
                attributes: Map<String, String>)

  fun paintText(leftX: Int, bottomY: Int, attributes: Map<String, String>, style: Map<String, Any?>)

  fun paintRhombus(leftX: Int, topY: Int, width: Int, height: Int, style: Map<String, Any?>,
                   attributes: Map<String, String>)

  fun clear()
  fun beginUpdate()
  fun endUpdate()
  fun toXml(): String
}
