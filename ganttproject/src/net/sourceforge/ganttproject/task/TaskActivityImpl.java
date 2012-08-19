/*
 * Created on 18.10.2004
 */
package net.sourceforge.ganttproject.task;

import java.util.Date;

import biz.ganttproject.core.time.TimeDuration;


/**
 * @author bard
 */
class TaskActivityImpl implements TaskActivity {

  private final Date myEndDate;

  private final Date myStartDate;

  private final TimeDuration myDuration;

  private float myIntensity;

  private final Task myTask;

  TaskActivityImpl(Task task, Date startDate, Date endDate) {
    this(task, startDate, endDate, 1.0f);
  }

  TaskActivityImpl(Task task, Date startDate, Date endDate, float intensity) {
    myStartDate = startDate;
    myEndDate = endDate;
    myDuration = task.getManager().createLength(task.getDuration().getTimeUnit(), startDate, endDate);
    myIntensity = intensity;
    myTask = task;
  }

  @Override
  public Date getStart() {
    return myStartDate;
  }

  @Override
  public Date getEnd() {
    return myEndDate;
  }

  @Override
  public TimeDuration getDuration() {
    return myDuration;
  }

  @Override
  public float getIntensity() {
    return myIntensity;
  }

  @Override
  public String toString() {
    return myTask.toString() + "[" + getStart() + ", " + getEnd() + "]";
  }

  @Override
  public Task getTask() {
    return myTask;
  }

  @Override
  public boolean isFirst() {
    return this == getTask().getActivities()[0];
  }

  @Override
  public boolean isLast() {
    TaskActivity[] all = getTask().getActivities();
    return this == all[all.length - 1];
  }
}
