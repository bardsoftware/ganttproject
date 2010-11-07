/*
 * Created on 21.11.2004
 */
package net.sourceforge.ganttproject.chart.item;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
 */
public class TaskProgressChartItem extends ChartItem {

    private int myPosX;

    private int myUnitWidth;

    // TODO Field is never read... remove?
    private TimeUnit myTimeUnit;

    private float myTaskLength;

    public TaskProgressChartItem(int posX, int unitWidth, TimeUnit bottomUnit,
            Task task) {
        super(task);
        myPosX = posX;
        myUnitWidth = unitWidth;
        myTimeUnit = bottomUnit;
        myTaskLength = task.getDuration().getLength(bottomUnit);
    }

    public float getProgressDelta(int currentX) {
        int deltaX = currentX - myPosX;
        float deltaUnits = (float) deltaX / (float) myUnitWidth;
        return 100 * deltaUnits / myTaskLength;
    }

}
