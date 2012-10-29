/*
 * Created on 21.11.2004
 */
package net.sourceforge.ganttproject.chart.item;

import net.sourceforge.ganttproject.task.Task;

/**
 * @author bard
 */
public class TaskBoundaryChartItem extends ChartItem {

  private final boolean isStart;

  public TaskBoundaryChartItem(Task task, boolean isStart) {
    super(task);
    this.isStart = isStart;
  }

  public boolean isStartBoundary() {
    return isStart;
  }
}
