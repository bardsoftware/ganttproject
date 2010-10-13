/*
 * This code is provided under the terms of GPL version 2.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.chart;

import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.chart.ChartModelBase.Offset;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitFunctionOfDate;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * Builds grid offsets for timelines where top cells are NOT constructed from the integer
 * number of bottom cells (e.g. months and weeks)
 */
class SkewedFrameOffsetBuilder {
    private final TimeUnitStack myTimeUnitStack;
    private final TimeUnit myTopUnit;
    private final TimeUnit myBottomUnit;
    private final Date myStartDate;
    private final int myBottomUnitWidth;
    private final int myChartWidth;
    private final GPCalendar myCalendar;

    SkewedFrameOffsetBuilder(
            TimeUnitStack timeUnitStack, GPCalendar calendar, TimeUnit topUnit, TimeUnit bottomUnit, Date startDate,
            int bottomUnitWidth, int chartWidth) {
        myTimeUnitStack = timeUnitStack;
        myCalendar = calendar;
        myStartDate = startDate;
        myTopUnit = topUnit;
        myBottomUnit = bottomUnit;
        myBottomUnitWidth = bottomUnitWidth;
        myChartWidth = chartWidth;
    }

    protected TimeUnit getBottomUnit() {
        return myBottomUnit;
    }

//    protected TimeUnit getTopUnit() {
//        return myTopUnit;
//    }

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
        {
            int offsetEnd = 0;
            Date currentDate = myStartDate;
            do {
                final TimeUnit bottomUnit = getBottomUnit();
                final TimeUnit defaultUnit = myTimeUnitStack.getDefaultTimeUnit();
                TimeFrame currentFrame = myTimeUnitStack.createTimeFrame(
                        currentDate, bottomUnit, defaultUnit);
                Date endDate = currentFrame.getFinishDate();
                offsetEnd += getBottomUnitWidth();
                bottomUnitOffsets.add(
                        new Offset(
                                bottomUnit,
                                myStartDate,
                                currentFrame.getStartDate(),
                                endDate,
                                offsetEnd,
                                null));
                currentDate = endDate;
            } while (offsetEnd <= getChartWidth());
        }
        {
            int offsetEnd = 0;
            Date currentDate = myStartDate;
            SkewedFramesWidthFunction widthFunction = new SkewedFramesWidthFunction();
            widthFunction.initialize();
            do {
                final TimeUnit topUnit = getTopUnit(currentDate);
                final TimeUnit defaultUnit = myTimeUnitStack.getDefaultTimeUnit();
                final TimeFrame currentFrame = myTimeUnitStack.createTimeFrame(
                        currentDate, topUnit, defaultUnit);

                Date endDate = currentFrame.getFinishDate();
                offsetEnd += widthFunction.getTimeFrameWidth(currentFrame);
                topUnitOffsets.add(
                        new Offset(
                                topUnit,
                                myStartDate,
                                currentFrame.getStartDate(),
                                endDate,
                                offsetEnd,
                                null));
                currentDate = endDate;
            } while (offsetEnd <= getChartWidth());
        }
   }


    private class SkewedFramesWidthFunction {
        private float myWidthPerDefaultUnit;

        void initialize() {
            int defaultUnitsPerBottomUnit = myBottomUnit
                    .getAtomCount(myTimeUnitStack.getDefaultTimeUnit());
            myWidthPerDefaultUnit = (float) getBottomUnitWidth()
                    / defaultUnitsPerBottomUnit;
        }

        public int getTimeFrameWidth(TimeFrame timeFrame) {
            int defaultUnitsPerTopUnit = timeFrame.getUnitCount(myTimeUnitStack
                    .getDefaultTimeUnit());
            return (int) (defaultUnitsPerTopUnit * myWidthPerDefaultUnit);
        }

    }


}