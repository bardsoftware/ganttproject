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

    TaskLength calculateLength(int posX);

    ChartModel getModel();

    ChartUIConfiguration getStyle();
}