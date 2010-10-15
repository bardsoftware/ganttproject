/*
 * This code is provided under the terms of GPL version 2.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.GPCalendar.DayType;
import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.time.TimeFrame;
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

    RegularFrameOffsetBuilder(
            TimeUnitStack timeUnitStack, GPCalendar calendar, TimeUnit topUnit, TimeUnit bottomUnit, Date startDate,
            int bottomUnitWidth, int chartWidth, float weekendDecreaseFactor) {
        myTimeUnitStack = timeUnitStack;
        myCalendar = calendar;
        myStartDate = startDate;
        myTopUnit = topUnit;
        myBottomUnit = bottomUnit;
        myBottomUnitWidth = bottomUnitWidth;
        myChartWidth = chartWidth;
        myWeekendDecreaseFactor = weekendDecreaseFactor;
        myEndDate = null;
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

    protected float getOffsetStep(TimeFrame timeFrame) {
        return 1;
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
        Date currentDate = myStartDate;

        bottomUnitOffsets.add(new Offset(getBottomUnit(), myStartDate, myStartDate, myStartDate, 0, GPCalendar.DayType.WORKING));
        // We don't want to create numerous vertical stripes for weekend units (e.g., for 16
        // non-working hours may produce 16 vertical stripes that looks awful). We
        // accumulate consecutive weekend units instead and add just a single block.
//        Offset accumulatedWeekendOffset = null;
        int offsetEnd = 0;
        OffsetStep step = new OffsetStep();
        do {
            TimeFrame currentFrame = getTimeUnitStack().createTimeFrame(
                    currentDate, getTopUnit(currentDate), myBottomUnit);
            int bottomUnitCount = currentFrame.getUnitCount(getBottomUnit());
            // This will be true if there is at least one working bottom unit in this time frame
            // If there are only weekend bottom units, we'll merge neighbor top units
            // (like merging two weekend days into one continuous grey stripe)
//            boolean addTopUnitOffset = false;
            for (int i=0; i<bottomUnitCount; i++) {
                step.dayType = null;
//                step.incrementTopUnit = true;
                Date startDate = currentFrame.getUnitStart(getBottomUnit(), i);
                Date endDate = currentFrame.getUnitFinish(getBottomUnit(), i);
                calculateNextStep(step, currentFrame, startDate);
                offsetEnd = (int)(step.parrots*getBottomUnitWidth());
//                if (!step.incrementTopUnit) {
//                    accumulatedWeekendOffset = new Offset(
//                            getBottomUnit(), myStartDate, endDate,
//                            initialEnd+offsetEnd, step.dayType);
//                    continue;
//                }
//                addTopUnitOffset = true;
//                if (accumulatedWeekendOffset != null) {
//                    bottomUnitOffsets.add(accumulatedWeekendOffset);
//                    accumulatedWeekendOffset = null;
//                }
                bottomUnitOffsets.add(new Offset(
                        getBottomUnit(), myStartDate, startDate, endDate, initialEnd+offsetEnd, step.dayType));
            }
//            if (accumulatedWeekendOffset != null) {
//                bottomUnitOffsets.add(accumulatedWeekendOffset);
//                accumulatedWeekendOffset = null;
//            }
            currentDate = currentFrame.getFinishDate();
//            if (!addTopUnitOffset) {
//                continue;
//            }
            topUnitOffsets.add(new Offset(
                    getTopUnit(), myStartDate, currentFrame.getStartDate(), currentDate, initialEnd+offsetEnd, DayType.WORKING));

        } while (offsetEnd <= getChartWidth() && (myEndDate==null || currentDate.before(myEndDate)));
    }

    protected void calculateNextStep(OffsetStep step, TimeFrame currentFrame, Date startDate) {
        float offsetStep = getOffsetStep(currentFrame);
        step.dayType = getCalendar().getDayTypeDate(startDate);// ? GPCalendar.DayType.WORKING : GPCalendar.DayType.WEEKEND;
        //step.dayType = GPCalendar.DayType.WORKING;
        if (getCalendar().isNonWorkingDay(startDate)) {
            offsetStep = offsetStep / myWeekendDecreaseFactor;
        }
        step.parrots += offsetStep;
    }
}