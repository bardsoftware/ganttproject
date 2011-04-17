package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Line;
import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface Painter {
    void paint(GraphicPrimitiveContainer.Rectangle rectangle);

    void paint(Line line);
    
    void paint(Text next);
}
