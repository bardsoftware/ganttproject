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
package net.sourceforge.ganttproject.time.gregorian;

import java.util.Calendar;
import java.util.Date;

import net.sourceforge.ganttproject.calendar.CalendarFactory;
import net.sourceforge.ganttproject.time.DateFrameable;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitFunctionOfDate;
import net.sourceforge.ganttproject.time.TimeUnitText;

// TODO Remove all commented parts in this class?
class TimeFrameImpl implements TimeFrame {
    private final Date myBaseDate;

    private final TimeUnit myTopUnit;

    private final TimeUnit myBottomUnit;

    private Calendar myCalendar;

    private DateFrameable myLowestFrameable;

    private Date myStartDate;

    private Date myEndDate;

    private LineHeader myLineHeader;

    TimeFrameImpl(Date baseDate, TimeUnit topUnit, TimeUnit bottomUnit) {
        // if (!topUnit.isConstructedFrom(bottomUnit)) {
        // throw new RuntimeException("Top unit="+topUnit+" is not constructed
        // from bottom unit="+bottomUnit);
        // }
        if (topUnit instanceof TimeUnitFunctionOfDate) {
            topUnit = ((TimeUnitFunctionOfDate) topUnit)
                    .createTimeUnit(baseDate);
        }
        myBaseDate = baseDate;
        myLowestFrameable = calculateLowestFrameableUnit(topUnit, bottomUnit);
        // if (myHighestFrameable!=topUnit || myLowestFrameable!=bottomUnit) {
        // throw new RuntimeException("Current implementation requires all units
        // to be frameable. myHighestFrameable="+myHighestFrameable+"
        // myLowestFrameable="+myLowestFrameable);
        // }
        myTopUnit = topUnit;
        myBottomUnit = bottomUnit;
        myCalendar = CalendarFactory.newCalendar();
        myCalendar.setTime(myBaseDate);
        if (myLowestFrameable == null) {
            throw new RuntimeException(
                    "Failed to find any time frameable unit :(");
        }
        myStartDate = myLowestFrameable.adjustLeft(myBaseDate);
        if (myStartDate.after(myBaseDate)) {
            throw new IllegalStateException("Start date is after base date");
        }
        // System.err.println("TimeFrame.init: myStartDate="+myStartDate+"
        // myBaseDate="+myBaseDate);
        // myEndDate = calculateEndDate();
        // myNextFrameStartDate = shiftDate(myBaseDate, myTopUnit, 1);
    }

    private DateFrameable calculateLowestFrameableUnit(TimeUnit topUnit,
            TimeUnit bottomUnit) {
        // DateFrameable lowestFrameable = null;
        // for (TimeUnit timeUnit = topUnit; timeUnit!=null; timeUnit =
        // timeUnit.getDirectAtomUnit()) {
        // if (timeUnit instanceof DateFrameable) {
        // lowestFrameable = (DateFrameable)timeUnit;
        // }
        // if (bottomUnit.equals(timeUnit)) {
        // break;
        // }
        // }
        // return lowestFrameable;
        return bottomUnit;
    }

    private Date calculateEndDate() {
        // int countFrameable =
        // myTopUnit.getAtomCount((TimeUnit)myHighestFrameable);
        Date date = myTopUnit.adjustRight(myBaseDate);
        if (date.before(myStartDate)) {
            throw new IllegalStateException("End date="+date+" start="+myStartDate+" base="+myBaseDate);
        }
        // for (int i=0; i<countFrameable; i++) {
        // date = myHighestFrameable.adjustRight(date);
        // }
        return date;
    }

    private LineHeader calculateLines(LineHeader lastHeader) {
        TimeUnit curUnit = lastHeader == null ? myTopUnit : lastHeader.myUnit
                .getDirectAtomUnit();
        LineHeader curHeader = createHeader(curUnit);
        fillLine(lastHeader, curHeader);
        if (lastHeader != null) {
            lastHeader.append(curHeader);
        }
        if (curUnit != myBottomUnit) {
            calculateLines(curHeader);
        }
        return curHeader;
    }

