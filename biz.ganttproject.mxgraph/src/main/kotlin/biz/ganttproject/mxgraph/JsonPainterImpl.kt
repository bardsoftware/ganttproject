/*
Copyright 2026 BarD Software s.r.o

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

import com.fasterxml.jackson.databind.ObjectMapper

/**
 * This is the low-level counterpart of [MxPainterImpl]. Instead of building an mxGraph model,
 * it accumulates the chart primitives as plain maps and serializes them to JSON. The resulting
 * JSON model is intended to be rendered on the client side.
 */
internal class JsonPainterImpl : PainterImpl {
  private val rectangles = mutableListOf<Map<String, Any?>>()
  private val lines = mutableListOf<Map<String, Any?>>()
  private val rhombuses = mutableListOf<Map<String, Any?>>()
  private val mapper = ObjectMapper()

  fun toJson(): String {
    return mapper.writeValueAsString(linkedMapOf(
        "rectangles" to rectangles,
        "lines" to lines,
        "rhombuses" to rhombuses
    ))
  }

  override fun paintRectangle(leftX: Int, topY: Int, width: Int, height: Int, style: Map<String, Any?>, attributes: Map<String, String>) {
    rectangles.add(linkedMapOf(
        "x" to leftX,
        "y" to topY,
        "width" to width,
        "height" to height,
        "style" to style,
        "attributes" to attributes
    ))
  }

  override fun paintLine(startX: Int, startY: Int, finishX: Int, finishY: Int, style: Map<String, Any?>, attributes: Map<String, String>) {
    lines.add(linkedMapOf(
        "startX" to startX,
        "startY" to startY,
        "finishX" to finishX,
        "finishY" to finishY,
        "style" to style,
        "attributes" to attributes
    ))
  }

  override fun paintText(leftX: Int, bottomY: Int, attributes: Map<String, String>, style: Map<String, Any?>) =
      TODO("Text rendering is not implemented in JsonPainterImpl")

  override fun paintRhombus(leftX: Int, topY: Int, width: Int, height: Int, style: Map<String, Any?>, attributes: Map<String, String>) {
    rhombuses.add(linkedMapOf(
        "x" to leftX,
        "y" to topY,
        "width" to width,
        "height" to height,
        "style" to style,
        "attributes" to attributes
    ))
  }

  override fun clear() {
    rectangles.clear()
    lines.clear()
    rhombuses.clear()
  }

  override fun beginUpdate() {}

  override fun endUpdate() {}
}
