package biz.ganttproject.core.calendar;

import java.util.Date;
import java.util.List;

import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;

public interface GPCalendarCalc extends GPCalendar {
  public static enum MoveDirection {
    FORWARD, BACKWARD
  }

  List<GPCalendarActivity> getActivities(Date startDate, Date endDate);

  List<GPCalendarActivity> getActivities(Date startDate, TimeUnit timeUnit, long l);

  /**
   * @return true when weekends are only shown and taken into account for the
   *         task scheduling.
   */
  public boolean getOnlyShowWeekends();

  /**
   * @param onlyShowWeekends
   *          must be set to true if weekends are only shown and not taken into
   *          account for the task scheduling
   */
  public void setOnlyShowWeekends(boolean onlyShowWeekends);

  /**
   * @return true when public holidays are working days. Weekends stay non-working.
   */
  public boolean getHolidaysAreWorkingDays();

  /**
   * @param holidaysAreWorkingDays
   *          must be set to true if public holidays are taken into account for the
   *          task scheduling like any other working day
   */
  public void setHolidaysAreWorkingDays(boolean holidaysAreWorkingDays);

  Date findClosestWorkingTime(Date time);

  /**
   * Adds <code>shift</code> period to <code>input</code> date taking into
   * account this calendar working/non-working time If input date corresponds to
   * Friday midnight and this calendar if configured to have a weekend on
   * Saturday and Sunday then adding a shift of "1 day" will result to the
   * midnight of the next Monday
   */
  Date shiftDate(Date input, TimeDuration shift);

  Date findClosest(Date time, TimeUnit timeUnit, MoveDirection direction, DayType dayType);

  Date findClosest(Date time, TimeUnit timeUnit, MoveDirection direction, DayType dayType, Date limit);
  GPCalendarCalc PLAIN = new AlwaysWorkingTimeCalendarImpl();
  String EXTENSION_POINT_ID = "net.sourceforge.ganttproject.calendar";

  public GPCalendarCalc copy();
}
