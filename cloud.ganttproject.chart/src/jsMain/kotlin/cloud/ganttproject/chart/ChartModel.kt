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
@file:JsExport
@file:OptIn(kotlin.js.ExperimentalJsExport::class)
package cloud.ganttproject.chart

/**
 * External declarations mirroring `chart_model.ts`, the JSON model produced by
 * JsonPainterImpl on the server side (module biz.ganttproject.mxgraph).
 *
 * The JSON model groups chart primitives by kind into separate collections. Every primitive
 * carries its geometry at the top level, a nested `style` object with the visual properties,
 * and a free-form `attributes` map that binds the primitive to a model object.
 *
 * The style objects hold mxGraph style keys (see com.mxgraph.util.mxConstants). In TypeScript
 * they carry an index signature `[key: string]: StyleValue | undefined` which cannot be
 * expressed in Kotlin; only the keys used by the renderer are declared explicitly.
 */

/** Free-form attributes attached to a primitive (e.g. task/resource identifiers). */
external interface Attributes

/** Root object serialized by JsonPainterImpl.toJson(). */
external interface ChartModel {
  val rectangles: Array<RectanglePrimitive>
  val lines: Array<LinePrimitive>
  val rhombuses: Array<RhombusPrimitive>
}

/** Visual style shared by filled shapes (rectangles and rhombuses). Keyed by mxGraph style names. */
external interface ShapeStyle {
  /** Fill color as a `#rrggbb` string, or the mxGraph `none` sentinel. */
  val fillColor: String?
  /** Stroke/border color as a `#rrggbb` string, or the mxGraph `none` sentinel. */
  val strokeColor: String?
  /** Opacity as a percentage in the [0, 100] range. */
  val opacity: Double?
  /** Border width in pixels. */
  val strokeWidth: Double?
}

/** Visual style for lines. Keyed by mxGraph style names. */
external interface LineStyle {
  /** Arrow head at the end of the line: the mxGraph `classic` shape or the `none` sentinel. */
  val endArrow: String?
  /** Stroke color as a `#rrggbb` string. Falls back to `#000000`. */
  val strokeColor: String?
  /** Opacity as a percentage in the [0, 100] range. */
  val opacity: Double?
  /** Whether the line is dashed: 1 for dashed, 0 otherwise. */
  val dashed: Double?
  /** Line width in pixels. */
  val strokeWidth: Double?
}

external interface RectanglePrimitive {
  /** X coordinate of the top-left corner. */
  val x: Double
  /** Y coordinate of the top-left corner. */
  val y: Double
  val width: Double
  val height: Double
  val style: ShapeStyle
  val attributes: Attributes
}

external interface RhombusPrimitive {
  /** X coordinate of the bounding box top-left corner. */
  val x: Double
  /** Y coordinate of the bounding box top-left corner. */
  val y: Double
  val width: Double
  val height: Double
  val style: ShapeStyle
  val attributes: Attributes
}

external interface LinePrimitive {
  val startX: Double
  val startY: Double
  val finishX: Double
  val finishY: Double
  val style: LineStyle
  val attributes: Attributes
}
