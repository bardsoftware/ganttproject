/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.common.base.Supplier;

import net.sourceforge.ganttproject.util.PropertiesUtil;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Line;
import biz.ganttproject.core.chart.canvas.Canvas.Polygon;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;
import biz.ganttproject.core.chart.canvas.Canvas.Text;
import biz.ganttproject.core.chart.canvas.Canvas.TextGroup;
import biz.ganttproject.core.chart.canvas.Painter;
import biz.ganttproject.core.chart.render.LineRenderer;
import biz.ganttproject.core.chart.render.PolygonRenderer;
import biz.ganttproject.core.chart.render.RectangleRenderer;
import biz.ganttproject.core.chart.render.TextPainter;
import biz.ganttproject.core.option.ChangeValueEvent;
import biz.ganttproject.core.option.ChangeValueListener;

/**
 * Implements styled painters for the available primitives (see
 * {@link Canvas})
 *
 * @author bard
 */
public class StyledPainterImpl implements Painter {
  private Graphics2D myGraphics;

  private final Map<String, RectanglePainter> myStyle2painter = new HashMap<String, RectanglePainter>();

  private ChartUIConfiguration myConfig;

  private final int margin;

  /** List X coordinates used to draw polygons */
  private int[] myXPoints = new int[4];

  /** List Y coordinates used to draw polygons */
  private int[] myYPoints = new int[4];

  private final Properties myProperties;

  private final TextPainter myTextPainter;

  private final LineRenderer myLineRenderer;

  private final RectangleRenderer myRectangleRenderer;

  private final PolygonRenderer myPolygonRenderer;

  /** Default stroke used for the primitives */
  private final static BasicStroke defaultStroke = new BasicStroke();

  public StyledPainterImpl(final ChartUIConfiguration config) {
    myConfig = config;
    margin = myConfig.getMargin();

    myStyle2painter.put("task.progress", new ColouredRectanglePainter(Color.BLACK));
    myStyle2painter.put("task.progress.end", new ColouredRectanglePainter(Color.BLACK));
    myStyle2painter.put("load.normal", myResourceLoadPainter);
    myStyle2painter.put("load.normal.first", myResourceLoadPainter);
    myStyle2painter.put("load.normal.last", myResourceLoadPainter);
    myStyle2painter.put("load.normal.first.last", myResourceLoadPainter);
    myStyle2painter.put("load.overload", myResourceLoadPainter);
    myStyle2painter.put("dependency.arrow.down", myArrowDownPainter);
    myStyle2painter.put("load.overload.first", myResourceLoadPainter);
    myStyle2painter.put("load.overload.last", myResourceLoadPainter);
    myStyle2painter.put("load.overload.first.last", myResourceLoadPainter);
    myStyle2painter.put("dependency.arrow.up", myArrowUpPainter);
    myStyle2painter.put("dependency.arrow.left", myArrowLeftPainter);
    myStyle2painter.put("dependency.arrow.right", myArrowRightPainter);
    myStyle2painter.put("dayoff", myDayOffPainter);
    myStyle2painter.put("load.underload", myResourceLoadPainter);
    myStyle2painter.put("load.underload.first", myResourceLoadPainter);
    myStyle2painter.put("load.underload.last", myResourceLoadPainter);
    myStyle2painter.put("load.underload.first.last", myResourceLoadPainter);
    myStyle2painter.put("previousStateTask", myPreviousStateTaskRectanglePainter);

    myProperties = new Properties();
    PropertiesUtil.loadProperties(myProperties, "/chart.properties");
    config.getChartStylesOption().addChangeValueListener(new ChangeValueListener() {
      @Override
      public void changeValue(ChangeValueEvent event) {
        for (Entry<String, String> entry : config.getChartStylesOption().getValues()) {
          myProperties.put(entry.getKey(), entry.getValue());
        }
      }
    });
    myTextPainter = new TextPainter(myProperties, new Supplier<Font>() {
      public Font get() {
        return config.getChartFont();
      }
    });
    myLineRenderer = new LineRenderer(myProperties);
    myRectangleRenderer = new RectangleRenderer(myProperties);
    myPolygonRenderer = new PolygonRenderer(myProperties);
  }

  public void setGraphics(Graphics g) {
    myGraphics = (Graphics2D) g;
    myTextPainter.setGraphics(myGraphics);
    myLineRenderer.setGraphics(myGraphics);
    myRectangleRenderer.setGraphics(myGraphics);
    myPolygonRenderer.setGraphics(myGraphics);
  }

  @Override
  public void prePaint() {
    myGraphics.setStroke(defaultStroke);
    myGraphics.setFont(myConfig.getChartFont());
  }

