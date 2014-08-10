/*
 */
package net.sourceforge.ganttproject.task;

import biz.ganttproject.core.chart.scene.BarChartActivity;

/**
 * @author bard
 */
public interface TaskActivity extends BarChartActivity<Task> {
  float getIntensity();

  boolean isFirst();

  boolean isLast();
}
