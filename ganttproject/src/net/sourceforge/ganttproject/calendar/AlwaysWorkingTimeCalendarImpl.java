/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
 */
public class AlwaysWorkingTimeCalendarImpl extends GPCalendarBase implements
        GPCalendar {
    public List<GPCalendarActivity> getActivities(Date startDate, Date endDate) {
        return Collections.singletonList((GPCalendarActivity) new CalendarActivityImpl(startDate,
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

    protected List<GPCalendarActivity> getActivitiesBackward(Date startDate, TimeUnit timeUnit,
            long unitCount) {
        Date activityEnd = timeUnit.adjustLeft(startDate);
        Date activityStart = activityEnd;
        while (unitCount-- > 0) {
            activityStart = timeUnit.jumpLeft(activityStart);
        }
        return Collections.singletonList((GPCalendarActivity) new CalendarActivityImpl(
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
        // All days are working days for this calendar, so not public holidays
    }

    public boolean isPublicHoliDay(Date curDayStart) {
        // All days are working days for this calendar, so not public holidays
        return false;
    }

    public boolean isNonWorkingDay(Date curDayStart) {
        // All days are working days for this calendar
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
        // Nothing needs to be done, as the class does not have non working days
    }

    public void setPublicHolidays(URL calendar, IGanttProject gp) {
        // Nothing needs to be done, as the class does not have non working days
    }

    public Iterator<Date> getPublicHolidays() {
        // Nothing needs to be done, as the class does not have non working days
        return null;
    }
    
    public void clearPublicHolidays() {
        // Nothing needs to be done, as the class does not have non working days
    }

    public List<GPCalendarActivity> getActivities(Date startingFrom, TaskLength period) {
        return getActivities(startingFrom, period.getTimeUnit(), period
                .getLength());
    }
}
