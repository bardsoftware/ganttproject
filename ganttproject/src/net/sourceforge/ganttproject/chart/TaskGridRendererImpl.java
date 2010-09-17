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
public class TaskGridRendererImpl extends ChartRendererBase {
    public TaskGridRendererImpl(ChartModelImpl model) {
        super(model);
        getPrimitiveContainer().setOffset(0, model.getChartUIConfiguration().getHeaderHeight());
    }

    @Override
    public void render() {
        getPrimitiveContainer().clear();

        int rowHeight = getConfig().getRowHeight();
        int ypos = rowHeight;
        List/* <Task> */tasks = ((ChartModelImpl) getChartModel())
                .getVisibleTasks();
        for (int i = 0; i < tasks.size(); i++) {
            GraphicPrimitiveContainer.Line nextLine = getPrimitiveContainer()
                    .createLine(0, ypos,
                            (int) getChartModel().getBounds().getWidth(), ypos);
            nextLine.setForegroundColor(Color.GRAY);
            ypos += rowHeight;
        }
    }

    protected int getHeight() {
        return (int) getChartModel().getBounds().getHeight()
                - getConfig().getHeaderHeight();
    }
}
