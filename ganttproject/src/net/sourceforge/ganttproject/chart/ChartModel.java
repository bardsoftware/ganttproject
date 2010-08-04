package net.sourceforge.ganttproject.chart;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Date;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author dbarashev
 */
public interface ChartModel {
    ChartHeader getChartHeader();

    void setBounds(Dimension bounds);

    void setStartDate(Date startDate);

    /**
     * This method calculates the end date of this chart. It is a function of
     * (start date, bounds, bottom time unit, top time unit, bottom unit width)
     * so it expects that all these parameters are set correctly.
     */
    Date getEndDate();

    Date getStartDate();

    void setBottomUnitWidth(int pixelsWidth);

    void setRowHeight(int rowHeight);

    void setTopTimeUnit(TimeUnit topTimeUnit);

    void setBottomTimeUnit(TimeUnit bottomTimeUnit);

    void setVisibleTasks(java.util.List/* <Task> */visibleTasks);

    void paint(Graphics g);

    void setTaskContainment(TaskContainmentHierarchyFacade taskContainment);

    Task findTaskWithCoordinates(int x, int y);

    Rectangle getBoundingRectangle(Task task);

    float calculateLength(int fromX, int toX, int y);

    void setVerticalOffset(int i);
}
