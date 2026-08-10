/*
 * TypeScript interfaces reflecting the JSON model produced by MxGraphPainter backed by
 * JsonPainterImpl (biz.ganttproject.mxgraph) on the server side.
 *
 * The JSON model is a single flat list of chart primitives, kept in the order in which the
 * canvas emits them to the painter, so that the client reproduces the overlapping of the
 * shapes by painting them in the same order. The kind of a primitive is told by its `type`
 * property. Every primitive carries its geometry at the top level, a nested `style` object
 * with the visual properties, and a free-form `attributes` map that binds the primitive to a
 * model object.
 *
 * The `style` object holds mxGraph style keys (see com.mxgraph.util.mxConstants), because the
 * painting logic resolves the styles identically for both the mxGraph and the JSON backends.
 * The commonly used keys are listed below as optional properties, but the map may contain any
 * mxGraph style key, hence the index signature.
 *
 * Only rectangles, lines, rhombuses and texts are currently emitted. Text groups are not
 * implemented yet.
 */

/** Free-form attributes attached to a primitive (e.g. task/resource identifiers). */
export type Attributes = Record<string, string>;

/** A style value as emitted into the JSON model. */
export type StyleValue = string | number;

/** Root object serialized by JsonPainterImpl.toJson(). */
export interface ChartModel {
  /** The primitives in the order they were emitted by the canvas: paint them in this order. */
  primitives: ChartPrimitive[];
}

/** A chart primitive, discriminated by its `type` property. */
export type ChartPrimitive =
  | RectanglePrimitive
  | LinePrimitive
  | RhombusPrimitive
  | TextPrimitive;

/** Visual style shared by filled shapes (rectangles and rhombuses). Keyed by mxGraph style names. */
export interface ShapeStyle {
  /** Fill color as a `#rrggbb` string, or the mxGraph `none` sentinel. */
  fillColor?: string;
  /** Stroke/border color as a `#rrggbb` string, or the mxGraph `none` sentinel. */
  strokeColor?: string;
  /** Opacity as a percentage in the [0, 100] range. */
  opacity?: number;
  /** Border width in pixels, as prescribed by the chart style. */
  strokeWidth?: number;
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
  /**
   * The dashes of a dashed line: the lengths of the dashes and the gaps in pixels, separated by
   * spaces, repeated along the line. Absent if the chart style prescribes no particular dashes.
   */
  dashPattern?: string;
  /** Line width in pixels, as prescribed by the chart style. */
  strokeWidth?: number;
  [key: string]: StyleValue | undefined;
}

/**
 * Visual style for texts. Keyed by mxGraph style names.
 *
 * The alignment values tell how the label is placed relative to the (x, y) anchor point of the
 * primitive, and the spacings are the paddings of the chart style which the alignment formulas
 * take into account (see TextPainter in biz.ganttproject.core, the desktop reference renderer).
 */
export interface TextStyle {
  /** Horizontal alignment: the mxGraph `left`, `center` or `right` value. */
  align?: string;
  /** Vertical alignment: the mxGraph `top`, `middle` or `bottom` value. */
  verticalAlign?: string;
  /** Font color as a `#rrggbb` string. Falls back to `#000000`. */
  fontColor?: string;
  /** Font size in pixels. */
  fontSize?: number;
  /** Font family name. */
  fontFamily?: string;
  /** Text opacity as a percentage in the [0, 100] range. */
  textOpacity?: number;
  /** Padding above the label, in pixels. */
  spacingTop?: number;
  /** Padding below the label, in pixels. */
  spacingBottom?: number;
  /** Padding on the left of the label, in pixels. */
  spacingLeft?: number;
  /** Padding on the right of the label, in pixels. */
  spacingRight?: number;
  [key: string]: StyleValue | undefined;
}

export interface RectanglePrimitive {
  type: 'rectangle';
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
  type: 'rhombus';
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
  type: 'line';
  startX: number;
  startY: number;
  finishX: number;
  finishY: number;
  style: LineStyle;
  attributes: Attributes;
}

export interface TextPrimitive {
  type: 'text';
  /** X coordinate of the anchor point, interpreted according to `style.align`. */
  x: number;
  /** Y coordinate of the anchor point, interpreted according to `style.verticalAlign`. */
  y: number;
  /** The label which fits into the space available for this text. */
  text: string;
  style: TextStyle;
  attributes: Attributes;
}
