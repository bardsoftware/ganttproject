/*
Copyright 2020 Dmitry Kazakov, BarD Software s.r.o

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

import com.mxgraph.io.mxCodec
import com.mxgraph.model.mxCell
import com.mxgraph.model.mxGeometry
import com.mxgraph.util.mxConstants
import com.mxgraph.util.mxPoint
import com.mxgraph.util.mxXmlUtils
import com.mxgraph.view.mxGraph
import com.mxgraph.view.mxPerimeter
import org.w3c.dom.Element

/**
 * This is low-level mxGraph interaction code which actually calls mxGraph API
 */
internal class MxPainterImpl {
  private val graph = mxGraph()
  private val parent = graph.defaultParent
  private val codec = mxCodec()

  fun toXml(): String {
    return mxXmlUtils.getXml(codec.encode(graph.model))
  }

  fun paintRectangle(leftX: Int, topY: Int, width: Int, height: Int, style: Map<String, Any?>, attributes: Map<String, String>) {
    val shapeStyle = mapOf(
        mxConstants.STYLE_SHAPE to mxConstants.SHAPE_RECTANGLE,
        mxConstants.STYLE_PERIMETER to mxPerimeter.RectanglePerimeter
    )
    graph.insertVertex(
        parent, null, attributes.toUserObject(),
        leftX.toDouble(), topY.toDouble(),
        width.toDouble(), height.toDouble(),
        (style + shapeStyle).toStyleString()
    )
  }

  fun paintLine(startX: Int, startY: Int, finishX: Int, finishY: Int, style: Map<String, Any?>, attributes: Map<String, String>) {
    val edge = graph.createEdge(parent, null, attributes.toUserObject(), null, null, style.toStyleString()) as mxCell
    edge.geometry = mxGeometry()
    edge.geometry.sourcePoint = mxPoint(startX.toDouble(), startY.toDouble())
    edge.geometry.targetPoint = mxPoint(finishX.toDouble(), finishY.toDouble())
    graph.addEdge(edge, null, null, null, null)
  }

  fun paintText(leftX: Int, bottomY: Int, attributes: Map<String, String>, style: Map<String, Any?>) {
    val textStyle = mapOf(
        mxConstants.STYLE_SPACING to 0,
        mxConstants.STYLE_FILLCOLOR to mxConstants.NONE,
        mxConstants.STYLE_STROKECOLOR to mxConstants.NONE,
        mxConstants.STYLE_LABEL_POSITION to mxConstants.ALIGN_RIGHT,
        mxConstants.STYLE_VERTICAL_LABEL_POSITION to mxConstants.ALIGN_TOP
    )
    graph.insertVertex(
        parent, null, attributes.toUserObject(),
        leftX.toDouble(), bottomY.toDouble(), 0.0, 0.0,
        "text;" + (style + textStyle).toStyleString()
    )
  }

  fun paintRhombus(leftX: Int, topY: Int, width: Int, height: Int, style: Map<String, Any?>, attributes: Map<String, String>) {
    val shapeStyle = mapOf(
        mxConstants.STYLE_SHAPE to mxConstants.SHAPE_RHOMBUS,
        mxConstants.STYLE_PERIMETER to mxPerimeter.RhombusPerimeter
    )
    graph.insertVertex(
        parent, null, attributes.toUserObject(),
        leftX.toDouble(), topY.toDouble(),
        width.toDouble(), height.toDouble(),
        (style + shapeStyle).toStyleString()
    )
  }

  fun clear() {
    graph.removeCells(graph.getChildCells(graph.defaultParent))
  }

  fun beginUpdate() {
    graph.model.beginUpdate()
  }

  fun endUpdate() {
    graph.model.endUpdate()
  }

  private fun Map<String, String>.toUserObject(): Element {
    val element = codec.document.createElement("Object")
    entries.forEach {
      element.setAttribute(it.key, it.value)
    }
    return element
  }
}

private fun <K, V> Map<K, V>.toStyleString() = entries.joinToString(";") { "${it.key}=${it.value}" }
