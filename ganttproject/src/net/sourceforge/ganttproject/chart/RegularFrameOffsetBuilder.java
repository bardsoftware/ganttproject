/*
 * This code is provided under the terms of GPL version 2.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.calendar.walker.WorkingUnitCounter;
import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.gui.options.model.GP1XOptionConverter;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitFunctionOfDate;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * Builds grid offsets for timelines where top cells are always constructed from the integer
 * number of bottom cells (e.g. week from days)
 */
class RegularFrameOffsetBuilder {
    protected static class OffsetStep {
        public float parrots;
        public GPCalendar.DayType dayType;
    }
    // We want weekend units to be less wide than working ones. This constant
    // is a decrease factor
    public static final int WEEKEND_UNIT_WIDTH_DECREASE_FACTOR = 10;
    private final TimeUnitStack myTimeUnitStack;
    private final TimeUnit myTopUnit;
    private final TimeUnit myBottomUnit;
    private final Date myStartDate;
    private final int myBottomUnitWidth;
    private final int myChartWidth;
    private final GPCalendar myCalendar;
    private final float myWeekendDecreaseFactor;
    private final Date myEndDate;
    private final TimeUnit baseUnit; 

    RegularFrameOffsetBuilder(
            TimeUnitStack timeUnitStack, GPCalendar calendar, TimeUnit topUnit, TimeUnit bottomUnit, Date startDate,
            int bottomUnitWidth, int chartWidth, float weekendDecreaseFactor) {
        this( timeUnitStack,  calendar,  topUnit,  bottomUnit,  startDate,
                 bottomUnitWidth,  chartWidth,  weekendDecreaseFactor, null);
    }

    RegularFrameOffsetBuilder(
            TimeUnitStack timeUnitStack, GPCalendar calendar, TimeUnit topUnit, TimeUnit bottomUnit, Date startDate,
            int bottomUnitWidth, int chartWidth, float weekendDecreaseFactor, Date endDate) {
        myTimeUnitStack = timeUnitStack;
        myCalendar = calendar;
        myStartDate = startDate;
        myTopUnit = topUnit;
        myBottomUnit = bottomUnit;
        myBottomUnitWidth = bottomUnitWidth;
        myChartWidth = chartWidth;
        myWeekendDecreaseFactor = weekendDecreaseFactor;
        myEndDate = endDate;
        baseUnit = findCommonUnit(bottomUnit, topUnit);
    }

    protected TimeUnit getBottomUnit() {
        return myBottomUnit;
    }

    protected TimeUnit getTopUnit() {
        return myTopUnit;
    }

    protected TimeUnit getTopUnit(Date startDate) {
        TimeUnit result = myTopUnit;
        if (myTopUnit instanceof TimeUnitFunctionOfDate) {
            if (startDate == null) {
                throw new RuntimeException("No date is set");
            } else {
                result = ((TimeUnitFunctionOfDate) myTopUnit).createTimeUnit(startDate);
            }
        }
        return result;
    }

    protected int getBottomUnitWidth() {
        return myBottomUnitWidth;
    }

    protected float getOffsetStep(TimeUnit timeUnit) {
        return timeUnit.getAtomCount(baseUnit);
    }

    protected int getChartWidth() {
        return myChartWidth;
    }

    protected GPCalendar getCalendar() {
        return myCalendar;
    }

    protected TimeUnitStack getTimeUnitStack() {
        return myTimeUnitStack;
    }

    void constructOffsets(List<Offset> topUnitOffsets, List<Offset> bottomUnitOffsets) {
        constructOffsets(topUnitOffsets, bottomUnitOffsets, 0);
    }
    void constructOffsets(List<Offset> topUnitOffsets, List<Offset> bottomUnitOffsets, int initialEnd) {

        bottomUnitOffsets.add(new Offset(getBottomUnit(), myStartDate, myStartDate, myStartDate, 0, GPCalendar.DayType.WORKING));
        constructBottomOffsets(getBottomUnit(), bottomUnitOffsets, initialEnd, getBottomUnitWidth());
        //constructBottomOffsets(getTopUnit(), topUnitOffsets, initialEnd, getBottomUnitWidth());
        constructTopOffsets(getTopUnit(), topUnitOffsets, bottomUnitOffsets, initialEnd, getBottomUnitWidth());
    }

