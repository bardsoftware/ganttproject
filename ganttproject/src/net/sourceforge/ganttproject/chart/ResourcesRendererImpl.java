/*
 * Created on 03.12.2004
 */
package net.sourceforge.ganttproject.chart;

import java.util.List;

import net.sourceforge.ganttproject.chart.GraphicPrimitiveContainer.Text;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
 */
public class ResourcesRendererImpl extends ChartRendererBase implements
        TimeUnitVisitor {

    public ResourcesRendererImpl(ChartModelImpl model) {
        super(model);
        // TODO Auto-generated constructor stub
    }

    public void beforeProcessingTimeFrames() {
        // TODO Auto-generated method stub
    }

    public void afterProcessingTimeFrames() {
        List<Task> visibleTasks = ((ChartModelImpl) getChartModel())
                .getVisibleTasks();
        int bottomY = getConfig().getRowHeight();
        for (int i = 0; i < visibleTasks.size(); i++) {
            Task nextTask = visibleTasks.get(i);
            ResourceAssignment[] assignments = nextTask.getAssignments();
            if (assignments.length > 0) {
                StringBuffer resources = new StringBuffer();
                for (int j = 0; j < assignments.length; j++) {
                    resources.append(assignments[j].getResource().getName());
                    if (j < assignments.length - 1) {
                        resources.append(", ");
                    }
                }
                Text text = getPrimitiveContainer().createText(0, bottomY,
                        resources.toString());
                text.setStyle("task.resources");
            }
            bottomY += getConfig().getRowHeight();
        }
    }

    public void startTimeFrame(TimeFrame timeFrame) {
        // TODO Auto-generated method stub

    }

    public void endTimeFrame(TimeFrame timeFrame) {
        // TODO Auto-generated method stub

    }

    public void startUnitLine(TimeUnit timeUnit) {
        // TODO Auto-generated method stub

    }

    public void endUnitLine(TimeUnit timeUnit) {
        // TODO Auto-generated method stub

    }

    public void nextTimeUnit(int unitIndex) {
        // TODO Auto-generated method stub

    }

}
