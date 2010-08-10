package net.sourceforge.ganttproject.chart;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.chart.ChartModelBase.OptionEventDispatcher;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * @author dbarashev
 */
public interface ChartModel {
    ChartHeader getChartHeader();

    void setBounds(Dimension bounds);

    Dimension getBounds();
    Dimension getMaxBounds();
    void setStartDate(Date startDate);

    /**
     * This method calculates the end date of this chart. It is a function of
     * (start date, bounds, bottom time unit, top time unit, bottom unit width)
     * so it expects that all these parameters are set correctly.
     */
    Date getEndDate();

    Date getStartDate();

    void setBottomUnitWidth(int pixelsWidth);
    int getBottomUnitWidth();
    void setRowHeight(int rowHeight);

    void setTopTimeUnit(TimeUnit topTimeUnit);

    void setBottomTimeUnit(TimeUnit bottomTimeUnit);
    public TimeUnit getBottomUnit();

    void setVisibleTasks(java.util.List/* <Task> */visibleTasks);

    void paint(Graphics g);

    void setTaskContainment(TaskContainmentHierarchyFacade taskContainment);


    Rectangle getBoundingRectangle(Task task);

    float calculateLength(int fromX, int toX, int y);

    void setVerticalOffset(int i);

    ChartUIConfiguration getChartUIConfiguration();

    void addRenderer(ChartRendererBase renderer);

    List<ChartModelBase.Offset> getTopUnitOffsets();
    List<ChartModelBase.Offset> getBottomUnitOffsets();
    List<Offset> getDefaultUnitOffsets();
    List<Offset> getDefaultUnitOffsetsInRange(Offset startOffset, Offset endOffset);
    Date getDateAt(int x);
    TaskManager getTaskManager();
    TimeUnitStack getTimeUnitStack();
    OptionEventDispatcher getOptionEventDispatcher();
}