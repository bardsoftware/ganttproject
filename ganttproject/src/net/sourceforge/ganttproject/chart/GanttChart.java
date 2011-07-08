package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.GanttPreviousState;

public interface GanttChart extends TimelineChart {
    void setBaseline(GanttPreviousState ganttPreviousState);

    GanttPreviousState getBaseline();
}