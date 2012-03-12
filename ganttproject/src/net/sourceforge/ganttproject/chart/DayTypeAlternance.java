/*
 * Created on 24.01.2005
 */
package net.sourceforge.ganttproject.chart;

import java.util.Date;

import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.task.TaskLength;

/**
 * @author bard
 */
public class DayTypeAlternance {

  private DayType myDayType;

  private TaskLength myDuration;

  private Date myEnd;

  DayTypeAlternance(DayType dayType, TaskLength duration, Date endDate) {
    myDayType = dayType;
    myDuration = duration;
    myEnd = endDate;
  }

  public Date getEnd() {
    return myEnd;
  }

  public DayType getDayType() {
    return myDayType;
  }

  public TaskLength getDuration() {
    return myDuration;
  }

  @Override
  public String toString() {
    return "period length=" + myDuration.getLength() + " (" + myDuration.getTimeUnit().getName() + ")" + " is"
        + (myDayType == DayType.WEEKEND ? " holiday" : " working\n");
  }
}
