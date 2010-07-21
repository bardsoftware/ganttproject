package net.sourceforge.ganttproject.chart;

import java.awt.Graphics;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface Painter {
    void paint(GraphicPrimitiveContainer.Rectangle rectangle);

    void paint(Text next);
    
    void setGraphics(Graphics g);
}
