/*
 * Created on 18.10.2004
 */
package biz.ganttproject.core.calendar;

import java.util.Date;

/**
 * @author bard
 */
public class CalendarActivityImpl implements GPCalendarActivity {

  private final boolean isWorkingTime;

  private final Date myEndDate;

  private final Date myStartDate;

  public CalendarActivityImpl(Date startDate, Date endDate, boolean isWorkingTime) {
    myStartDate = startDate;
    myEndDate = endDate;
    this.isWorkingTime = isWorkingTime;
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
  public boolean isWorkingTime() {
    return isWorkingTime;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return (isWorkingTime() ? "Working time: " : "Holiday: ") + "[" + getStart() + ", " + getEnd() + "]";
  }
}