    private void fillLine(LineHeader higherHeader, LineHeader header) {
        if (higherHeader == null) {
            Date startDate = myStartDate;
            Date endDate = ((DateFrameable) myTopUnit).adjustRight(myBaseDate);
            // System.err.println("filling line="+header+" endDate="+endDate);
            LineItem item = createLineItem(startDate, endDate);
            header.myFirstItem = item;
        } else {
            // System.err.println("filling line="+header);
            for (LineItem higherItem = higherHeader.myFirstItem; higherItem != null; higherItem = higherItem.myNextItem) {
                int unitCount = getUnitCount(higherHeader, header, higherItem);
                Date curStartDate = higherItem.myStartDate;
                LineItem curItem = null;
                // System.err.println("unit count="+unitCount+"
                // startDate="+curStartDate);
                for (int i = 0; i < unitCount
                        && curStartDate.compareTo(higherItem.myEndDate) < 0; i++) {
                    Date nextEndDate = ((DateFrameable) header.myUnit)
                            .adjustRight(curStartDate);
                    LineItem newItem = createLineItem(curStartDate, nextEndDate);
                    if (curItem == null) {
                        header.myFirstItem = newItem;
                    } else {
                        curItem.myNextItem = newItem;
                    }
                    curItem = newItem;
                    curStartDate = nextEndDate;
                }
                // System.err.println("result: "+header.fullDump());
            }
        }

    }

    private int getUnitCount(LineHeader higherHeader, LineHeader header,
            LineItem higherItem) {
        TimeUnit higherUnit = higherHeader.myUnit instanceof TimeUnitFunctionOfDate ? ((TimeUnitFunctionOfDate) higherHeader.myUnit)
                .createTimeUnit(higherItem.myStartDate)
                : higherHeader.myUnit;
        TimeUnit lowerUnit = header.myUnit;
        int result = higherUnit.getAtomCount(lowerUnit);
        return result;
    }

    private LineItem createLineItem(Date startDate, Date endDate) {
        return new LineItem(startDate, endDate);
    }

    private LineHeader createHeader(TimeUnit unit) {
        return new LineHeader(unit);
    }

    public Date getFinishDate() {
        if (myEndDate == null) {
            myEndDate = calculateEndDate();
            // System.err.println("getFinishDate(): startDate="+myStartDate+"
            // finish="+myEndDate);
        }
        return myEndDate;
    }

    public int getUnitCount(TimeUnit unit) {
        LineHeader lineHeader = getLineHeader(unit);
        if (lineHeader == null) {
            lineHeader = new LineHeader(unit);
            // return getUnitCount(getLineHeader(), lineHeader,
            // getLineHeader().myFirstItem);
            fillLine(getLineHeader(), lineHeader);
            return lineHeader.getItemCount();
        }
        int result = lineHeader.getItemCount();
        if (result == -1) {
            throw new RuntimeException("There is not time unit=" + unit
                    + " in this time frame");
        }
        return result;
    }

    private LineHeader getLineHeader(TimeUnit timeUnit) {
        LineHeader result = getLineHeader();
        for (; result != null; result = result.next()) {
            if (result.myUnit == timeUnit) {
                break;
            }
        }
        return result;
    }

    // public int _getUnitCount(TimeUnit unit) {
    // if (unit.isConstructedFrom((TimeUnit)myLowestFrameable)) {
    // UnitInfo info = calculateInfo(myHighestFrameable, myEndDate);
    // if (unit==myHighestFrameable) {
    // return info.myRoundedCount;
    // }
    // else if (unit.isConstructedFrom((TimeUnit)myHighestFrameable)) {
    // int atomCount = unit.getAtomCount((TimeUnit)myHighestFrameable);
    // return (info.myRoundedCount/atomCount) +
    // (info.myRoundedCount%atomCount==0 ? 0 : 1);
    // }
    // else {
    // int atomCount1 = ((TimeUnit)myHighestFrameable).getAtomCount(unit);
    // int count = info.myTruncatedCount*atomCount1;
    // if (info.myRoundedCount>info.myTruncatedCount) {
    // UnitInfo lowestInfo = calculateInfo(myLowestFrameable, info.lastDate);
    // int atomCount2 = unit.getAtomCount((TimeUnit)myLowestFrameable);
    // count += lowestInfo.myRoundedCount/atomCount2 +
    // (lowestInfo.myRoundedCount%atomCount2==0 ? 0 : 1);
    // }
    // return count;
    // }
    // }
    // else {
    // UnitInfo lowestInfo = calculateInfo(myLowestFrameable, myEndDate);
    // int atomCount = ((TimeUnit)myLowestFrameable).getAtomCount(unit);
    // return atomCount*lowestInfo.myRoundedCount;
    // }
    // }
    //
    // private UnitInfo calculateInfo(DateFrameable frameable, Date date) {
    // Date lastDate = date;
    // int count = 0;
    // for (;date.compareTo(myStartDate)>0; count++) {
    // lastDate = date;
    // date = frameable.jumpLeft(date);
    // }
    // int truncatedCount = date.compareTo(myStartDate)<0 ? count-1 : count;
    // return new UnitInfo(truncatedCount, count, lastDate);
    // }
    // private Date shiftDate(Date currentDate, TimeUnit timeUnit, int
    // unitCount) {
    // Calendar c = (Calendar) myCalendar.clone();
    // c.setTime(currentDate);
    // int calendarField = getCalendarField(timeUnit);
    // int currentValue = c.get(calendarField);
    // clearFields(c, timeUnit);
    // c.add(calendarField, unitCount);
    // return c.getTime();
    // }
    //
    // private void clearFields(Calendar c, TimeUnit topUnit) {
    // for (TimeUnit currentUnit = topUnit; currentUnit!=null; currentUnit =
    // currentUnit.getDirectAtomUnit()) {
    // int calendarField = getCalendarField(currentUnit);
    // c.clear(calendarField);
    // c.getTime();
    // }
    // }
    //
    // private int getCalendarField(TimeUnit timeUnit) {
    // Integer field = (Integer) ourUnit2field.get(timeUnit);
    // return field.intValue();
    // }

