/*
 * Created on 18.10.2004
 */
package net.sourceforge.ganttproject.calendar;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.calendar.GPCalendar.MoveDirection;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
 */
public class AlwaysWorkingTimeCalendarImpl extends GPCalendarBase implements
        GPCalendar {
    public List<CalendarActivityImpl> getActivities(Date startDate, Date endDate) {
        return Collections.singletonList(new CalendarActivityImpl(startDate,
                endDate, true));
    }

    protected List<GPCalendarActivity> getActivitiesForward(Date startDate, TimeUnit timeUnit,
            long l) {
        Date activityStart = timeUnit.adjustLeft(startDate);
        Date activityEnd = activityStart;
        for (; l > 0; l--) {
            activityEnd = timeUnit.adjustRight(activityEnd);
        }
        return Collections.singletonList((GPCalendarActivity)new CalendarActivityImpl(
                activityStart, activityEnd, true));
    }

    protected List<CalendarActivityImpl> getActivitiesBackward(Date startDate, TimeUnit timeUnit,
            long unitCount) {
        Date activityEnd = timeUnit.adjustLeft(startDate);
        Date activityStart = activityEnd;
        while (unitCount-- > 0) {
            activityStart = timeUnit.jumpLeft(activityStart);
        }
        return Collections.singletonList(new CalendarActivityImpl(
                activityStart, activityEnd, true));
    }

    public void setWeekDayType(int day, DayType type) {
        if (type == GPCalendar.DayType.WEEKEND) {
            throw new IllegalArgumentException(
                    "I am always working time calendar, I don't accept holidays!");
        }
    }

    public DayType getWeekDayType(int day) {
        return GPCalendar.DayType.WORKING;
    }

    public Date findClosestWorkingTime(Date time) {
        return time;
    }

    public void setPublicHoliDayType(int month, int date) {
        // TODO Auto-generated method stub

    }

    public boolean isPublicHoliDay(Date curDayStart) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isNonWorkingDay(Date curDayStart) {
        // TODO Auto-generated method stub
        return false;
    }

    public DayType getDayTypeDate(Date curDayStart) {
        return GPCalendar.DayType.WORKING;
    }

    public boolean getOnlyShowWeekends() {
        // Weekends are always working days for this calendar
        return true;
    }

    public void setOnlyShowWeekends(boolean onlyShowWeekends) {
        // Ignore onlyShowWeekends, since weekends are always
        // working days for this calendar
    }

    public void setPublicHoliDayType(Date curDayStart) {
        // TODO Auto-generated method stub

    }

    public void setPublicHolidays(URL calendar, GanttProject gp) {
        // TODO Auto-generated method stub

    }

    public Collection<Date> getPublicHolidays() {
        // TODO Auto-generated method stub
        return null;
    }

    public List getActivities(Date startingFrom, TaskLength period) {
        return getActivities(startingFrom, period.getTimeUnit(), period
                .getLength());
    }
}
