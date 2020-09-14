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
package biz.ganttproject.core.chart.render;

import biz.ganttproject.core.chart.canvas.Canvas;

import java.awt.*;
import java.util.Properties;

public class SummaryTaskRenderer {
  private final Properties myProperties;
  private Graphics2D myGraphics;

  public SummaryTaskRenderer(Properties props) {
    myProperties = props;
  }
  
  public void setGraphics(Graphics2D graphics) {
    myGraphics = graphics;
  }
  
  public void render(Canvas.Rectangle rect) {
    Graphics2D g = (Graphics2D) myGraphics.create();
    Style style = Style.getStyle(myProperties, rect.getStyle());

    Style.Color background = style.getBackgroundColor(rect);
    if (background != null) {
      g.setColor(background.get());
    }
    Style.Padding padding = style.getPadding();
    g.fillRect(rect.getLeftX() + padding.getLeft(), rect.getTopY() + padding.getTop(),
            rect.getWidth() - (padding.getLeft() + padding.getRight()), rect.getHeight() - (padding.getTop() + padding.getBottom()));

    if (rect.hasStyle("task.summary.open")) {
      g.fillPolygon(
          new int[] { rect.getLeftX(), rect.getLeftX() + rect.getHeight(), rect.getLeftX() },
          new int[] { rect.getTopY(), rect.getTopY(), rect.getBottomY() },
          3
      );
    }
    if (rect.hasStyle("task.summary.close")) {
      g.fillPolygon(
          new int[] { rect.getRightX(), rect.getRightX() - rect.getHeight(), rect.getRightX() },
          new int[] { rect.getTopY(), rect.getTopY(), rect.getBottomY() },
          3
      );
    }
  }
}
