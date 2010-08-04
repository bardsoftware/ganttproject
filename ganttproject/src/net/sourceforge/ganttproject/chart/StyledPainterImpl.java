package net.sourceforge.ganttproject.chart;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.HAlignment;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.VAlignment;
import net.sourceforge.ganttproject.shape.ShapeConstants;
import net.sourceforge.ganttproject.shape.ShapePaint;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.time.TimeUnitText;
import net.sourceforge.ganttproject.util.TextLengthCalculatorImpl;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class StyledPainterImpl implements Painter {
    private Graphics myGraphics;

    private final Map myStyle2painter = new HashMap();

    private final TextLengthCalculatorImpl myTextLengthCalculator;

    private ChartUIConfiguration myConfig;

    private final int margin;

    public StyledPainterImpl(ChartUIConfiguration configuration) {
        // myGraphics = g;
        myStyle2painter.put("task", myTaskRectanglePainter);
        myStyle2painter.put("task.start", myTaskStartRectanglePainter);
        myStyle2painter.put("task.end", myTaskEndRectanglePainter);
        myStyle2painter.put("task.startend", myTaskStartEndRectanglePainter);        
        myConfig = configuration;
        myStyle2painter.put("calendar.holiday", myCalendarHolidayPainter);
        myStyle2painter.put("task.milestone", myMilestonePanter);
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
        myStyle2painter.put("previousStateTask",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.earlier",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.later",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.milestone",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.milestone.earlier",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.milestone.later",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.super",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.super.earlier",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.super.later",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.super.apart",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.super.apart.earlier",
                myPreviousStateTaskRectanglePainter);
        myStyle2painter.put("previousStateTask.super.apart.later",
                myPreviousStateTaskRectanglePainter);
        myTextLengthCalculator = new TextLengthCalculatorImpl(myGraphics);
        margin = myConfig.getMargin();
    }

    private Map myGraphics2calculator = new HashMap();

    public void setGraphics(Graphics g) {
        myGraphics = g;
        myTextLengthCalculator.setGraphics(g);
    }

    public void paint(GraphicPrimitiveContainer.Rectangle next) {
        if (myGraphics == null) {
            throw new RuntimeException("Graphics is null");
        }
        Graphics g = myGraphics;
        RectanglePainter painter = (RectanglePainter) myStyle2painter.get(next
                .getStyle());
        if (painter != null) {
            painter.paint(next);
        } else {
            if (next.getBackgroundColor() == null) {
                Color foreColor = next.getForegroundColor();
                if (foreColor == null) {
                    foreColor = Color.BLACK;
                }
                g.setColor(foreColor);
                g.drawRect(next.myLeftX, next.myTopY, next.myWidth,
                        next.myHeight);
            } else {
                g.setColor(next.getBackgroundColor());
                g.fillRect(next.myLeftX, next.myTopY, next.myWidth,
                        next.myHeight);
            }
        }
    }

    private RectanglePainter myCalendarHolidayPainter = new RectanglePainter() {
        public void paint(Rectangle next) {
            Color c = next.getBackgroundColor();
            Graphics2D g = (Graphics2D) myGraphics;
            g.setColor(c);
            g.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
        }
    };
//    private RectanglePainter myCalendarHolidayPainter = new RectanglePainter() {
//        Composite myAlphaComposite = AlphaComposite.getInstance(
//                AlphaComposite.SRC_OVER, 0.6f);
//
//        public void paint(Rectangle next) {
//            Color c = next.getBackgroundColor();
//            Graphics2D g = (Graphics2D) myGraphics;
//            g.setColor(c);
//            Composite was = g.getComposite();
//            g.setComposite(myAlphaComposite);
//            g.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
//            g.setComposite(was);
//        }
//    };

    class TaskRectanglePainter implements RectanglePainter {
        public void paint(GraphicPrimitiveContainer.Rectangle next) {
            Object modelObject = next.getModelObject();
            if (modelObject instanceof TaskActivity==false) {
                throw new RuntimeException("Model object is expected to be TaskActivity ");
            }
            Task task = ((TaskActivity)modelObject).getTask();
            Color c = task.getColor();
            if (c==null) {
                c = getDefaultColor();
            }
            Graphics2D g = (Graphics2D) myGraphics;
            g.setColor(c);
            ShapePaint shapePaint = task.getShape();
            if (myConfig.isCriticalPathOn() && task.isCritical()) {
                shapePaint = new ShapePaint(ShapeConstants.THICK_BACKSLASH,
                        Color.BLACK, c);
            }
            
            if (shapePaint!=null) {
                g.setPaint(shapePaint);
            }
            g.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
            g.setColor(Color.black);
            drawBorder(g, next);
        }
        protected void drawBorder(Graphics g, Rectangle next) {
            g.drawLine(next.myLeftX-getCorrectionShift(), next.myTopY, next.getRightX()-getCorrectionShift(), next.myTopY);
            g.drawLine(next.myLeftX-getCorrectionShift(), next.getBottomY(), next.getRightX()-getCorrectionShift(), next.getBottomY());
        }
        private Color getDefaultColor() {
            return Color.BLUE;
        }        
        protected int getCorrectionShift() {
            return 0;
        }
    }
    
    private RectanglePainter myTaskRectanglePainter = new TaskRectanglePainter();
    private RectanglePainter myTaskStartRectanglePainter = new TaskRectanglePainter() {
        protected void drawBorder(Graphics g, Rectangle next) {
            super.drawBorder(g, next);
            g.drawLine(next.myLeftX, next.myTopY, next.myLeftX, next.getBottomY());
        }
        protected int getCorrectionShift() {
            return -1;
        }
        
    };
    private RectanglePainter myTaskEndRectanglePainter = new TaskRectanglePainter() {
        protected void drawBorder(Graphics g, Rectangle next) {
            super.drawBorder(g, next);
            g.drawLine(next.getRightX()-1, next.myTopY, next.getRightX()-1, next.getBottomY());
        }
        protected int getCorrectionShift() {
            return 1;
        }
    };
    private RectanglePainter myTaskStartEndRectanglePainter = new TaskRectanglePainter() {
        protected void drawBorder(Graphics g, Rectangle next) {
            super.drawBorder(g, next);
            g.drawLine(next.myLeftX, next.myTopY, next.myLeftX, next.getBottomY());
            g.drawLine(next.getRightX(), next.myTopY, next.getRightX(), next.getBottomY());
        }
    };

    private RectanglePainter myTaskHolidayRectanglePainter = new RectanglePainter() {
        float myAlphaValue = 0;
        Composite myAlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, myAlphaValue);
        
        public void paint(GraphicPrimitiveContainer.Rectangle next) {
            if (myAlphaValue!=myConfig.getWeekendAlphaValue()) {
                myAlphaValue = myConfig.getWeekendAlphaValue();
                myAlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, myAlphaValue);
            }
            Object modelObject = next.getModelObject();
            if (modelObject instanceof TaskActivity==false) {
                throw new RuntimeException("Model object is expected to be TaskActivity ");
            }
            Task task = ((TaskActivity)modelObject).getTask();
            Color c = task.getColor();
            if (c==null) {
                c = getDefaultColor();
            }
            Graphics2D g = (Graphics2D) myGraphics;
            g.setColor(c);            
            Composite was = g.getComposite();
            g.setComposite(myAlphaComposite);
            g.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
            g.setColor(Color.black);
            g.drawLine(next.myLeftX, next.myTopY, next.getRightX(), next.myTopY);
            g.drawLine(next.myLeftX, next.getBottomY(), next.getRightX(), next.getBottomY());
            //g.drawRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
            
            g.setComposite(was);
        }
        private Color getDefaultColor() {
            return Color.BLUE;
        }


    };
    
