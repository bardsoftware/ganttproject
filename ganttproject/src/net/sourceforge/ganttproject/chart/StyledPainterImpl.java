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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.HAlignment;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Label;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Line;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.TextGroup;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.VAlignment;
import net.sourceforge.ganttproject.shape.ShapeConstants;
import net.sourceforge.ganttproject.shape.ShapePaint;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.util.PropertiesUtil;
import net.sourceforge.ganttproject.util.TextLengthCalculatorImpl;

/**
 * Implements styled painters for the available primitives (see {@link GraphicPrimitiveContainer})
 *
 * @author bard
 */
public class StyledPainterImpl implements Painter {
    private Graphics2D myGraphics;

    private final Map<String,RectanglePainter> myStyle2painter = new HashMap<String, RectanglePainter>();

    private final TextLengthCalculatorImpl myTextLengthCalculator;

    private ChartUIConfiguration myConfig;

    private final int margin;

    /** List X coordinates used to draw polygons */
    private int[] myXPoints = new int[4];

    /** List Y coordinates used to draw polygons */
    private int[] myYPoints = new int[4];

    private Properties myProperties;

    /** Default stroke used for the primitives */
    private final static BasicStroke defaultStroke = new BasicStroke();

    private final static BasicStroke dependencyRubber = new BasicStroke(1f,
            BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f,
            new float[] { 2.5f }, 0f);

    public StyledPainterImpl(ChartUIConfiguration config) {
        myConfig = config;
        myTextLengthCalculator = new TextLengthCalculatorImpl(null);
        margin = myConfig.getMargin();

        myStyle2painter.put("task", myTaskRectanglePainter);
        myStyle2painter.put("task.start", myTaskStartRectanglePainter);
        myStyle2painter.put("task.end", myTaskEndRectanglePainter);
        myStyle2painter.put("task.startend", myTaskStartEndRectanglePainter);
        myStyle2painter.put("calendar.holiday", myCalendarHolidayPainter);
        myStyle2painter.put("task.milestone", myMilestonePainter);
        myStyle2painter.put("task.holiday", myTaskHolidayRectanglePainter);
        myStyle2painter.put("task.supertask", myTaskSupertaskRectanglePainter);
        myStyle2painter.put("task.supertask.start", mySupertaskStartPainter);
        myStyle2painter.put("task.supertask.end", mySupertaskEndPainter);
        myStyle2painter.put("task.projectTask",
                myTaskProjectTaskRectanglePainter);
        myStyle2painter
                .put("task.projectTask.start", myProjectTaskStartPainter);
        myStyle2painter.put("task.projectTask.end", myProjectTaskEndPainter);
        myStyle2painter.put("task.progress", new ColouredRectanglePainter(
                Color.BLACK));
        myStyle2painter.put("task.progress.end", new ColouredRectanglePainter(
                Color.BLACK));
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
    }

    public void setGraphics(Graphics g) {
        myGraphics = (Graphics2D) g;
        myTextLengthCalculator.setGraphics(myGraphics);
    }

    @Override
    public void prePaint() {
        myGraphics.setStroke(defaultStroke);
        myGraphics.setFont(myConfig.getChartFont());
    }

