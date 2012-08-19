/*
 * Created on 24.01.2005
 */
package net.sourceforge.ganttproject.chart;

import java.util.Date;

import biz.ganttproject.core.time.TimeDuration;

import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;

/**
 * @author bard
 */
public class DayTypeAlternance {

  private DayType myDayType;

  private TimeDuration myDuration;

  private Date myEnd;

  DayTypeAlternance(DayType dayType, TimeDuration duration, Date endDate) {
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

  public TimeDuration getDuration() {
    return myDuration;
  }

  @Override
  public String toString() {
    return "period length=" + myDuration.getLength() + " (" + myDuration.getTimeUnit().getName() + ")" + " is"
        + (myDayType == DayType.WEEKEND ? " holiday" : " working\n");
  }
}