//    
    private RectanglePainter myTaskSupertaskRectanglePainter = new RectanglePainter() {
        public void paint(Rectangle next) {
            Color c = next.getBackgroundColor();
            if (c == null) {
                c = getDefaultColor();
            }
            if (myConfig.isCriticalPathOn()
                    && ((TaskActivity) next.getModelObject()).getTask()
                            .isCritical())
                c = Color.RED;

            Graphics g = myGraphics;
            g.setColor(c);
            g.fillRect(next.myLeftX, next.myTopY + next.myHeight - 6,
                    next.myWidth, 3);
        }

        private Color getDefaultColor() {
            return Color.BLACK;
        }

    };

    private RectanglePainter mySupertaskStartPainter = new RectanglePainter() {
        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            Color c = Color.BLACK;
            // if(((TaskActivity)next.getModelObject()).getTask().isCritical())
            // c = Color.RED;

            g.setColor(c);
            int topy = next.myTopY + next.myHeight - 3;
            g.fillPolygon(new int[] { next.myLeftX, next.myLeftX + 3,
                    next.myLeftX }, new int[] { topy, topy, topy + 3 }, 3);
        }
    };

    private RectanglePainter mySupertaskEndPainter = new RectanglePainter() {
        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            Color c = Color.BLACK;
            // if(((TaskActivity)next.getModelObject()).getTask().isCritical())
            // c = Color.RED;

            g.setColor(c);

            int topy = next.myTopY + next.myHeight - 3;
            int rightx = next.myLeftX + next.myWidth;
            g.fillPolygon(new int[] { rightx - 3, rightx, rightx }, new int[] {
                    topy, topy, topy + 3 }, 3);
        }
    };

    private RectanglePainter myTaskProjectTaskRectanglePainter = new RectanglePainter() {
        public void paint(Rectangle next) {
            Color c = getDefaultColor();
            if (myConfig.isCriticalPathOn()
                    && ((TaskActivity) next.getModelObject()).getTask()
                            .isCritical())
                c = Color.RED;

            Graphics g = myGraphics;
            g.setColor(c);
            g.fillRect(next.myLeftX, next.myTopY + next.myHeight - 9,
                    next.myWidth, 6);
        }

        private Color getDefaultColor() {
            return Color.BLACK;
        }

    };

    private RectanglePainter myProjectTaskStartPainter = new RectanglePainter() {
        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            Color c = Color.BLACK;
            // if(((TaskActivity)next.getModelObject()).getTask().isCritical())
            // c = Color.RED;

            g.setColor(c);
            int topy = next.myTopY + next.myHeight - 3;
            g.fillPolygon(new int[] { next.myLeftX, next.myLeftX + 3,
                    next.myLeftX }, new int[] { topy, topy, topy + 3 }, 3);
        }
    };

    private RectanglePainter myProjectTaskEndPainter = new RectanglePainter() {
        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            Color c = Color.BLACK;
            // if(((TaskActivity)next.getModelObject()).getTask().isCritical())
            // c = Color.RED;

            g.setColor(c);

            int topy = next.myTopY + next.myHeight - 3;
            int rightx = next.myLeftX + next.myWidth;
            g.fillPolygon(new int[] { rightx - 3, rightx, rightx }, new int[] {
                    topy, topy, topy + 3 }, 3);
        }
    };

    private RectanglePainter myMilestonePanter = new RectanglePainter() {
        private int[] myXPoints = new int[4];

        private int[] myYPoints = new int[4];

        public void paint(Rectangle next) {
            Object modelObject = next.getModelObject();
            if (modelObject instanceof TaskActivity == false) {
                throw new RuntimeException(
                        "Model object is expected to be TaskActivity ");
            }
            Task task = ((TaskActivity) modelObject).getTask();
            Color c = task.getColor();
            Graphics g = myGraphics;
            if (myConfig.isCriticalPathOn()
                    && ((TaskActivity) next.getModelObject()).getTask()
                            .isCritical())
                c = Color.RED;

            g.setColor(c);
            int middleX = next.getRightX() - next.myWidth / 2;
            int middleY = next.getBottomY() - next.myHeight / 2;
            myXPoints[0] = middleX - next.myHeight / 2;
            myXPoints[1] = middleX;
            myXPoints[2] = middleX + next.myHeight / 2;
            myXPoints[3] = middleX;
            myYPoints[0] = middleY;
            myYPoints[1] = next.myTopY;
            myYPoints[2] = middleY;
            myYPoints[3] = next.getBottomY();

            g.fillPolygon(myXPoints, myYPoints, 4);
        }

    };

    private RectanglePainter myArrowDownPainter = new RectanglePainter() {
        private int[] myXPoints = new int[3];

        private int[] myYPoints = new int[3];

        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            g.setColor(Color.BLACK);
            myXPoints[0] = next.myLeftX;
            myXPoints[1] = next.getRightX();
            myXPoints[2] = next.getMiddleX();
            myYPoints[0] = next.myTopY;
            myYPoints[1] = next.myTopY;
            myYPoints[2] = next.getBottomY();
            g.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private RectanglePainter myArrowUpPainter = new RectanglePainter() {
        private int[] myXPoints = new int[3];

        private int[] myYPoints = new int[3];

        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            g.setColor(Color.BLACK);
            myXPoints[0] = next.myLeftX;
            myXPoints[1] = next.getRightX();
            myXPoints[2] = next.getMiddleX();
            myYPoints[0] = next.getBottomY();
            myYPoints[1] = next.getBottomY();
            myYPoints[2] = next.myTopY;
            g.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private RectanglePainter myArrowLeftPainter = new RectanglePainter() {
        private int[] myXPoints = new int[3];

        private int[] myYPoints = new int[3];

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

    private RectanglePainter myArrowRightPainter = new RectanglePainter() {
        private int[] myXPoints = new int[3];

        private int[] myYPoints = new int[3];

        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            g.setColor(Color.BLACK);
            myXPoints[0] = next.myLeftX;
            myXPoints[1] = next.getRightX();
            myXPoints[2] = next.myLeftX;
            myYPoints[0] = next.myTopY;
            myYPoints[1] = next.getMiddleY();
            myYPoints[2] = next.getBottomY();
            g.fillPolygon(myXPoints, myYPoints, 3);
        }
    };

    private RectanglePainter myDayOffPainter = new RectanglePainter() {
        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            String style = next.getStyle();

            int margin = StyledPainterImpl.this.margin - 3;
            Color c = myConfig.getDayOffColor();
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));
            g.fillRect(next.myLeftX, next.myTopY + margin, next.myWidth,
                    next.myHeight - 2 * margin);
            g.setColor(Color.BLACK);
            g.drawLine(next.myLeftX, next.myTopY + margin, next.myLeftX, next
                    .getBottomY()
                    - margin);
            g.drawLine(next.myLeftX, next.myTopY + margin, next.getRightX(),
                    next.myTopY + margin);
            g.drawLine(next.myLeftX, next.getBottomY() - margin, next
                    .getRightX(), next.getBottomY() - margin);
            g.drawLine(next.getRightX(), next.myTopY + margin,
                    next.getRightX(), next.getBottomY() - margin);
        }
    };

    private RectanglePainter myResourceLoadPainter = new RectanglePainter() {
        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            String style = next.getStyle();
            g.setFont(myConfig.getChartFont());

            Color color;
            if (style.indexOf("overload") > 0)
                color = myConfig.getResourceOverloadColor();
            else if (style.indexOf("underload") > 0)
                color = myConfig.getResourceUnderLoadColor();
            else
                color = myConfig.getResourceNormalLoadColor();

            g.setColor(color);
            g.fillRect(next.myLeftX, next.myTopY + margin, next.myWidth,
                    next.myHeight - 2 * margin);
            if (style.indexOf(".first") > 0) {
                g.setColor(Color.BLACK);
                g.drawLine(next.myLeftX, next.myTopY + margin, next.myLeftX,
                        next.getBottomY() - margin);
            }
            if (style.indexOf(".last") > 0) {
                g.setColor(Color.BLACK);
                g.drawLine(next.getRightX(), next.myTopY + margin, next
                        .getRightX(), next.getBottomY() - margin);
            }
            g.setColor(Color.BLACK);
            ResourceLoadRenderer.ResourceLoad load = (ResourceLoadRenderer.ResourceLoad) next
                    .getModelObject();
            int l = Math.round(load.getLoad());
            String disp = l + "%";
            int emsLength = myTextLengthCalculator.getTextLength(disp);
            boolean display = emsLength <= next.myWidth;
            if (load.getLoad() != 100f && display) {
                g.drawString(disp, next.getMiddleX()
                        - myTextLengthCalculator.getTextLength(disp) / 2,
                        next.myTopY + margin + next.myHeight / 2);
                g.drawLine(next.myLeftX, next.myTopY + margin, next.myLeftX,
                        next.getBottomY() - margin);
            }
            g.setColor(Color.BLACK);
            g.drawLine(next.myLeftX, next.myTopY + margin, next.getRightX(),
                    next.myTopY + margin);
            g.drawLine(next.myLeftX, next.getBottomY() - margin, next
                    .getRightX(), next.getBottomY() - margin);
        }
    };

    private RectanglePainter myPreviousStateTaskRectanglePainter = new RectanglePainter() {
        private int[] myXPoints = new int[4];

        private int[] myYPoints = new int[4];

        public void paint(GraphicPrimitiveContainer.Rectangle next) {
            Object modelObject = next.getModelObject();
            Graphics g = myGraphics;
            String style = next.getStyle();
            Color c;

            if (style.indexOf("earlier") > 0)
                c = myConfig.getEarlierPreviousTaskColor();
            else if (style.indexOf("later") > 0)
                c = myConfig.getLaterPreviousTaskColor();
            else
                c = myConfig.getPreviousTaskColor();
            g.setColor(c);
            if (style.indexOf("milestone") > 0) {
                int middleX = (next.myWidth <= next.myHeight) ? next
                        .getRightX()
                        - next.myWidth / 2 : next.myLeftX + next.myHeight / 2;
                int middleY = next.getBottomY() - next.myHeight / 2;

                myXPoints[0] = next.myLeftX + 2;
                ;
                myXPoints[1] = middleX + 3;
                myXPoints[2] = (next.myWidth <= next.myHeight) ? next
                        .getRightX() + 4 : next.myLeftX + next.myHeight + 4;
                myXPoints[3] = middleX + 3;
                myYPoints[0] = middleY;
                myYPoints[1] = next.myTopY - 1;
                ;
                myYPoints[2] = middleY;
                myYPoints[3] = next.getBottomY() + 1;
                ;

                g.fillPolygon(myXPoints, myYPoints, 4);
            } else if (style.indexOf("super") > 0) {
                g.fillRect(next.myLeftX, next.myTopY + next.myHeight - 6,
                        next.myWidth, 3);
                int topy = next.myTopY + next.myHeight - 3;
                // if the super task is completely displayed
                // so whe draw the left triangle
                if (style.indexOf("apart") <= 0)
                    g.fillPolygon(new int[] { next.myLeftX, next.myLeftX + 3,
                            next.myLeftX }, new int[] { topy, topy, topy + 3 },
                            3);

                int rightx = next.myLeftX + next.myWidth;
                g.fillPolygon(new int[] { rightx - 3, rightx, rightx },
                        new int[] { topy, topy, topy + 3 }, 3);
            } else {

                g.fillRect(next.myLeftX, next.myTopY, next.myWidth,
                        next.myHeight);
                g.setColor(Color.black);
                g.drawRect(next.myLeftX, next.myTopY, next.myWidth,
                        next.myHeight);
            }
        }

    };

    private interface RectanglePainter {
        public void paint(GraphicPrimitiveContainer.Rectangle next);
    }

    private class ColouredRectanglePainter implements RectanglePainter {
        private Color myColor;

        private ColouredRectanglePainter(Color color) {
            myColor = color;
        }

        public void paint(Rectangle next) {
            Graphics g = myGraphics;
            g.setColor(myColor);
            g.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
        }

    }

    public void paint(Text next) {
        Font graphicFont = myGraphics.getFont();
        int requestedMaxLength = next.getMaxLength();
        Color foreColor = next.getForegroundColor();
        if (foreColor == null) {
            foreColor = Color.BLACK;
        }
        myGraphics.setColor(foreColor);

        if (next.getFont() != null) {
            myGraphics.setFont(next.getFont());
        } else {
            myGraphics.setFont(myConfig.getChartFont());
        }

        if (next.getStyle() != null && next.getStyle().equals("text.ganttinfo"))
//            myGraphics.setFont(myConfig.getChartFont().deriveFont(10f));
            myGraphics.setFont(myConfig.getChartFont());
        int actualLength;
        String nextTextString = next.getText();
        if (next.getModelObject() != null) {
            TimeUnitText nextText = (TimeUnitText) next.getModelObject();
            nextTextString = nextText.getText(requestedMaxLength,
                    myTextLengthCalculator);
            actualLength = requestedMaxLength;
        } else {
            actualLength = TextLengthCalculatorImpl.getTextLength(myGraphics,
                    next.getText());
            if (requestedMaxLength >= 0 && actualLength > requestedMaxLength) {
                return;
            }
        }
        int fontHeight = myGraphics.getFont().getSize();
        int xleft = next.getLeftX();
        int ybottom = next.getBottomY();
        if (HAlignment.CENTER == next.getHAlignment()) {
            xleft -= actualLength / 2;
        }
        if (HAlignment.RIGHT == next.getHAlignment()) {
            xleft -= actualLength;
        }
        if (VAlignment.CENTER == next.getVAlignment()) {
            ybottom += fontHeight / 2;
        }
        if (VAlignment.TOP == next.getVAlignment()) {
            ybottom += fontHeight;
        }
        myGraphics.drawString(nextTextString, xleft, ybottom);
        myGraphics.setFont(graphicFont);
    }
}
