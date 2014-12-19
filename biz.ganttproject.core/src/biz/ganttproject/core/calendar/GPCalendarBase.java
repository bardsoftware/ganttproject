/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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
package biz.ganttproject.core.calendar;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import biz.ganttproject.core.time.DateFrameable;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.TimeUnit;


/**
 * @author bard
 */
abstract class GPCalendarBase implements GPCalendarCalc {
  private final List<GPCalendarListener> myListeners = Lists.newArrayList();
  private String myName;
  private String myId;
  
  @Override
  public String getID() {
    return myId == null ? myName : myId;
  }
  
  @Override
  public String getName() {
    return myName;
  }
  
  @Override
  public void setName(String name) {
    myName = name;
  }

  @Override
  public void setID(String id) {
    myId = id;
  }

  public Date shiftDate(Date input, TimeDuration shift) {
    if (shift.getLength() == 0) {
      return input;
    }
    List<GPCalendarActivity> activities = getActivities(input, shift);
    if (activities.isEmpty()) {
      throw new RuntimeException("FIXME: Failed to compute calendar activities in time period=" + shift
          + " starting from " + input);
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
    return unitCount > 0 ? getActivitiesForward(startDate, timeUnit, unitCount) : getActivitiesBackward(startDate,
        timeUnit, -unitCount);
  }

  protected abstract List<GPCalendarActivity> getActivitiesBackward(Date startDate, TimeUnit timeUnit, long unitCount);

  protected abstract List<GPCalendarActivity> getActivitiesForward(Date startDate, TimeUnit timeUnit, long unitCount);

  public List<GPCalendarActivity> getActivities(Date startingFrom, TimeDuration period) {
    return getActivities(startingFrom, period.getTimeUnit(), period.getLength());
  }

  public Date findClosest(Date time, TimeUnit timeUnit, MoveDirection direction, DayType dayType) {
    return findClosest(time, timeUnit, direction, dayType, null);
  }

  public Date findClosest(Date time, TimeUnit timeUnit, MoveDirection direction, DayType dayType, Date limit) {
    return doFindClosest(time, timeUnit, direction, dayType, limit);
  }
  
  protected Date doFindClosest(Date time, DateFrameable framer, MoveDirection direction, DayType dayType, Date limit) {
    Date nextUnitStart = direction == GPCalendarCalc.MoveDirection.FORWARD ? framer.adjustRight(time)
        : framer.jumpLeft(time);
    int nextUnitMask = getDayMask(nextUnitStart);
    switch (dayType) {
    case WORKING:
      if ((nextUnitMask & DayMask.WORKING) == DayMask.WORKING) {
        return nextUnitStart;
      }
      break;
    case WEEKEND:
    case HOLIDAY:
    case NON_WORKING:
      if ((nextUnitMask & DayMask.WORKING) == 0) {
        return nextUnitStart;
      }
      break;
    default:
      assert false : "Should not be here";
    }
    if (limit != null) {
      if (direction == MoveDirection.FORWARD && nextUnitStart.compareTo(limit) >= 0
          || direction == MoveDirection.BACKWARD && nextUnitStart.compareTo(limit) <= 0) {
        return null;
      }
    }
    return doFindClosest(nextUnitStart, framer, direction, dayType, limit);
  }

  
  @Override
  public void addListener(GPCalendarListener listener) {
    myListeners.add(listener);
  }

  protected void fireCalendarChanged() {
    for (GPCalendarListener l : myListeners) {
      try {
        l.onCalendarChange();
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
  }
  public abstract int getDayMask(Date date);
}
