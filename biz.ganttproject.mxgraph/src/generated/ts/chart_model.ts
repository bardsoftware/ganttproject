/*
 * TypeScript interfaces reflecting the JSON model produced by MxGraphPainter backed by
 * JsonPainterImpl (biz.ganttproject.mxgraph) on the server side.
 *
 * The JSON model groups chart primitives by kind into separate collections. Every primitive
 * carries its geometry at the top level, a nested `style` object with the visual properties,
 * and a free-form `attributes` map that binds the primitive to a model object.
 *
 * The `style` object holds mxGraph style keys (see com.mxgraph.util.mxConstants), because the
 * painting logic resolves the styles identically for both the mxGraph and the JSON backends.
 * The commonly used keys are listed below as optional properties, but the map may contain any
 * mxGraph style key, hence the index signature.
 *
 * Only rectangles, lines and rhombuses are currently emitted. Text and text groups
 * are not implemented yet.
 */

/** Free-form attributes attached to a primitive (e.g. task/resource identifiers). */
export type Attributes = Record<string, string>;

/** A style value as emitted into the JSON model. */
export type StyleValue = string | number;

/** Root object serialized by JsonPainterImpl.toJson(). */
export interface ChartModel {
  rectangles: RectanglePrimitive[];
  lines: LinePrimitive[];
  rhombuses: RhombusPrimitive[];
}

/** Visual style shared by filled shapes (rectangles and rhombuses). Keyed by mxGraph style names. */
export interface ShapeStyle {
  /** Fill color as a `#rrggbb` string, or the mxGraph `none` sentinel. */
  fillColor?: string;
  /** Stroke/border color as a `#rrggbb` string, or the mxGraph `none` sentinel. */
  strokeColor?: string;
  /** Opacity as a percentage in the [0, 100] range. */
  opacity?: number;
  [key: string]: StyleValue | undefined;
}

/** Visual style for lines. Keyed by mxGraph style names. */
export interface LineStyle {
  /** Arrow head at the end of the line: the mxGraph `classic` shape or the `none` sentinel. */
  endArrow?: string;
  /** Stroke color as a `#rrggbb` string. Falls back to `#000000`. */
  strokeColor?: string;
  /** Opacity as a percentage in the [0, 100] range. */
  opacity?: number;
  /** Whether the line is dashed: 1 for dashed, 0 otherwise. */
  dashed?: number;
  [key: string]: StyleValue | undefined;
}

export interface RectanglePrimitive {
  /** X coordinate of the top-left corner. */
  x: number;
  /** Y coordinate of the top-left corner. */
  y: number;
  width: number;
  height: number;
  style: ShapeStyle;
  attributes: Attributes;
}

export interface RhombusPrimitive {
  /** X coordinate of the bounding box top-left corner. */
  x: number;
  /** Y coordinate of the bounding box top-left corner. */
  y: number;
  width: number;
  height: number;
  style: ShapeStyle;
  attributes: Attributes;
}

export interface LinePrimitive {
  startX: number;
  startY: number;
  finishX: number;
  finishY: number;
  style: LineStyle;
  attributes: Attributes;
}
