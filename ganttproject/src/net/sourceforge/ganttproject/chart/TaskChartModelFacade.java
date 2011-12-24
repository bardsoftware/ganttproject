package net.sourceforge.ganttproject.chart;

import java.util.List;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Rectangle;
import net.sourceforge.ganttproject.task.Task;

public interface TaskChartModelFacade {
    List<Rectangle> getTaskRectangles(Task t);
}
