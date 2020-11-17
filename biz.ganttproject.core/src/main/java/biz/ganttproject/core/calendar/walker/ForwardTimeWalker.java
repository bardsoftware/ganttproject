/*
GanttProject is an opensource project management tool.
Copyright (C) 2010 Dmitry Barashev

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
package biz.ganttproject.core.calendar.walker;

import java.util.Date;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.GPCalendar.DayMask;
import biz.ganttproject.core.time.TimeUnit;


/**
 * Abstract iterator-like class for walking forward over the calendar timeline
 * doing steps of the specified size. It takes into account calendar working and
 * non-working time.
 * 
 * @author dbarashev (Dmitry Barashev)
 */
public abstract class ForwardTimeWalker {
  private final GPCalendarCalc myCalendar;
  private final TimeUnit myTimeUnit;

  protected ForwardTimeWalker(GPCalendarCalc calendar, TimeUnit timeUnit) {
    myCalendar = calendar;
    myTimeUnit = timeUnit;
  }

  protected TimeUnit getTimeUnit() {
    return myTimeUnit;
  }

  abstract protected boolean isMoving();

  public void walk(Date startDate) {
    Date unitStart = myTimeUnit.adjustLeft(startDate);
    while (isMoving()) {
      boolean isWeekendState = (myCalendar.getDayMask(unitStart) & DayMask.WORKING) == 0;
      if (isWeekendState) {
        Date workingUnitStart = myCalendar.findClosestWorkingTime(unitStart);
        assert workingUnitStart.after(unitStart);
        processNonWorkingTime(unitStart, workingUnitStart);
        unitStart = workingUnitStart;
        continue;
      } else {
        Date nextUnitStart = myTimeUnit.adjustRight(unitStart);
        processWorkingTime(unitStart, nextUnitStart);
        unitStart = nextUnitStart;
      }
    }
  }

  protected abstract void processWorkingTime(Date intervalStart, Date nextIntervalStart);

  protected abstract void processNonWorkingTime(Date intervalStart, Date workingIntervalStart);
}
