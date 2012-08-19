/*
 */
package net.sourceforge.ganttproject.task;

import java.util.Date;

import net.sourceforge.ganttproject.time.TimeDuration;

/**
 * @author bard
 */
public interface TaskActivity {
  Date getStart();

  Date getEnd();

  TimeDuration getDuration();

  float getIntensity();

  Task getTask();

  boolean isFirst();

  boolean isLast();
}
