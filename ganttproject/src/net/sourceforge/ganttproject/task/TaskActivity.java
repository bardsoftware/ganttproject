/*
 */
package net.sourceforge.ganttproject.task;

import java.util.Date;

/**
 * @author bard
 */
public interface TaskActivity {
  Date getStart();

  Date getEnd();

  TaskLength getDuration();

  float getIntensity();

  Task getTask();

  boolean isFirst();

  boolean isLast();
}
