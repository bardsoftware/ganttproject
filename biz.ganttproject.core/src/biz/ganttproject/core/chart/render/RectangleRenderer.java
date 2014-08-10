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

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Paint;
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
    Paint paint = style.getBackgroundPaint(rect);
    if (paint != null) {
      g.setPaint(paint);
    }
    Float opacity = style.getOpacity(rect);
    if (opacity != null) {
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity.floatValue()));
    }
    Style.Padding padding = style.getPadding();
    if (style.getBackgroundImage() != null) {
      g.drawImage(style.getBackgroundImage(), rect.getLeftX() + padding.getLeft(), rect.getTopY() + padding.getTop(), null);
    } else {
      g.fillRect(rect.getLeftX() + padding.getLeft(), rect.getTopY() + padding.getTop(), 
          rect.getWidth() - (padding.getLeft() + padding.getRight()), rect.getHeight() - (padding.getTop() + padding.getBottom()));
    }
    Style.Borders border = style.getBorder(rect);
    if (border != null) {
      renderBorders(g, border, rect.getLeftX(), rect.getTopY(), rect.getWidth(), rect.getHeight());
    }
    return true;
  }
  
  static void renderBorders(Graphics2D g, Style.Borders border, int leftX, int topY, int width, int height) {
    if (border.isHomogeneous()) {
      g.setColor(border.getTop().getColor());
      g.setStroke(border.getTop().getStroke());
      g.drawRect(leftX, topY, width, height);
    } else {
      renderBorderEdge(g, border.getTop(), leftX, topY, leftX + width, topY);
      renderBorderEdge(g, border.getLeft(), leftX, topY, leftX, topY + height);
      renderBorderEdge(g, border.getBottom(), leftX, topY + height, leftX + width, topY + height);
      renderBorderEdge(g, border.getRight(), leftX + width, topY, leftX + width, topY + height);
    }
    
  }
  private static void renderBorderEdge(Graphics2D g, Style.Border border, int x1,int y1, int x2, int y2) {
    if (border != null) {
      g.setColor(border.getColor());
      g.setStroke(border.getStroke());
      g.drawLine(x1, y1, x2, y2);
    }
  }
}
