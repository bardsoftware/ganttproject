/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2010 Dmitry Barashev

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
package net.sourceforge.ganttproject.chart;

import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.time.TimeUnit;

public interface OffsetBuilder {
    public static abstract class Factory {
        protected TimeUnit myTopUnit;
        protected TimeUnit myBottomUnit;
        protected Date myStartDate;
        protected Date myEndDate;
        protected int myEndOffset;
        protected int myAtomicUnitWidth;
        protected float myWeekendDecreaseFactor;
        protected GPCalendar myCalendar;
        protected int myRightMarginTimeUnits;
    
        protected Factory() {
        }
        
        Factory withTopUnit(TimeUnit topUnit) {
            myTopUnit = topUnit;
            return this;
        }
        
        Factory withBottomUnit(TimeUnit bottomUnit) {
            myBottomUnit = bottomUnit;
            return this;
        }
        
        public Factory withStartDate(Date startDate) {
            myStartDate = startDate;
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
        
        Factory withAtomicUnitWidth(int atomicUnitWidth) {
            myAtomicUnitWidth = atomicUnitWidth;
            return this;
        }
        
        Factory withWeekendDecreaseFactor(float weekendDecreaseFactor) {
            myWeekendDecreaseFactor = weekendDecreaseFactor;
            return this;
        }
        
        Factory withCalendar(GPCalendar calendar) {
            myCalendar = calendar;
            return this;
        }
        
        Factory withRightMargin(int rightMarginTimeUnits) {
            myRightMarginTimeUnits = rightMarginTimeUnits;
            return this;
        }
        
        public abstract OffsetBuilder build();
    }
    void constructOffsets(List<Offset> topUnitOffsets, OffsetList bottomUnitOffsets);
    
}
