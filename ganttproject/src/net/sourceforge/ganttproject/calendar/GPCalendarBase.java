/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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

import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.calendar.GPCalendar.MoveDirection;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.DateFrameable;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
 */
abstract class GPCalendarBase {
    public Date shiftDate(Date input, TaskLength shift) {
        List<GPCalendarActivity> activities = getActivities(input, shift);
        if (activities.isEmpty()) {
            throw new RuntimeException(
                    "FIXME: Failed to compute calendar activities in time period="
                            + shift + " starting from " + input);
        }
        Date result;
        if (shift.getValue() >= 0) {
            GPCalendarActivity lastActivity = activities.get(activities.size() - 1);
            result = lastActivity.getEnd();
        } else {
            GPCalendarActivity firstActivity = activities.get(0);
            result = firstActivity.getStart();
        }
        return result;
    }

    public List<GPCalendarActivity> getActivities(Date startDate, TimeUnit timeUnit, long unitCount) {
        return unitCount > 0 ? getActivitiesForward(startDate, timeUnit,
                unitCount) : getActivitiesBackward(startDate, timeUnit,
                -unitCount);
    }

    protected abstract List<GPCalendarActivity> getActivitiesBackward(Date startDate,
            TimeUnit timeUnit, long unitCount);

    protected abstract List<GPCalendarActivity> getActivitiesForward(Date startDate,
            TimeUnit timeUnit, long unitCount);

    public List<GPCalendarActivity> getActivities(Date startingFrom,
            TaskLength period) {
        return getActivities(startingFrom, period.getTimeUnit(), period
                .getLength());
    }

    public Date findClosest(Date time, TimeUnit timeUnit, MoveDirection direction, DayType dayType) {
        return findClosest(time, timeUnit, direction, dayType, null);
    }

    protected Date findClosest(Date time, DateFrameable framer, MoveDirection direction, DayType dayType, Date limit) {
        Date nextUnitStart = direction == GPCalendar.MoveDirection.FORWARD ?
                framer.adjustRight(time) : framer.jumpLeft(time);
        switch (dayType) {
        case WORKING:
            if (!isNonWorkingDay(nextUnitStart)) {
                return nextUnitStart;
            }
            break;
        case WEEKEND:
        case HOLIDAY:
        case NON_WORKING:
            if (isNonWorkingDay(nextUnitStart)) {
                return nextUnitStart;
            }
            break;
        }
        if (limit != null) {
            if (direction == GPCalendar.MoveDirection.FORWARD && nextUnitStart.compareTo(limit) >= 0
                    || direction == GPCalendar.MoveDirection.BACKWARD && nextUnitStart.compareTo(limit) <= 0) {
                return null;
            }
        }
        return findClosest(nextUnitStart, framer, direction, dayType, limit);
    }

    public abstract boolean isNonWorkingDay(Date date);
}
