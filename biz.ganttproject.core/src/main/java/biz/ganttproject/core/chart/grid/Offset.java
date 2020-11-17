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
package biz.ganttproject.core.chart.grid;

import java.util.Date;
import java.util.List;

import biz.ganttproject.core.time.TimeUnit;


public class Offset {
  private final Date myOffsetAnchor;
  private final Date myOffsetEnd;
  private int myOffsetPixels;
  private final TimeUnit myOffsetUnit;
  private final int myDayMask;
  private final Date myOffsetStart;
  private int myStartPixels;

  public Offset(TimeUnit offsetUnit, Date offsetAnchor, Date offsetStart, Date offsetEnd, int startPixels, int offsetPixels,
      int dayMask) {
    myOffsetAnchor = offsetAnchor;
    myOffsetStart = offsetStart;
    myOffsetEnd = offsetEnd;
    myOffsetPixels = offsetPixels;
    myOffsetUnit = offsetUnit;
    myDayMask = dayMask;
    myStartPixels = startPixels;
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

  public int getStartPixels() {
    return myStartPixels;
  }
  public int getOffsetPixels() {
    return myOffsetPixels;
  }

  void shift(int pixels) {
    myStartPixels += pixels;
    myOffsetPixels += pixels;
  }

  public TimeUnit getOffsetUnit() {
    return myOffsetUnit;
  }

  public int getDayMask() {
    return myDayMask;
  }

  @Override
  public String toString() {
    return String.format("start: %s[%dpx] end: %s[%dpx] unit=%s", 
        myOffsetStart, myStartPixels, myOffsetEnd, myOffsetPixels, myOffsetUnit.getName());
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
      int startPixels, int endPixels, int dayMask) {
    return new Offset(timeUnit, anchor, closedStartDate, closedEndDate, startPixels, endPixels, dayMask);
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