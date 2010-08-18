package net.sourceforge.ganttproject.chart;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;

import net.sourceforge.ganttproject.GanttGraphicArea.ChartImplementation;
import net.sourceforge.ganttproject.task.Task;

public class RenderedGanttChartImage extends RenderedChartImage {
    private final List<Task> myVisibleTasks;
    private ChartImplementation myChartImplementation;
    
    public RenderedGanttChartImage(ChartModelBase chartModel, ChartImplementation chartImplementation, List<Task> tasks, BufferedImage taskImage, int chartWidth, int chartHeight) {
        super(chartModel, taskImage, chartWidth, chartHeight);
        myVisibleTasks = tasks;
        myChartImplementation = chartImplementation;
    }

    protected void paintChart(Graphics g) {
        if (myVisibleTasks.isEmpty()) {
            myChartImplementation.paintComponent(g);
        } else {
            myChartImplementation.paintComponent(g, myVisibleTasks);
        }        
    }

}
