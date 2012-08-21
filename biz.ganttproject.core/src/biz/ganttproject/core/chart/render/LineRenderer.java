/*
GanttProject is an opensource project management tool.
Copyright (C) 2012 Dmitry Barashev, GanttProject Team

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
package biz.ganttproject.core.chart.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Properties;

import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Line;

/**
 * Renders line shapes.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class LineRenderer {
  private final static BasicStroke DASHED_STROKE = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
      1f, new float[] { 2.5f }, 0f);
  private final static BasicStroke DEFAULT_STROKE = new BasicStroke();
  
  private final Properties myProperties;
  private Graphics2D myGraphics;

  public LineRenderer(Properties props) {
    myProperties = props;
  }

  public void setGraphics(Graphics2D graphics) {
    myGraphics = graphics;
  }
  
  public void renderLine(Canvas.Line line) {
    Color foreColor = line.getForegroundColor();
    if (foreColor == null) {
      foreColor = Color.BLACK;
    }
    myGraphics.setColor(foreColor);
    if ("dependency.line.rubber".equals(line.getStyle())) {
      myGraphics.setStroke(DASHED_STROKE);
    }
    myGraphics.drawLine(line.getStartX(), line.getStartY(), line.getFinishX(), line.getFinishY());
    if (line.getArrow() == Line.Arrow.FINISH) {
      int xsign = Integer.signum(line.getFinishX() - line.getStartX());
      int ysign = Integer.signum(line.getFinishY() - line.getStartY());
      int[] xpoints = new int[] {line.getFinishX(), line.getFinishX() - xsign * 7 - Math.abs(ysign) * 3, line.getFinishX() - xsign * 7 + Math.abs(ysign) * 3};
      int[] ypoints = new int[] {line.getFinishY(), line.getFinishY() - ysign * 7 - Math.abs(xsign) * 3, line.getFinishY() - ysign * 7 + Math.abs(xsign) * 3};
      myGraphics.fillPolygon(xpoints, ypoints, 3);
    }
    if ("dependency.line.rubber".equals(line.getStyle())) {
      // Revert to default stroke
      myGraphics.setStroke(DEFAULT_STROKE);
    }
  }
}