    void constructBottomOffsets(TimeUnit timeUnit, List<Offset> offsets, int initialEnd, int baseUnitWidth) {
        Date currentDate = myStartDate;
        int offsetEnd = 0;
        OffsetStep step = new OffsetStep();
        TimeUnit concreteTimeUnit = timeUnit;
        do {
            if (timeUnit instanceof TimeUnitFunctionOfDate) {
                concreteTimeUnit = ((TimeUnitFunctionOfDate)timeUnit).createTimeUnit(currentDate);
            }
            calculateNextStep(step, concreteTimeUnit, currentDate);
            Date endDate = concreteTimeUnit.adjustRight(currentDate);
            offsetEnd = (int) (step.parrots * baseUnitWidth);
            offsets.add(new Offset(
                concreteTimeUnit, myStartDate, currentDate, endDate, initialEnd+offsetEnd, step.dayType));
            currentDate = endDate;
        } while (offsetEnd <= getChartWidth() && (myEndDate == null || currentDate.before(myEndDate)));
    }

    private void constructTopOffsets(TimeUnit timeUnit, List<Offset> topOffsets, List<Offset> bottomOffsets, int initialEnd, int baseUnitWidth) {
        OffsetLookup offsetLookup = new OffsetLookup();
        Date currentDate = myStartDate;
        int offsetEnd;
        TimeUnit concreteTimeUnit = timeUnit;
        do {
            if (timeUnit instanceof TimeUnitFunctionOfDate) {
                concreteTimeUnit = ((TimeUnitFunctionOfDate)timeUnit).createTimeUnit(currentDate);
            }
            Date endDate = concreteTimeUnit.adjustRight(currentDate);
            int bottomOffsetLowerBound = offsetLookup.lookupOffsetByEndDate(endDate, bottomOffsets);
            if (bottomOffsetLowerBound >= 0) {
                offsetEnd = bottomOffsets.get(bottomOffsetLowerBound).getOffsetPixels();
            } else {
                if (-bottomOffsetLowerBound > bottomOffsets.size()) {
                    offsetEnd = getChartWidth() + 1;
                } else {
                    Offset ubOffset = bottomOffsetLowerBound <= -2 ?
                        bottomOffsets.get(-bottomOffsetLowerBound - 2) : null;
                    Date ubEndDate = ubOffset == null ? myStartDate : ubOffset.getOffsetEnd();
                    int ubEndPixel = ubOffset == null ? 0 : ubOffset.getOffsetPixels();
                    WorkingUnitCounter counter = new WorkingUnitCounter(GPCalendar.PLAIN, baseUnit);               
                    offsetEnd = ubEndPixel + counter.run(ubEndDate, endDate).getLength() * baseUnitWidth;
                }
            }
            topOffsets.add(new Offset(concreteTimeUnit, myStartDate, currentDate, endDate, initialEnd + offsetEnd, DayType.WORKING));
            currentDate = endDate;
        } while (offsetEnd <= getChartWidth() && (myEndDate==null || currentDate.before(myEndDate)));
    }

    protected void calculateNextStep(OffsetStep step, TimeUnit timeUnit, Date startDate) {
        float offsetStep = getOffsetStep(timeUnit);
        step.dayType = getCalendar().getDayTypeDate(startDate);// ? GPCalendar.DayType.WORKING : GPCalendar.DayType.WEEKEND;
        //step.dayType = GPCalendar.DayType.WORKING;
        if (getCalendar().isNonWorkingDay(startDate)) {
            offsetStep = offsetStep / myWeekendDecreaseFactor;
        }
        step.parrots += offsetStep;
    }

    /**
     * @returns a common TimeUnit for the given units or null if none if found
     *          (should not happen since all should be derived from atom)
     */
    //TODO Method might be nice for other things... If so, refactor to a more common location
    private TimeUnit findCommonUnit(TimeUnit unit1, TimeUnit unit2) {

        // Create (cache) list with TimeUnits which can be derived from unit1
        ArrayList<TimeUnit> units1 = new ArrayList<TimeUnit>();
        TimeUnit current = unit1;
        do {
            units1.add(current);
        } while((current = current.getDirectAtomUnit()) != null);
        
        // Now compare lists to find a common unit
        current = unit2;
        while(current != null) {
            Iterator<TimeUnit> u1Iterator = units1.iterator();
            while(u1Iterator.hasNext()) {
                TimeUnit nextU1 = u1Iterator.next();
                if(current.equals(nextU1)) {
                    return current;
                }
            }
            current = current.getDirectAtomUnit();
        }
        return null;
    }
}