  @Override
  public void paint(Canvas.Rectangle next) {
    assert myGraphics != null;
    if (myRectangleRenderer.render(next)) {
      return;
    }
    RectanglePainter painter = myStyle2painter.get(next.getStyle());
    if (painter != null) {
      // Use found painter
      painter.paint(next);
    } else {
      // Use default painter, since no painter was provided
      if (next.getBackgroundColor() == null) {
        Color foreColor = next.getForegroundColor();
        if (foreColor == null) {
          foreColor = Color.BLACK;
        }
        myGraphics.setColor(foreColor);
        myGraphics.drawRect(next.getLeftX(), next.getTopY(), next.getWidth(), next.getHeight());
      } else {
        myGraphics.setColor(next.getBackgroundColor());
        myGraphics.fillRect(next.getLeftX(), next.getTopY(), next.getWidth(), next.getHeight());
      }
    }
  }

  /**
   * Interface providing a method to paint a rectangle (currently, used to draw
   * many more other things...)
   */
  private interface RectanglePainter {
    public void paint(Canvas.Rectangle next);
  }


  private final RectanglePainter myArrowDownPainter = new RectanglePainter() {
    @Override
    public void paint(Rectangle next) {
      myXPoints[0] = next.getLeftX();
      myXPoints[1] = next.getRightX();
      myXPoints[2] = next.getMiddleX();
      myYPoints[0] = next.getTopY();
      myYPoints[1] = next.getTopY();
      myYPoints[2] = next.getBottomY();
      myGraphics.setColor(Color.BLACK);
      myGraphics.fillPolygon(myXPoints, myYPoints, 3);
    }
  };

  private final RectanglePainter myArrowUpPainter = new RectanglePainter() {
    @Override
    public void paint(Rectangle next) {
      myXPoints[0] = next.getLeftX();
      myXPoints[1] = next.getRightX();
      myXPoints[2] = next.getMiddleX();
      myYPoints[0] = next.getBottomY();
      myYPoints[1] = next.getBottomY();
      myYPoints[2] = next.getTopY();
      myGraphics.setColor(Color.BLACK);
      myGraphics.fillPolygon(myXPoints, myYPoints, 3);
    }
  };

  private final RectanglePainter myArrowLeftPainter = new RectanglePainter() {
    @Override
    public void paint(Rectangle next) {
      Graphics g = myGraphics;
      g.setColor(Color.BLACK);
      myXPoints[0] = next.getLeftX();
      myXPoints[1] = next.getRightX();
      myXPoints[2] = next.getRightX();
      myYPoints[0] = next.getMiddleY();
      myYPoints[1] = next.getTopY();
      myYPoints[2] = next.getBottomY();
      g.fillPolygon(myXPoints, myYPoints, 3);
    }
  };

  private final RectanglePainter myArrowRightPainter = new RectanglePainter() {
    @Override
    public void paint(Rectangle next) {
      myXPoints[0] = next.getLeftX();
      myXPoints[1] = next.getRightX();
      myXPoints[2] = next.getLeftX();
      myYPoints[0] = next.getTopY();
      myYPoints[1] = next.getMiddleY();
      myYPoints[2] = next.getBottomY();
      myGraphics.setColor(Color.BLACK);
      myGraphics.fillPolygon(myXPoints, myYPoints, 3);
    }
  };

