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

/**
 * Renders rectangles on Graphics2D.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class RectangleRenderer {
  private final Properties myProperties;
  private Graphics2D myGraphics;

  public RectangleRenderer(Properties props) {
    myProperties = props;
  }
  
  public void setGraphics(Graphics2D graphics) {
    myGraphics = graphics;
  }
  
  public boolean render(Canvas.Rectangle rect) {
    Graphics2D g = (Graphics2D) myGraphics.create();
    Style style = Style.getStyle(myProperties, rect.getStyle());

    if (style.getVisibility(rect) == Style.Visibility.HIDDEN) {
      return false;
    }
    Style.Color background = style.getBackgroundColor(rect);
    if (background != null) {
      g.setColor(background.get());
    }
    
    g.fillRect(rect.myLeftX, rect.myTopY, rect.myWidth, rect.myHeight);

    Style.Border border = style.getBorder(rect);
    if (border != null) {
      g.setColor(border.getColor());
      g.setStroke(border.getStroke());
      g.drawRect(rect.myLeftX, rect.myTopY, rect.getWidth(), rect.myHeight);
    }
    return true;
  }
}