    public Date getStartDate() {
        return myStartDate;
    }

    public TimeUnit getTopUnit() {
        return myTopUnit;
    }

    public TimeUnit getBottomUnit() {
        return myBottomUnit;
    }

    // public int getUnitCount(TimeUnit unitLine) {
    // int counter = 0;
    // for (Date nextUnitStart = shiftDate(myStartDate, unitLine, counter);
    // nextUnitStart.before(myNextFrameStartDate);
    // nextUnitStart = shiftDate(myStartDate, unitLine, ++counter)) {
    // //System.err.println("myStart="+myStartDate+"
    // nextFrame="+myNextFrameStartDate+" nextUnitStart="+nextUnitStart);
    // }
    // return counter;
    // }

    public TimeUnitText getUnitText(TimeUnit unitLine, int position) {
        LineHeader lineHeader = getLineHeader(unitLine);
        LineItem lineItem = lineHeader == null ? null : lineHeader
                .getLineItem(position);
        Date startDate = lineItem == null ? null : lineItem.myStartDate;
        TimeUnitText result = startDate == null ? null : getUnitText(unitLine,
                startDate);
        // if ("31".equals(result)) {
        // System.err.println("unit line="+unitLine+" position="+position);
        // }
        return result;
    }

    private TimeUnitText getUnitText(TimeUnit unitLine, Date startDate) {
        // String result = null;
        return unitLine.format(startDate);
        // if (unitLine.equals(GregorianTimeUnitStack.DAY)) {
        // result = ""+startDate.getDate();
        // }
        // return result;
    }

    public Date getUnitStart(TimeUnit unitLine, int position) {
        LineHeader lineHeader = getLineHeader(unitLine);
        LineItem lineItem = lineHeader == null ? null : lineHeader
                .getLineItem(position);
        Date result = lineItem == null ? null : lineItem.myStartDate;
        return result;
    }

    public Date getUnitFinish(TimeUnit unitLine, int position) {
        LineHeader lineHeader = getLineHeader(unitLine);
        LineItem lineItem = lineHeader == null ? null : lineHeader
                .getLineItem(position);
        Date result = lineItem == null ? null : lineItem.myEndDate;
        return result;
    }

    public String toString() {
        return "Time frame start=" + getStartDate() + " end=" + getFinishDate()
                + "\n top unit=" + getTopUnit() + "\n bottom unit="
                + getBottomUnit();
    }

    private static class LineHeader {
        final TimeUnit myUnit;

        LineItem myFirstItem;

        private LineHeader myNextHeader;

        private int myItemCount = -1;

        public LineHeader(TimeUnit myUnit) {
            this.myUnit = myUnit;
        }

        public String toString() {
            return myUnit.toString();
        }

        void append(LineHeader next) {
            myNextHeader = next;
        }

        LineHeader next() {
            return myNextHeader;
        }

        public int getItemCount() {
            if (myItemCount == -1) {
                myItemCount = 0;
                for (LineItem item = myFirstItem; item != null; item = item.myNextItem) {
                    myItemCount++;
                }
            }
            return myItemCount;
        }

        LineItem getLineItem(int position) {
            LineItem result = myFirstItem;
            for (; result != null && position-- > 0; result = result.myNextItem) {
                // position--;
            }
            return result;
        }
    }

    private static class LineItem {
        LineItem myNextItem;

        final Date myStartDate;

        final Date myEndDate;

        public LineItem(Date myStartDate, Date myEndDate) {
            this.myStartDate = myStartDate;
            this.myEndDate = myEndDate;
        }

        public String toString() {
            return myStartDate.toString() + " - " + myEndDate.toString();
        }
    }

    public void trimLeft(Date exactDate) {
        myStartDate = exactDate;
        myLineHeader = null;
    }

    private LineHeader getLineHeader() {
        if (myLineHeader == null) {
            myLineHeader = calculateLines(null);
        }
        return myLineHeader;
    }
}
