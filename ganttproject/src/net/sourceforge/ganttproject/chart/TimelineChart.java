package net.sourceforge.ganttproject.chart;

import java.awt.Graphics;

import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;

public interface TimelineChart extends Chart {
    void setBottomUnitWidth(int width);

    void setTopUnit(TimeUnit topUnit);

    void setBottomUnit(TimeUnit bottomUnit);

    void paintChart(Graphics g);

    void addRenderer(ChartRendererBase renderer);
    void resetRenderers();

    /**
     * Scrolls the chart by a number of days
     *
     * @param days
     *            are the number of days to scroll. If days < 0 it scrolls to
     *            the right otherwise to the left.
     */
    public void scrollBy(TaskLength duration);

    ChartModel getModel();

    ChartUIConfiguration getStyle();

    void setStartOffset(int pixels);

}