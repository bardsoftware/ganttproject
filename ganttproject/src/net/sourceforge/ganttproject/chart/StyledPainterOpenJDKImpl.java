package net.sourceforge.ganttproject.chart;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import net.sourceforge.ganttproject.chart.StyledPainterImpl;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.chart.StyledPainterImpl.RectanglePainter;
import net.sourceforge.ganttproject.chart.StyledPainterImpl.TaskRectanglePainter;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

/**
 * Painter for OpenJDK, corrects OpenJDK problems with StyledPainterImpl
 * 
 * @author Maarten Bezemer
 */
public class StyledPainterOpenJDKImpl extends StyledPainterImpl {

    public StyledPainterOpenJDKImpl(ChartUIConfiguration configuration) {
        super(configuration);

        myTaskRectanglePainter = new TaskRectanglePainter() {
            protected void drawBorder(Graphics g, Rectangle next) {
                super.drawBorder(g, next.myLeftX - getCorrectionShift(), next.myTopY - 1,
                        next.getRightX() - getCorrectionShift(), next.getBottomY());
            }        
        };

        myTaskStartRectanglePainter = new TaskRectanglePainter() {
            protected void drawBorder(Graphics g, Rectangle next) {
                super.drawBorder(g, next.myLeftX - getCorrectionShift(), next.myTopY - 1,
                        next.getRightX() - getCorrectionShift() - 2, next.getBottomY());
                g.drawLine(next.myLeftX, next.myTopY - 1, next.myLeftX, next.getBottomY());
            }
            protected int getCorrectionShift() {
                return -1;
            }
        };

        myTaskEndRectanglePainter = new TaskRectanglePainter() {
            protected void drawBorder(Graphics g, Rectangle next) {
                super.drawBorder(g, next.myLeftX-getCorrectionShift() + 1, next.myTopY - 1, 
                        next.getRightX()-getCorrectionShift() - 1, next.getBottomY());
                g.drawLine(next.getRightX() - 1, next.myTopY - 1, next.getRightX() - 1, next.getBottomY());
            }
            protected int getCorrectionShift() {
                return 1;
            }
        };

        myTaskStartEndRectanglePainter = new TaskRectanglePainter() {
            protected void drawBorder(Graphics g, Rectangle next) {
                super.drawBorder(g, next);
                g.drawLine(next.myLeftX, next.myTopY - 1, next.myLeftX, next.getBottomY());
                g.drawLine(next.getRightX(), next.myTopY - 1, next.getRightX(), next.getBottomY());
            }
        };

        myTaskHolidayRectanglePainter = new RectanglePainter() {
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
                Graphics2D g = (Graphics2D) myGraphics;
                g.setColor(task.getColor());            
                Composite was = g.getComposite();
                g.setComposite(myAlphaComposite);
                g.fillRect(next.myLeftX, next.myTopY, next.myWidth, next.myHeight);
                g.setColor(Color.black);
                g.drawLine(next.myLeftX, next.myTopY, next.getRightX(), next.myTopY);
                g.drawLine(next.myLeftX, next.getBottomY() + 1, next.getRightX(), next.getBottomY());
                
                g.setComposite(was);
            }
        };

        // Update with new references
        myStyle2painter.put("task", myTaskRectanglePainter);
        myStyle2painter.put("task.start", myTaskStartRectanglePainter);
        myStyle2painter.put("task.end", myTaskEndRectanglePainter);
        myStyle2painter.put("task.startend", myTaskStartEndRectanglePainter);
        myStyle2painter.put("task.holiday", myTaskHolidayRectanglePainter);
    }
}