  private final RectanglePainter myDayOffPainter = new RectanglePainter() {
    @Override
    public void paint(Rectangle next) {
      int margin = StyledPainterImpl.this.margin - 3;
      Color c = myConfig.getDayOffColor();
      myGraphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));
      myGraphics.fillRect(next.getLeftX(), next.getTopY() + margin, next.getWidth(), next.getHeight() - 2 * margin);
      myGraphics.setColor(Color.BLACK);
      myGraphics.drawLine(next.getLeftX(), next.getTopY() + margin, next.getLeftX(), next.getBottomY() - margin);
      myGraphics.drawLine(next.getLeftX(), next.getTopY() + margin, next.getRightX(), next.getTopY() + margin);
      myGraphics.drawLine(next.getLeftX(), next.getBottomY() - margin, next.getRightX(), next.getBottomY() - margin);
      myGraphics.drawLine(next.getRightX(), next.getTopY() + margin, next.getRightX(), next.getBottomY() - margin);
    }
  };

  private final RectanglePainter myResourceLoadPainter = new RectanglePainter() {
    @Override
    public void paint(Rectangle next) {
      String style = next.getStyle();
      Color c;
      if (style.indexOf("overload") > 0) {
        c = myConfig.getResourceOverloadColor();
      } else if (style.indexOf("underload") > 0) {
        c = myConfig.getResourceUnderLoadColor();
      } else {
        c = myConfig.getResourceNormalLoadColor();
      }
      myGraphics.setColor(c);

      myGraphics.fillRect(next.getLeftX(), next.getTopY() + margin, next.getWidth(), next.getHeight() - 2 * margin);
      if (style.indexOf(".first") > 0) {
        myGraphics.setColor(Color.BLACK);
        myGraphics.drawLine(next.getLeftX(), next.getTopY() + margin, next.getLeftX(), next.getBottomY() - margin);
      }
      if (style.indexOf(".last") > 0) {
        myGraphics.setColor(Color.BLACK);
        myGraphics.drawLine(next.getRightX(), next.getTopY() + margin, next.getRightX(), next.getBottomY() - margin);
      }
      myGraphics.setColor(Color.BLACK);

//      ResourceLoadRenderer.ResourceLoad load = (ResourceLoadRenderer.ResourceLoad) next.getModelObject();
//      int loadInt = Math.round(load.getLoad());
//      String loadStr = loadInt + "%";
//      int emsLength = myTextLengthCalculator.getTextLength(loadStr);
//      boolean displayLoad = (loadInt != 100 && emsLength <= next.getWidth());
//      if (displayLoad) {
//        myGraphics.drawString(loadStr, next.getMiddleX() - myTextLengthCalculator.getTextLength(loadStr) / 2,
//            next.getTopY() + margin + next.getHeight() / 2);
//        myGraphics.drawLine(next.getLeftX(), next.getTopY() + margin, next.getLeftX(), next.getBottomY() - margin);
//      }
      myGraphics.setColor(Color.BLACK);
      myGraphics.drawLine(next.getLeftX(), next.getTopY() + margin, next.getRightX(), next.getTopY() + margin);
      myGraphics.drawLine(next.getLeftX(), next.getBottomY() - margin, next.getRightX(), next.getBottomY() - margin);
    }
  };

  private final RectanglePainter myPreviousStateTaskRectanglePainter = new RectanglePainter() {
    private int[] myXPoints = new int[4];
    private int[] myYPoints = new int[4];

    @Override
    public void paint(Canvas.Rectangle next) {
      Graphics g = myGraphics;
      final Color c;
      if (next.hasStyle("earlier")) {
        c = myConfig.getEarlierPreviousTaskColor();
      } else if (next.hasStyle("later")) {
        c = myConfig.getLaterPreviousTaskColor();
      } else {
        c = myConfig.getPreviousTaskColor();
      }
      g.setColor(c);

      if (next.hasStyle("milestone")) {
        int middleX = (next.getWidth() <= next.getHeight()) ? next.getRightX() - next.getWidth() / 2 : next.getLeftX()
            + next.getHeight() / 2;
        int middleY = next.getMiddleY();

        myXPoints[0] = next.getLeftX() + 2;
        myYPoints[0] = middleY;
        myXPoints[1] = middleX + 3;
        myYPoints[1] = next.getTopY() - 1;
        myXPoints[2] = (next.getWidth() <= next.getHeight()) ? next.getRightX() + 4 : next.getLeftX() + next.getHeight() + 4;
        myYPoints[2] = middleY;
        myXPoints[3] = middleX + 3;
        myYPoints[3] = next.getBottomY() + 1;

        g.fillPolygon(myXPoints, myYPoints, 4);
      } else if (next.hasStyle("super")) {
        g.fillRect(next.getLeftX(), next.getTopY() + next.getHeight() - 6, next.getWidth(), 3);
        int topy = next.getTopY() + next.getHeight() - 3;
        int rightx = next.getLeftX() + next.getWidth();
        g.fillPolygon(new int[] { rightx - 3, rightx, rightx }, new int[] { topy, topy, topy + 3 }, 3);
      } else {
        g.fillRect(next.getLeftX(), next.getTopY(), next.getWidth(), next.getHeight());
        g.setColor(Color.black);
        g.drawLine(next.getLeftX(), next.getTopY(), next.getRightX(), next.getTopY());
        g.drawLine(next.getLeftX(), next.getBottomY(), next.getRightX(), next.getBottomY());
        if (next.hasStyle("start")) {
          g.drawLine(next.getLeftX(), next.getTopY(), next.getLeftX(), next.getBottomY());
        }
        if (next.hasStyle("end")) {
          g.drawLine(next.getRightX(), next.getTopY(), next.getRightX(), next.getBottomY());
        }
      }
    }
  };

  private class ColouredRectanglePainter implements RectanglePainter {
    private Color myColor;

    private ColouredRectanglePainter(Color color) {
      myColor = color;
    }

    @Override
    public void paint(Rectangle next) {
      myGraphics.setColor(myColor);
      myGraphics.fillRect(next.getLeftX(), next.getTopY(), next.getWidth(), next.getHeight());
    }
  }

  @Override
  public void paint(Line line) {
    myLineRenderer.renderLine(line);
  }

  @Override
  public void paint(Text text) {
    myTextPainter.paint(text);
  }

  @Override
  public void paint(TextGroup textGroup) {
    myTextPainter.paint(textGroup);
  }

  @Override
  public void paint(Polygon p) {
    myPolygonRenderer.render(p);
  }
}
