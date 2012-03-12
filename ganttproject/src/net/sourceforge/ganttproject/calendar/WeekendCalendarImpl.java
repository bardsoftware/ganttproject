/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.calendar;

import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.calendar.walker.ForwardTimeWalker;
import net.sourceforge.ganttproject.parser.HolidayTagHandler;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.gregorian.FramerImpl;

/**
 * @author bard
 */
public class WeekendCalendarImpl extends GPCalendarBase implements GPCalendar {

  private final Calendar myCalendar = CalendarFactory.newCalendar();

  private final FramerImpl myFramer = new FramerImpl(Calendar.DAY_OF_WEEK);

  private DayType[] myTypes = new DayType[7];

  private boolean myOnlyShowWeekends = false;

  private int myWeekendDaysCount;

  private Set<Date> publicHolidaysArray = new LinkedHashSet<Date>();

  private final Set<Date> myStableHolidays = new LinkedHashSet<Date>();

  private AlwaysWorkingTimeCalendarImpl myRestlessCalendar = new AlwaysWorkingTimeCalendarImpl();

  private URL myCalendarUrl;

  public WeekendCalendarImpl() {
    for (int i = 0; i < myTypes.length; i++) {
      myTypes[i] = GPCalendar.DayType.WORKING;
    }
    setWeekDayType(GregorianCalendar.SATURDAY, GPCalendar.DayType.WEEKEND);
    setWeekDayType(GregorianCalendar.SUNDAY, GPCalendar.DayType.WEEKEND);
  }

  @Override
  public List<GPCalendarActivity> getActivities(Date startDate, final Date endDate) {
    if (getWeekendDaysCount() == 0 && publicHolidaysArray.isEmpty() && myStableHolidays.isEmpty()) {
      return myRestlessCalendar.getActivities(startDate, endDate);
    }
    List<GPCalendarActivity> result = new ArrayList<GPCalendarActivity>();
    Date curDayStart = myFramer.adjustLeft(startDate);
    boolean isWeekendState = isNonWorkingDay(curDayStart);
    while (curDayStart.before(endDate)) {
      Date changeStateDayStart = findClosest(curDayStart, myFramer, MoveDirection.FORWARD,
          isWeekendState ? DayType.WORKING : DayType.NON_WORKING, endDate);
      if (changeStateDayStart == null) {
        changeStateDayStart = endDate;
      }
      if (changeStateDayStart.before(endDate) == false) {
        result.add(new CalendarActivityImpl(curDayStart, endDate, !isWeekendState));
        break;
      }
      result.add(new CalendarActivityImpl(curDayStart, changeStateDayStart, !isWeekendState));
      curDayStart = changeStateDayStart;
      isWeekendState = !isWeekendState;
    }
    return result;
  }

  public boolean isWeekend(Date curDayStart) {
    if (myOnlyShowWeekends) {
      return false;
    }

    myCalendar.setTime(curDayStart);
    int dayOfWeek = myCalendar.get(Calendar.DAY_OF_WEEK);
    return myTypes[dayOfWeek - 1] == GPCalendar.DayType.WEEKEND;
  }

  @Override
  protected List<GPCalendarActivity> getActivitiesForward(Date startDate, TimeUnit timeUnit, final long unitCount) {
    final List<GPCalendarActivity> result = new ArrayList<GPCalendarActivity>();
    new ForwardTimeWalker(this, timeUnit) {
      long myUnitCount = unitCount;

      @Override
      protected void processWorkingTime(Date intervalStart, Date nextIntervalStart) {
        result.add(new CalendarActivityImpl(intervalStart, nextIntervalStart, true));
        myUnitCount--;
      }

      @Override
      protected void processNonWorkingTime(Date intervalStart, Date workingIntervalStart) {
        result.add(new CalendarActivityImpl(intervalStart, workingIntervalStart, false));
      }

      @Override
      protected boolean isMoving() {
        return myUnitCount > 0;
      }
    }.walk(startDate);
    return result;
  }

