/*
Copyright 2004-2013 BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package biz.ganttproject.core.calendar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import biz.ganttproject.core.calendar.CalendarEvent.Type;
import biz.ganttproject.core.calendar.walker.ForwardTimeWalker;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.impl.FramerImpl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Implements a calendar which is aware of weekend days and recurring/one-off holidays and working days.
 * 
 * By default, this calendar assumes that any day D is a working day. Then it applies weekend check,
 * recurring event check and one-off event check in this specified order. Each check can override the result of the previous
 * one. For instance, if D is a weekend day then it is a holiday, unless one of the following is the case:
 * -- only show weekends option is on
 * -- there is one-off event at date D with type WORKING
 * -- there is a recurring event at date D with type WORKING and no one-off event at date D with type HOLIDAY
 * 
 * If D is a non-weekend day then it is a working day, unless one of the following is the case:
 * -- there is one-off event at date D with type HOLIDAY
 * -- there is a recurring event at date D with type HOLIDAY and no one-off event at date D with type WORKING
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public class WeekendCalendarImpl extends GPCalendarBase implements GPCalendarCalc {

  private final Calendar myCalendar = CalendarFactory.newCalendar();

  private final FramerImpl myFramer = new FramerImpl(Calendar.DAY_OF_WEEK);

  private final DayType[] myTypes = new DayType[7];

  private boolean myOnlyShowWeekends = false;

  private int myWeekendDaysCount;

  private final Map<Date, CalendarEvent> myRecurringEvents = Maps.newLinkedHashMap();
  private final Map<Date, CalendarEvent> myOneOffEvents = Maps.newLinkedHashMap();

  private final AlwaysWorkingTimeCalendarImpl myRestlessCalendar = new AlwaysWorkingTimeCalendarImpl();

  private String myBaseCalendarID;

  public WeekendCalendarImpl() {
    this(null);
  }
  
  public WeekendCalendarImpl(String baseCalendarID) {
    myBaseCalendarID = baseCalendarID;
    reset();
  }

  public void reset() {
    myRecurringEvents.clear();
    myOneOffEvents.clear();
    for (int i = 0; i < myTypes.length; i++) {
      myTypes[i] = GPCalendar.DayType.WORKING;
    }
    setWeekDayType(GregorianCalendar.SATURDAY, GPCalendar.DayType.WEEKEND);
    setWeekDayType(GregorianCalendar.SUNDAY, GPCalendar.DayType.WEEKEND);
    fireCalendarChanged();
  }
  
  @Override
  public List<GPCalendarActivity> getActivities(Date startDate, final Date endDate) {
    if (getWeekendDaysCount() == 0 && myOneOffEvents.isEmpty() && myRecurringEvents.isEmpty()) {
      return myRestlessCalendar.getActivities(startDate, endDate);
    }
    List<GPCalendarActivity> result = new ArrayList<GPCalendarActivity>();
    Date curDayStart = myFramer.adjustLeft(startDate);
    boolean isWeekendState = (getDayMask(curDayStart) & DayMask.WORKING) == 0;
    while (curDayStart.before(endDate)) {
      Date changeStateDayStart = doFindClosest(curDayStart, myFramer, MoveDirection.FORWARD,
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
      boolean isWeekendState = (getDayMask(prevUnitStart) & DayMask.WORKING) == 0;
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
    fireCalendarChanged();
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
    fireCalendarChanged();
  }

  private int getWeekendDaysCount() {
    return myOnlyShowWeekends ? 0 : myWeekendDaysCount;
  }

  @Override
  public Date findClosestWorkingTime(Date time) {
    if (getWeekendDaysCount() == 0 && myRecurringEvents.isEmpty() && myOneOffEvents.isEmpty()) {
      return time;
    }
    int dayMask = getDayMask(time);
    if ((dayMask & DayMask.WORKING) == DayMask.WORKING) {
      return time;
    }
    return doFindClosest(time, myFramer, MoveDirection.FORWARD, DayType.WORKING, null);
  }

  private boolean isPublicHoliDay(Date curDayStart) {
    CalendarEvent oneOff = myOneOffEvents.get(curDayStart);
    if (oneOff != null) {
      switch (oneOff.getType()) {
      case HOLIDAY:
        return true;
      case WORKING_DAY:
        return false;
      case NEUTRAL:
      default:
        // intentionally fall-through, consult recurring holidays in this case 
      }
    }
    CalendarEvent recurring = myRecurringEvents.get(getRecurringDate(curDayStart));
    if (recurring != null) {
      switch (recurring.getType()) {
      case HOLIDAY:
        return true;
      case WORKING_DAY:
        return false;
      case NEUTRAL:
      default:
        // intentionally fall-through, use default answer 
      }            
    }
    return false;
  }

  public CalendarEvent getEvent(Date date) {
    CalendarEvent result = myOneOffEvents.get(date);
    if (result == null) {
      result = myRecurringEvents.get(getRecurringDate(date));
    }
    return result;
  }
  
  private Date getRecurringDate(Date date) {
    myCalendar.setTime(date);
    myCalendar.set(Calendar.YEAR, 1);
    return myCalendar.getTime();
  }
  @Override
  public int getDayMask(Date date) {
    int result = 0;
    myCalendar.setTime(date);
    int dayOfWeek = myCalendar.get(Calendar.DAY_OF_WEEK);
    boolean isHoliday = isPublicHoliDay(date);
    boolean isWeekend = myTypes[dayOfWeek - 1] == DayType.WEEKEND;
    if (isWeekend) {
      result |= DayMask.WEEKEND;
      CalendarEvent oneOff = myOneOffEvents.get(date);
      if (oneOff != null && oneOff.getType() == Type.WORKING_DAY) {
        result |= DayMask.WORKING;
      }
    }
    if (isHoliday) {
      result |= DayMask.HOLIDAY;
      return result;
    }
    if (!isWeekend || myOnlyShowWeekends) {
      result |= DayMask.WORKING;
    }
    return result;
  }

//  @Override
//  public boolean isNonWorkingDay(Date curDayStart) {
//    return isWeekend(curDayStart) || isPublicHoliDay(curDayStart);
//  }

  @Override
  public void setPublicHolidays(Collection<CalendarEvent> holidays) {
    myRecurringEvents.clear();
    myOneOffEvents.clear();
    for (CalendarEvent h : holidays) {
      if (h.isRecurring) {
        myCalendar.setTime(h.myDate);
        myCalendar.set(Calendar.YEAR, 1);
        myRecurringEvents.put(myCalendar.getTime(), h);
      } else {
        myOneOffEvents.put(h.myDate, h);
      }
    }
    fireCalendarChanged();
//    myCalendarUrl = calendarUrl;
//    clearPublicHolidays();
//    if (calendarUrl != null) {
//      XMLCalendarOpen opener = new XMLCalendarOpen();
//
//      HolidayTagHandler tagHandler = new HolidayTagHandler(this);
//
//      opener.addTagHandler(tagHandler);
//      opener.addParsingListener(tagHandler);
//      try {
//        opener.load(calendarUrl.openStream());
//      } catch (Exception e) {
//        throw new RuntimeException(e);
//      }
//    }
  }

  @Override
  public Collection<CalendarEvent> getPublicHolidays() {
    List<CalendarEvent> result = Lists.newArrayListWithExpectedSize(myRecurringEvents.size() + myOneOffEvents.size());
    result.addAll(myRecurringEvents.values());
    result.addAll(myOneOffEvents.values());
    return result;
  }

  @Override
  public List<GPCalendarActivity> getActivities(Date startingFrom, TimeDuration period) {
    return getActivities(startingFrom, period.getTimeUnit(), period.getLength());
  }

  @Override
  public GPCalendarCalc copy() {
    WeekendCalendarImpl result = new WeekendCalendarImpl(myBaseCalendarID);
    for (int i = 1; i < 8; i++) {
      result.setWeekDayType(i, getWeekDayType(i));
    }
    result.setOnlyShowWeekends(getOnlyShowWeekends());
    result.setPublicHolidays(getPublicHolidays());
    //result.publicHolidaysArray.addAll(publicHolidaysArray);
    return result;
  }

  @Override
  public String getBaseCalendarID() {
    return myBaseCalendarID;
  }

  @Override
  public void setBaseCalendarID(String id) {
    myBaseCalendarID = id;
  }

  @Override
  public void importCalendar(GPCalendar calendar, ImportCalendarOption importOption) {
    if (ImportCalendarOption.Values.NO.equals(importOption.getSelectedValue())) {
      return;
    }
    if (ImportCalendarOption.Values.REPLACE.equals(importOption.getSelectedValue())) {
      reset();
      setPublicHolidays(calendar.getPublicHolidays());
      for (int i = 1; i <= 7; i++) {
        setWeekDayType(i, calendar.getWeekDayType(i));
      }
      return;
    }
    if (ImportCalendarOption.Values.MERGE.equals(importOption.getSelectedValue())) {
      LinkedHashSet<CalendarEvent> mergedHolidays = Sets.newLinkedHashSet(getPublicHolidays());
      mergedHolidays.addAll(calendar.getPublicHolidays());
      setPublicHolidays(mergedHolidays);      
    }
    for (int i = 1; i <= 7; i++) {
      if (calendar.getWeekDayType(i) == DayType.WEEKEND) {        
        setWeekDayType(i, DayType.WEEKEND);
      }
    }
  }
}
