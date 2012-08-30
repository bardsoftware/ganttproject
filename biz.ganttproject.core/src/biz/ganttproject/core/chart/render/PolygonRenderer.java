/*
Copyright 2012 GanttProject Team

This file is part of GanttProject, an opensource project management tool.

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

import java.awt.Graphics2D;
import java.util.Properties;

import biz.ganttproject.core.chart.canvas.Canvas;

public class PolygonRenderer {
  private final Properties myProperties;
  private Graphics2D myGraphics;

  public PolygonRenderer(Properties props) {
    myProperties = props;
  }
  
  public void setGraphics(Graphics2D graphics) {
    myGraphics = graphics;
  }

  public void render(Canvas.Polygon p) {
    Graphics2D g = (Graphics2D) myGraphics.create();
    Style style = Style.getStyle(myProperties, p.getStyle());

    if (style.getVisibility(p) == Style.Visibility.HIDDEN) {
      return;
    }
    Style.Color background = style.getBackgroundColor(p);
    if (background != null) {
      g.setColor(background.get());
    }
    g.fillPolygon(p.getPointsX(), p.getPointsY(), p.getPointCount());
  }
}
