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

import com.google.common.base.Function;

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.TimeUnitStack;


public interface OffsetBuilder {
  public static abstract class Factory {
    protected TimeUnit myTopUnit;
    protected TimeUnit myBottomUnit;
    protected TimeUnit myBaseUnit;
    protected Date myStartDate;
    protected Date myEndDate;
    protected int myEndOffset;
    protected int myAtomicUnitWidth;
    protected float myWeekendDecreaseFactor;
    protected GPCalendar myCalendar;
    protected int myRightMarginTimeUnits;
    protected Date myViewportStartDate;
    protected Function<TimeUnit, Float> myOffsetStepFn;
    
    protected Factory() {
    }

    public Factory withTopUnit(TimeUnit topUnit) {
      myTopUnit = topUnit;
      return this;
    }

    public Factory withBottomUnit(TimeUnit bottomUnit) {
      myBottomUnit = bottomUnit;
      return this;
    }

    public Factory withStartDate(Date startDate) {
      myStartDate = startDate;
      return this;
    }

    public Factory withViewportStartDate(Date viewportStartDate) {
      myViewportStartDate = viewportStartDate;
      return this;
    }

    public Factory withEndDate(Date endDate) {
      myEndDate = endDate;
      return this;
    }

    public Factory withEndOffset(int endOffset) {
      myEndOffset = endOffset;
      return this;
    }

    public Factory withAtomicUnitWidth(int atomicUnitWidth) {
      myAtomicUnitWidth = atomicUnitWidth;
      return this;
    }

    public Factory withWeekendDecreaseFactor(float weekendDecreaseFactor) {
      myWeekendDecreaseFactor = weekendDecreaseFactor;
      return this;
    }

    public Factory withCalendar(GPCalendar calendar) {
      myCalendar = calendar;
      return this;
    }

    public Factory withRightMargin(int rightMarginTimeUnits) {
      myRightMarginTimeUnits = rightMarginTimeUnits;
      return this;
    }

    public Factory withOffsetStepFunction(Function<TimeUnit, Float> offsetStepFn) {
      myOffsetStepFn = offsetStepFn;
      return this;
    }
    
    protected void preBuild() {
      myBaseUnit = TimeUnitStack.Util.findCommonUnit(myBottomUnit, myTopUnit);
      if (myOffsetStepFn == null) {
        myOffsetStepFn = new Function<TimeUnit, Float>() {
          @Override
          public Float apply(TimeUnit value) {
            return Float.valueOf(value.getAtomCount(myBaseUnit));
          }
        };
      }
    }
    
    public abstract OffsetBuilder build();
  }

  void constructOffsets(List<Offset> topUnitOffsets, OffsetList bottomUnitOffsets);

}
