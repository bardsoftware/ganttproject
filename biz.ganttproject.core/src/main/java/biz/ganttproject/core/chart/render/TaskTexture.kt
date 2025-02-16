package biz.ganttproject.core.chart.render

enum class TaskTexture(val paint: ShapePaint) {
  TRANSPARENT(ShapeConstants.TRANSPARENT),
  DEFAULT(ShapeConstants.DEFAULT),
  CROSS(ShapeConstants.CROSS),
  VERT(ShapeConstants.VERT),
  HORZ(ShapeConstants.HORZ),
  GRID(ShapeConstants.GRID),
  ROUND(ShapeConstants.ROUND),
  NW_TRIANGLE(ShapeConstants.NW_TRIANGLE),
  NE_TRIANGLE(ShapeConstants.NE_TRIANGLE),
  SW_TRIANGLE(ShapeConstants.SW_TRIANGLE),
  SE_TRIANGLE(ShapeConstants.SE_TRIANGLE),
  DIAMOND(ShapeConstants.DIAMOND),
  DOT(ShapeConstants.DOT),
  DOTS(ShapeConstants.DOT),
  SLASH(ShapeConstants.SLASH),
  BACKSLASH(ShapeConstants.BACKSLASH),
  THICK_VERT(ShapeConstants.THICK_VERT),
  THICK_HORZ(ShapeConstants.THICK_HORZ),
  THICK_GRID(ShapeConstants.THICK_GRID),
  THICK_SLASH(ShapeConstants.THICK_SLASH),
  THICK_BACKSLASH(ShapeConstants.THICK_BACKSLASH);

  companion object {
    fun find(paint: ShapePaint?): TaskTexture? {
      return entries.find { it.paint == paint }
    }
  }
}