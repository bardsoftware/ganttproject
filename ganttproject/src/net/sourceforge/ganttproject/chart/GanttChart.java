package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.task.Task;

public interface GanttChart extends TimelineChart {

    void editTaskAsNew(Task task);

}