/*
 * Created on 21.11.2004
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.util.List;

import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
 */
public class TaskGridRendererImpl extends ChartRendererBase implements
        TimeUnitVisitor {
    public TaskGridRendererImpl(ChartModelImpl model) {
        super(model);
    }

    public void beforeProcessingTimeFrames() {
        getPrimitiveContainer().clear();

        // GraphicPrimitiveContainer.Rectangle r =
        // getPrimitiveContainer().createRectangle(0, 0, getWidth(),
        // getHeight());
        // r.setBackgroundColor(Color.WHITE);
        // System.err.println("background rect="+r);
        int rowHeight = getConfig().getRowHeight();
        int ypos = rowHeight;
        List/* <Task> */tasks = ((ChartModelImpl) getChartModel())
                .getVisibleTasks();
        //boolean isLightLine = true;
        for (int i = 0; i < tasks.size(); i++) {
            // GraphicPrimitiveContainer.Rectangle nextRect =
            // getPrimitiveContainer().createRectangle(0, ypos,
            // (int)getChartModel().getBounds().getWidth(), rowHeight-1);

            // nextRect.setStyle(isLightLine ? "grid.light" : "grid.dark");
            GraphicPrimitiveContainer.Line nextLine = getPrimitiveContainer()
                    .createLine(0, ypos,
                            (int) getChartModel().getBounds().getWidth(), ypos);
            nextLine.setForegroundColor(Color.GRAY);
            //isLightLine = !isLightLine;
            ypos += rowHeight;
        }
    }

    protected int getHeight() {
        return (int) getChartModel().getBounds().getHeight()
                - getConfig().getHeaderHeight();
    }

    public void afterProcessingTimeFrames() {
    }

    public void startTimeFrame(TimeFrame timeFrame) {
    }

    public void endTimeFrame(TimeFrame timeFrame) {
    }

    public void startUnitLine(TimeUnit timeUnit) {
    }

    public void endUnitLine(TimeUnit timeUnit) {
    }

    public void nextTimeUnit(int unitIndex) {
    }

}
