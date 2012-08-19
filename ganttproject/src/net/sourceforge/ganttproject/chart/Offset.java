/*
GanttProject is an opensource project management tool. License: GPL3
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
package net.sourceforge.ganttproject.chart;

import java.util.Date;
import java.util.List;

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.calendar.GPCalendar.DayType;
import biz.ganttproject.core.time.TimeUnit;


public class Offset {
  private Date myOffsetAnchor;
  private Date myOffsetEnd;
  private int myOffsetPixels;
  private TimeUnit myOffsetUnit;
  private GPCalendar.DayType myDayType;
  private Date myOffsetStart;

  public Offset(TimeUnit offsetUnit, Date offsetAnchor, Date offsetStart, Date offsetEnd, int offsetPixels,
      GPCalendar.DayType dayType) {
    myOffsetAnchor = offsetAnchor;
    myOffsetStart = offsetStart;
    myOffsetEnd = offsetEnd;
    myOffsetPixels = offsetPixels;
    myOffsetUnit = offsetUnit;
    myDayType = dayType;
  }

  Date getOffsetAnchor() {
    return myOffsetAnchor;
  }

  public Date getOffsetStart() {
    return myOffsetStart;
  }

  public Date getOffsetEnd() {
    return myOffsetEnd;
  }

  public int getOffsetPixels() {
    return myOffsetPixels;
  }

  void shift(int pixels) {
    myOffsetPixels += pixels;
  }

  TimeUnit getOffsetUnit() {
    return myOffsetUnit;
  }

  public DayType getDayType() {
    return myDayType;
  }

  @Override
  public String toString() {
    return "start date: " + myOffsetStart + " end date: " + myOffsetEnd + " end pixel: " + myOffsetPixels
        + " time unit: " + myOffsetUnit.getName();
  }

  @Override
  public boolean equals(Object that) {
    if (false == that instanceof Offset) {
      return false;
    }
    Offset thatOffset = (Offset) that;
    return myOffsetPixels == thatOffset.myOffsetPixels && myOffsetEnd.equals(thatOffset.myOffsetEnd)
        && myOffsetAnchor.equals(thatOffset.myOffsetAnchor);
  }

  @Override
  public int hashCode() {
    return myOffsetEnd.hashCode();
  }

  /**
   * @param concreteTimeUnit
   * @param startDate
   * @param currentDate
   * @param endDate
   * @param i
   * @param dayType
   * @return
   */
  public static Offset createFullyClosed(TimeUnit timeUnit, Date anchor, Date closedStartDate, Date closedEndDate,
      int pixels, DayType dayType) {
    return new Offset(timeUnit, anchor, closedStartDate, closedEndDate, pixels, dayType);
  }

  public static String debugPrint(List<Offset> offsets) {
    StringBuilder builder = new StringBuilder();
    builder.append("anchor=" + offsets.get(0).getOffsetAnchor());
    for (Offset offset : offsets) {
      builder.append(offset.toString()).append("\n");
    }
    return builder.toString();
  }
}