    @Override
    public void paint(GraphicPrimitiveContainer.Rectangle next) {
        assert myGraphics != null;
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
                myGraphics.drawRect(next.myLeftX, next.myTopY, next.myWidth,
                        next.myHeight);
            } else {
                myGraphics.setColor(next.getBackgroundColor());
                myGraphics.fillRect(next.myLeftX, next.myTopY, next.myWidth,
                        next.myHeight);
            }
        }
    }

    /** Interface providing a method to paint a rectangle (currently, used to draw many more other things...) */
    private interface RectanglePainter {
        public void paint(GraphicPrimitiveContainer.Rectangle next);
    }

    private final RectanglePainter myCalendarHolidayPainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            Color c = next.getBackgroundColor();
            myGraphics.setColor(c);
            myGraphics.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
        }
    };

    private class TaskRectanglePainter implements RectanglePainter {
        @Override
        public void paint(GraphicPrimitiveContainer.Rectangle next) {
            Object modelObject = next.getModelObject();
            if (modelObject instanceof TaskActivity == false) {
                throw new RuntimeException("Model object is expected to be TaskActivity ");
            }
            Task task = ((TaskActivity) modelObject).getTask();
            Color c = task.getColor();
            myGraphics.setColor(task.getColor());
            ShapePaint shapePaint = task.getShape();
            if (myConfig.isCriticalPathOn() && task.isCritical()) {
                shapePaint = new ShapePaint(ShapeConstants.THICK_BACKSLASH,
                        Color.BLACK, c);
            }

            if (shapePaint != null) {
                myGraphics.setPaint(shapePaint);
            }
            myGraphics.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
            myGraphics.setColor(Color.BLACK);
            drawBorder(myGraphics, next);
        }
        protected void drawBorder(Graphics g, Rectangle next) {
            g.drawLine(next.myLeftX-getCorrectionShift(), next.myTopY, next.getRightX()-getCorrectionShift(), next.myTopY);
            g.drawLine(next.myLeftX-getCorrectionShift(), next.getBottomY(), next.getRightX()-getCorrectionShift(), next.getBottomY());
        }
        protected int getCorrectionShift() {
            return 0;
        }
    }

    private final RectanglePainter myTaskRectanglePainter = new TaskRectanglePainter();
    private final RectanglePainter myTaskStartRectanglePainter = new TaskRectanglePainter() {
        @Override
        protected void drawBorder(Graphics g, Rectangle next) {
            super.drawBorder(g, next);
            g.drawLine(next.myLeftX, next.myTopY, next.myLeftX, next.getBottomY());
        }
        @Override
        protected int getCorrectionShift() {
            return -1;
        }
    };

    private final RectanglePainter myTaskEndRectanglePainter = new TaskRectanglePainter() {
        @Override
        protected void drawBorder(Graphics g, Rectangle next) {
            super.drawBorder(g, next);
            g.drawLine(next.getRightX()-1, next.myTopY, next.getRightX()-1, next.getBottomY());
        }
        @Override
        protected int getCorrectionShift() {
            return 1;
        }
    };

    private final RectanglePainter myTaskStartEndRectanglePainter = new TaskRectanglePainter() {
        @Override
        protected void drawBorder(Graphics g, Rectangle next) {
            super.drawBorder(g, next);
            g.drawLine(next.myLeftX, next.myTopY, next.myLeftX, next.getBottomY());
            g.drawLine(next.getRightX(), next.myTopY, next.getRightX(), next.getBottomY());
        }
    };

    private final RectanglePainter myTaskHolidayRectanglePainter = new RectanglePainter() {
        float myAlphaValue = 0;
        Composite myAlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, myAlphaValue);

        @Override
        public void paint(GraphicPrimitiveContainer.Rectangle next) {
            if (myAlphaValue != myConfig.getWeekendAlphaValue()) {
                myAlphaValue = myConfig.getWeekendAlphaValue();
                myAlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, myAlphaValue);
            }
            Object modelObject = next.getModelObject();
            if (modelObject instanceof TaskActivity==false) {
                throw new RuntimeException("Model object is expected to be TaskActivity ");
            }
            Task task = ((TaskActivity) modelObject).getTask();
            myGraphics.setColor(task.getColor());
            Composite oldComposite = myGraphics.getComposite();
            myGraphics.setComposite(myAlphaComposite);
            myGraphics.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
            myGraphics.setColor(Color.black);
            myGraphics.drawLine(next.myLeftX, next.myTopY, next.getRightX(), next.myTopY);
            myGraphics.drawLine(next.myLeftX, next.getBottomY(), next.getRightX(), next.getBottomY());
            //g.drawRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);

            myGraphics.setComposite(oldComposite);
        }
    };

    private final RectanglePainter myTaskSupertaskRectanglePainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            Color c = next.getBackgroundColor();
            if (c == null) {
                c = getDefaultColor();
            }
            if (myConfig.isCriticalPathOn()
                    && ((TaskActivity) next.getModelObject()).getTask()
                            .isCritical()){
                c = Color.RED;
            }
            myGraphics.setColor(c);
            myGraphics.fillRect(next.myLeftX, next.getBottomY() - 6,
                    next.myWidth, 3);
        }
        private Color getDefaultColor() {
            return Color.BLACK;
        }

    };

    private final RectanglePainter mySupertaskStartPainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            int bottomy = next.getBottomY() - 3;
            myXPoints[0] = next.myLeftX;
            myYPoints[0] = bottomy;
            myXPoints[1] = next.myLeftX + 3;
            myYPoints[1] = bottomy;
            myXPoints[2] = next.myLeftX;
            myYPoints[2] = bottomy + 3;
            myGraphics.setColor(Color.BLACK);
            myGraphics.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private final RectanglePainter mySupertaskEndPainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            int bottomy = next.getBottomY() - 3;
            int rightx = next.getRightX();
            myXPoints[0] = rightx - 3;
            myYPoints[0] = bottomy;
            myXPoints[1] = rightx;
            myYPoints[1] = bottomy;
            myXPoints[2] = rightx;
            myYPoints[2] = bottomy + 3;
            myGraphics.setColor(Color.BLACK);
            myGraphics.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private final RectanglePainter myTaskProjectTaskRectanglePainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            Color c = getDefaultColor();
            if (myConfig.isCriticalPathOn()
                    && ((TaskActivity) next.getModelObject()).getTask()
                            .isCritical()) {
                c = Color.RED;
            }
            myGraphics.setColor(c);
            myGraphics.fillRect(next.myLeftX, next.myTopY + next.myHeight - 9,
                    next.myWidth, 6);
        }

        private Color getDefaultColor() {
            return Color.BLACK;
        }
    };

    private final RectanglePainter myProjectTaskStartPainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            int bottomy = next.getBottomY() - 3;
            myXPoints[0] = next.myLeftX;
            myYPoints[0] = bottomy;
            myXPoints[1] = next.myLeftX + 3;
            myYPoints[1] = bottomy;
            myXPoints[2] = next.myLeftX;
            myYPoints[2] = bottomy + 3;
            myGraphics.setColor(Color.BLACK);
            myGraphics.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private final RectanglePainter myProjectTaskEndPainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            int bottomy = next.getBottomY() - 3;
            int rightx = next.getRightX();
            myXPoints[0] = rightx - 3;
            myYPoints[0] = bottomy;
            myXPoints[1] = rightx;
            myYPoints[1] = bottomy;
            myXPoints[2] = rightx;
            myYPoints[2] = bottomy + 3;
            myGraphics.setColor(Color.BLACK);
            myGraphics.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private final RectanglePainter myMilestonePainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            Object modelObject = next.getModelObject();
            if (modelObject instanceof TaskActivity == false) {
                throw new RuntimeException(
                        "Model object is expected to be TaskActivity ");
            }
            Task task = ((TaskActivity) modelObject).getTask();
            Color c = task.getColor();
            if (myConfig.isCriticalPathOn()
                    && ((TaskActivity) next.getModelObject()).getTask()
                            .isCritical()) {
                c = Color.RED;
            }

            int middleX = next.getMiddleX();
            int middleY = next.getMiddleY();
            myXPoints[0] = middleX - next.myHeight / 2;
            myYPoints[0] = middleY;
            myXPoints[1] = middleX;
            myYPoints[1] = next.myTopY;
            myXPoints[2] = middleX + next.myHeight / 2;
            myYPoints[2] = middleY;
            myXPoints[3] = middleX;
            myYPoints[3] = next.getBottomY();
            myGraphics.setColor(c);
            myGraphics.fillPolygon(myXPoints, myYPoints, 4);
        }
    };

    private final RectanglePainter myArrowDownPainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            myXPoints[0] = next.myLeftX;
            myXPoints[1] = next.getRightX();
            myXPoints[2] = next.getMiddleX();
            myYPoints[0] = next.myTopY;
            myYPoints[1] = next.myTopY;
            myYPoints[2] = next.getBottomY();
            myGraphics.setColor(Color.BLACK);
            myGraphics.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private final RectanglePainter myArrowUpPainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            myXPoints[0] = next.myLeftX;
            myXPoints[1] = next.getRightX();
            myXPoints[2] = next.getMiddleX();
            myYPoints[0] = next.getBottomY();
            myYPoints[1] = next.getBottomY();
            myYPoints[2] = next.myTopY;
            myGraphics.setColor(Color.BLACK);
            myGraphics.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private final RectanglePainter myArrowLeftPainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            g.setColor(Color.BLACK);
            myXPoints[0] = next.myLeftX;
            myXPoints[1] = next.getRightX();
            myXPoints[2] = next.getRightX();
            myYPoints[0] = next.getMiddleY();
            myYPoints[1] = next.myTopY;
            myYPoints[2] = next.getBottomY();
            g.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private final RectanglePainter myArrowRightPainter = new RectanglePainter() {
        @Override
        public void paint(Rectangle next) {
            myXPoints[0] = next.myLeftX;
            myXPoints[1] = next.getRightX();
            myXPoints[2] = next.myLeftX;
            myYPoints[0] = next.myTopY;
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
            myGraphics.setColor(new Color(c.getRed(), c.getGreen(),
                    c.getBlue(), 100));
            myGraphics.fillRect(next.myLeftX, next.myTopY + margin,
                    next.myWidth, next.myHeight - 2 * margin);
            myGraphics.setColor(Color.BLACK);
            myGraphics.drawLine(next.myLeftX, next.myTopY + margin,
                    next.myLeftX, next.getBottomY() - margin);
            myGraphics.drawLine(next.myLeftX, next.myTopY + margin, next
                    .getRightX(), next.myTopY + margin);
            myGraphics.drawLine(next.myLeftX, next.getBottomY() - margin, next
                    .getRightX(), next.getBottomY() - margin);
            myGraphics.drawLine(next.getRightX(), next.myTopY + margin, next
                    .getRightX(), next.getBottomY() - margin);
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

            myGraphics.fillRect(next.myLeftX, next.myTopY + margin, next.myWidth,
                    next.myHeight - 2 * margin);
            if (style.indexOf(".first") > 0) {
                myGraphics.setColor(Color.BLACK);
                myGraphics.drawLine(next.myLeftX, next.myTopY + margin, next.myLeftX,
                        next.getBottomY() - margin);
            }
            if (style.indexOf(".last") > 0) {
                myGraphics.setColor(Color.BLACK);
                myGraphics.drawLine(next.getRightX(), next.myTopY + margin, next
                        .getRightX(), next.getBottomY() - margin);
            }
            myGraphics.setColor(Color.BLACK);

            ResourceLoadRenderer.ResourceLoad load = (ResourceLoadRenderer.ResourceLoad) next
                    .getModelObject();
            int loadInt = Math.round(load.getLoad());
            String loadStr = loadInt + "%";
            int emsLength = myTextLengthCalculator.getTextLength(loadStr);
            boolean displayLoad = (loadInt != 100 && emsLength <= next.myWidth);
            if (displayLoad) {
                myGraphics.drawString(loadStr, next.getMiddleX()
                        - myTextLengthCalculator.getTextLength(loadStr) / 2,
                        next.myTopY + margin + next.myHeight / 2);
                myGraphics.drawLine(next.myLeftX, next.myTopY + margin, next.myLeftX,
                        next.getBottomY() - margin);
            }
            myGraphics.setColor(Color.BLACK);
            myGraphics.drawLine(next.myLeftX, next.myTopY + margin, next.getRightX(),
                    next.myTopY + margin);
            myGraphics.drawLine(next.myLeftX, next.getBottomY() - margin, next
                    .getRightX(), next.getBottomY() - margin);
        }
    };

    private final RectanglePainter myPreviousStateTaskRectanglePainter = new RectanglePainter() {
        private int[] myXPoints = new int[4];
        private int[] myYPoints = new int[4];

        @Override
        public void paint(GraphicPrimitiveContainer.Rectangle next) {
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
                int middleX = (next.myWidth <= next.myHeight) ?
                    next.getRightX() - next.myWidth / 2 : next.myLeftX + next.myHeight / 2;
                int middleY = next.getMiddleY();

                myXPoints[0] = next.myLeftX + 2;
                myYPoints[0] = middleY;
                myXPoints[1] = middleX + 3;
                myYPoints[1] = next.myTopY - 1;
                myXPoints[2] = (next.myWidth <= next.myHeight) ? next
                        .getRightX() + 4 : next.myLeftX + next.myHeight + 4;
                myYPoints[2] = middleY;
                myXPoints[3] = middleX + 3;
                myYPoints[3] = next.getBottomY() + 1;

                g.fillPolygon(myXPoints, myYPoints, 4);
            } else if (next.hasStyle("super")) {
                g.fillRect(next.myLeftX, next.myTopY + next.myHeight - 6,
                        next.myWidth, 3);
                int topy = next.myTopY + next.myHeight - 3;
                int rightx = next.myLeftX + next.myWidth;
                g.fillPolygon(new int[] { rightx - 3, rightx, rightx },
                        new int[] { topy, topy, topy + 3 }, 3);
            } else {
                g.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
                g.setColor(Color.black);
                g.drawLine(next.myLeftX, next.myTopY, next.getRightX(), next.myTopY);
                g.drawLine(next.myLeftX, next.getBottomY(), next.getRightX(), next.getBottomY());
                if (next.hasStyle("start")) {
                    g.drawLine(next.myLeftX, next.myTopY, next.myLeftX, next.getBottomY());
                }
                if (next.hasStyle("end")) {
                    g.drawLine(next.getRightX(), next.myTopY, next.getRightX(), next.getBottomY());
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
            myGraphics.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
        }
    }

    @Override
    public void paint(Line line) {
        Color foreColor = line.getForegroundColor();
        if (foreColor == null) {
            foreColor = Color.BLACK;
        }
        myGraphics.setColor(foreColor);
        if ("dependency.line.rubber".equals(line.getStyle())) {
            myGraphics.setStroke(dependencyRubber);
        }
        myGraphics.drawLine(line.getStartX(), line.getStartY(),
                line.getFinishX(), line.getFinishY());

        if ("dependency.line.rubber".equals(line.getStyle())) {
            // Revert to default stroke
            myGraphics.setStroke(defaultStroke);
        }
    }

    @Override
    public void paint(Text next) {
        Font graphicFont = null;
        Color foreColor = next.getForegroundColor();
        if (foreColor == null) {
            foreColor = Color.BLACK;
        }
        myGraphics.setColor(foreColor);

        if (next.getFont() != null && (next.getStyle() == null || next.getStyle().equals("text.ganttinfo") == false)) {
            graphicFont = myGraphics.getFont();
            myGraphics.setFont(next.getFont());
        }

        Label[] labels = next.getLabels(myTextLengthCalculator);
        if (labels.length == 0) {
            return;
        }
//        int actualLength = myTextLengthCalculator.getTextLength(nextTextString);
//        if (requestedMaxLength >= 0 && actualLength > requestedMaxLength) {
//            return; // Text is too large
//        }
        //FIXME This check if not 100% working (when scrolling to the right the text seems to disappear too soon...)
//        if (next.getLeftX() + actualLength < 0) {
//            return; // Text is not visible: too far to the left for current view
//        }
        Label label = labels[0];
        if (label == null) {
            return;
        }
        paint(next.getLeftX(), next.getBottomY(), next.getHAlignment(), next.getVAlignment(), label);
        if (graphicFont != null) {
            // Set Font back to original font
            myGraphics.setFont(graphicFont);
        }
    }

    private void paint(int xleft, int ybottom, HAlignment alignHor, VAlignment alignVer, Label label) {
        switch (alignHor) {
        case CENTER:
            xleft = xleft - label.lengthPx / 2;
            break;
        case RIGHT:
            xleft = xleft - label.lengthPx;
            break;
        }
        switch (alignVer) {
        case CENTER:
            ybottom = ybottom + myGraphics.getFont().getSize() / 2;
            break;
        case TOP:
            ybottom = ybottom + myGraphics.getFont().getSize();
            break;
        }
        myGraphics.drawString(label.text, xleft, ybottom);
    }

    @Override
    public void paint(TextGroup textGroup) {
        TextLengthCalculatorImpl calculator = new TextLengthCalculatorImpl((Graphics2D)myGraphics.create());
        FontChooser fontChooser = new FontChooser(myProperties, calculator);
        textGroup.setFonts(fontChooser);
        for (int i = 0; i < textGroup.getLineCount(); i++) {
            paintTextLine(textGroup, i);
        }
    }

    private void paintTextLine(TextGroup textGroup, int lineNum) {
        List<Text> line = textGroup.getLine(lineNum);
        Font savedFont = myGraphics.getFont();
        Color savedColor = myGraphics.getColor();

        myGraphics.setFont(textGroup.getFont(lineNum));
        myGraphics.setColor(textGroup.getColor(lineNum));

        List<Label[]> labelList = new ArrayList<Label[]>();
        int maxIndex = Integer.MAX_VALUE;
        for (Text t : line) {
            Label[] labels = t.getLabels(myTextLengthCalculator);
            maxIndex = Math.min(maxIndex, labels.length);
            if (maxIndex == 0) {
                return;
            }
            labelList.add(labels);
        }

        for (int i = 0; i < labelList.size(); i++) {
            Label longest = labelList.get(i)[maxIndex - 1];
            Text t = line.get(i);
            paint(textGroup.getLeftX() + t.getLeftX(), textGroup.getBottomY(lineNum), t.getHAlignment(), t.getVAlignment(), longest);
        }

        myGraphics.setFont(savedFont);
        myGraphics.setColor(savedColor);
    }
}
