/*
 * Created on 18.10.2004
 */
package net.sourceforge.ganttproject.chart;

import java.util.Date;
import java.util.List;

import com.google.common.base.Preconditions;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import biz.ganttproject.core.time.TimeDuration;


/**
 * @author bard
 */
class TaskActivityPart implements TaskActivity {

  private final Date myEndDate;

  private final Date myStartDate;

  private final TimeDuration myDuration;

  private final TaskActivity myOriginal;

  TaskActivityPart(TaskActivity original, Date startDate, Date endDate) {
    myStartDate = Preconditions.checkNotNull(startDate);
    myEndDate = Preconditions.checkNotNull(endDate);
    myOriginal = Preconditions.checkNotNull(original);
    Task task = original.getOwner();
    myDuration = task.getManager().createLength(task.getDuration().getTimeUnit(), startDate, endDate);
    Preconditions.checkState(myStartDate.compareTo(myOriginal.getStart()) >= 0);
    Preconditions.checkState(myEndDate.compareTo(myOriginal.getEnd()) <= 0);
    Preconditions.checkState(myEndDate.after(myStartDate));
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
    return myOriginal.getIntensity();
  }

  @Override
  public String toString() {
    return myOriginal.toString() + " part [" + getStart() + ", " + getEnd() + "]";
  }

  @Override
  public Task getOwner() {
    return myOriginal.getOwner();
  }

  @Override
  public boolean isFirst() {
    return myOriginal.isFirst() && myStartDate.equals(myOriginal.getStart());
  }

  @Override
  public boolean isLast() {
    return myOriginal.isLast() && myEndDate.equals(myOriginal.getEnd());
  }
}
