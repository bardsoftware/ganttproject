/*
 * Created on 21.11.2004
 */
package net.sourceforge.ganttproject.chart.item;

import net.sourceforge.ganttproject.task.Task;

/**
 * @author bard
 */
public class ChartItem {
  private final Task myTask;

  protected ChartItem(Task task) {
    myTask = task;
  }

  public Task getTask() {
    return myTask;
  }
}