  @Override
  protected List<GPCalendarActivity> getActivitiesBackward(Date startDate, TimeUnit timeUnit, long unitCount) {
    List<GPCalendarActivity> result = new LinkedList<GPCalendarActivity>();
    Date unitStart = timeUnit.adjustLeft(startDate);
    while (unitCount > 0) {
      Date prevUnitStart = timeUnit.jumpLeft(unitStart);
      boolean isWeekendState = isNonWorkingDay(prevUnitStart);
      if (isWeekendState) {
        Date lastWorkingUnitStart = findClosest(prevUnitStart, timeUnit, MoveDirection.BACKWARD, DayType.WORKING);
        Date firstWeekendUnitStart = timeUnit.adjustRight(lastWorkingUnitStart);
        Date lastWeekendUnitEnd = unitStart;
        result.add(0, new CalendarActivityImpl(firstWeekendUnitStart, lastWeekendUnitEnd, false));
        unitStart = firstWeekendUnitStart;
      } else {
        result.add(0, new CalendarActivityImpl(prevUnitStart, unitStart, true));
        unitCount--;
        unitStart = prevUnitStart;
      }
    }
    return result;
  }

  @Override
  public void setWeekDayType(int day, DayType type) {
    if (type != myTypes[day - 1]) {
      myWeekendDaysCount += (type == DayType.WEEKEND ? 1 : -1);
    }
    myTypes[day - 1] = type;
  }

  @Override
  public DayType getWeekDayType(int day) {
    return myTypes[day - 1];
  }

  @Override
  public boolean getOnlyShowWeekends() {
    return myOnlyShowWeekends;
  }

  @Override
  public void setOnlyShowWeekends(boolean onlyShowWeekends) {
    myOnlyShowWeekends = onlyShowWeekends;
  }

  private int getWeekendDaysCount() {
    return myOnlyShowWeekends ? 0 : myWeekendDaysCount;
  }

  @Override
  public Date findClosestWorkingTime(Date time) {
    if (getWeekendDaysCount() == 0 && myStableHolidays.isEmpty() && publicHolidaysArray.isEmpty()) {
      return time;
    }
    if (!isNonWorkingDay(time)) {
      return time;
    }
    return findClosest(time, myFramer, MoveDirection.FORWARD, DayType.WORKING, null);
  }

  @Override
  public void setPublicHoliDayType(int month, int date) {
    setPublicHoliDayType(new GanttCalendar(1, month - 1, date).getTime());
    myStableHolidays.add(new GanttCalendar(1, month - 1, date).getTime());
  }

  @Override
  public void setPublicHoliDayType(Date curDayStart) {
    publicHolidaysArray.add(curDayStart);
  }

  @Override
  public boolean isPublicHoliDay(Date curDayStart) {
    boolean result = publicHolidaysArray.contains(curDayStart);
    if (!result) {
      result = myStableHolidays.contains(new GanttCalendar(1, curDayStart.getMonth(), curDayStart.getDate()).getTime());
    }
    return result;
  }

  @Override
  public DayType getDayTypeDate(Date curDayStart) {
    myCalendar.setTime(curDayStart);
    int dayOfWeek = myCalendar.get(Calendar.DAY_OF_WEEK);
    if (isPublicHoliDay(curDayStart))
      return GPCalendar.DayType.HOLIDAY;
    else if (getWeekDayType(dayOfWeek) == GPCalendar.DayType.WORKING)
      return GPCalendar.DayType.WORKING;
    else
      return GPCalendar.DayType.WEEKEND;
  }

  @Override
  public boolean isNonWorkingDay(Date curDayStart) {
    return isWeekend(curDayStart) || isPublicHoliDay(curDayStart);
  }

  @Override
  public void setPublicHolidays(URL calendarUrl) {
    myCalendarUrl = calendarUrl;
    clearPublicHolidays();
    if (calendarUrl != null) {
      XMLCalendarOpen opener = new XMLCalendarOpen();

      HolidayTagHandler tagHandler = new HolidayTagHandler(this);

      opener.addTagHandler(tagHandler);
      opener.addParsingListener(tagHandler);
      try {
        opener.load(calendarUrl.openStream());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public Collection<Date> getPublicHolidays() {
    return Collections.unmodifiableCollection(publicHolidaysArray);
  }

  @Override
  public void clearPublicHolidays() {
    publicHolidaysArray.clear();
  }

  @Override
  public List<GPCalendarActivity> getActivities(Date startingFrom, TaskLength period) {
    return getActivities(startingFrom, period.getTimeUnit(), period.getLength());
  }

  @Override
  public GPCalendar copy() {
    WeekendCalendarImpl result = new WeekendCalendarImpl();
    for (int i = 1; i < 8; i++) {
      result.setWeekDayType(i, getWeekDayType(i));
    }
    result.setOnlyShowWeekends(getOnlyShowWeekends());
    result.setPublicHolidays(myCalendarUrl);
    result.publicHolidaysArray.addAll(publicHolidaysArray);
    return result;
  }

  @Override
  public URL getPublicHolidaysUrl() {
    return myCalendarUrl;
  }
}
