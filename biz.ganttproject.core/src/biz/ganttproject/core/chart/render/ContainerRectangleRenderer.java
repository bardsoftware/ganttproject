package biz.ganttproject.core.chart.render;

import biz.ganttproject.core.chart.canvas.Canvas;

import java.awt.*;
import java.util.Properties;

public class ContainerRectangleRenderer {
  private final Properties myProperties;
  private Graphics2D myGraphics;

  public ContainerRectangleRenderer(Properties props) {
    myProperties = props;
  }
  
  public void setGraphics(Graphics2D graphics) {
    myGraphics = graphics;
  }
  
  public void render(Canvas.Rectangle rect) {
    Graphics2D g = (Graphics2D) myGraphics.create();
    Style style = Style.getStyle(myProperties, rect.getStyle());

    // rect.getBackgroundColor() is color of the triangle parts, save it for later use
    Color trianglesColor = rect.getBackgroundColor();
    // set it to null to get style color of the rectangle part
    rect.setBackgroundColor(null);
    Style.Color background = style.getBackgroundColor(rect);
    if (background != null) {
      g.setColor(background.get());
    }

    Style.Padding padding = style.getPadding();
    g.fillRect(rect.getLeftX() + padding.getLeft(), rect.getTopY() + padding.getTop(),
            rect.getWidth() - (padding.getLeft() + padding.getRight()), rect.getHeight() - (padding.getTop() + padding.getBottom()));

    if (trianglesColor == null) {
      trianglesColor = Color.BLACK;
    }
    myGraphics.setColor(trianglesColor);
    if (rect.hasStyle("task.container.open")) {
      myGraphics.fillPolygon(
              new int[] { rect.getLeftX(), rect.getLeftX() + rect.getHeight(), rect.getLeftX() },
              new int[] { rect.getTopY(), rect.getTopY(), rect.getBottomY() },
              3
      );
    }
    if (rect.hasStyle("task.container.close")) {
      myGraphics.fillPolygon(
              new int[] { rect.getRightX(), rect.getRightX() - rect.getHeight(), rect.getRightX() },
              new int[] { rect.getTopY(), rect.getTopY(), rect.getBottomY() },
              3
      );
    }

  }
}
