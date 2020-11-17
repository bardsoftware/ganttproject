/*
 * This code is provided under the terms of GPL version 3.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2010
 */
package biz.ganttproject.core.chart.grid;

import java.util.Date;
import java.util.List;

import com.google.common.base.Function;

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.calendar.GPCalendar.DayMask;
import biz.ganttproject.core.calendar.GPCalendar.DayType;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.walker.WorkingUnitCounter;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.TimeUnitFunctionOfDate;


/**
 * Builds grid offsets for timelines where top cells are always constructed from
 * the integer number of bottom cells (e.g. week from days)
 */
public class OffsetBuilderImpl implements OffsetBuilder {
  protected static class OffsetStep {
    public float parrots;
    public int dayMask;
  }

  // We want weekend units to be less wide than working ones. This constant
  // is a decrease factor
  public static final int WEEKEND_UNIT_WIDTH_DECREASE_FACTOR = 10;
  private final TimeUnit myTopUnit;
  private final TimeUnit myBottomUnit;
  private final Date myStartDate;
  private final int myDefaultUnitWidth;
  private final int myChartWidth;
  private final GPCalendar myCalendar;
  private final float myWeekendDecreaseFactor;
  private final Date myEndDate;
  private final TimeUnit baseUnit;
  private final int myRightMarginBottomUnitCount;
  private final Date myViewportStartDate;
  private final Function<TimeUnit, Float> myOffsetStepFn;

//  protected RegularFrameOffsetBuilder(GPCalendar calendar, TimeUnit topUnit, TimeUnit bottomUnit, Date startDate,
//      Date viewportStartDate, int defaultUnitWidth, int chartWidth, float weekendDecreaseFactor, Date endDate,
//      int rightMarginTimeUnits) {
  protected OffsetBuilderImpl(OffsetBuilder.Factory factory) {
    myCalendar = factory.myCalendar;
    myStartDate = factory.myStartDate;
    myViewportStartDate = factory.myViewportStartDate;
    myTopUnit = factory.myTopUnit;
    myBottomUnit = factory.myBottomUnit;
    myDefaultUnitWidth = factory.myAtomicUnitWidth;
    myChartWidth = factory.myEndOffset;
    myWeekendDecreaseFactor = factory.myWeekendDecreaseFactor;
    myEndDate = factory.myEndDate;
    baseUnit = factory.myBaseUnit;
    myRightMarginBottomUnitCount = factory.myRightMarginTimeUnits;
    myOffsetStepFn = factory.myOffsetStepFn;
  }

  private TimeUnit getBottomUnit() {
    return myBottomUnit;
  }

  private TimeUnit getTopUnit() {
    return myTopUnit;
  }

  public static TimeUnit getConcreteUnit(TimeUnit timeUnit, Date date) {
    return (timeUnit instanceof TimeUnitFunctionOfDate) ? ((TimeUnitFunctionOfDate) timeUnit).createTimeUnit(date)
        : timeUnit;
  }

  private int getDefaultUnitWidth() {
    return myDefaultUnitWidth;
  }

  protected float getOffsetStep(TimeUnit timeUnit) {
    return myOffsetStepFn.apply(timeUnit);
  }

  private int getChartWidth() {
    return myChartWidth;
  }

  private GPCalendar getCalendar() {
    return myCalendar;
  }

//  public void setRightMarginBottomUnitCount(int value) {
//    myRightMarginBottomUnitCount = value;
//  }

  @Override
  public void constructOffsets(List<Offset> topUnitOffsets, OffsetList bottomUnitOffsets) {
    constructOffsets(topUnitOffsets, bottomUnitOffsets, 0);
  }

  void constructOffsets(List<Offset> topUnitOffsets, List<Offset> bottomUnitOffsets, int initialEnd) {

    // bottomUnitOffsets.add(new Offset(getBottomUnit(), myStartDate,
    // myStartDate, myStartDate, 0, GPCalendar.DayType.WORKING));
    constructBottomOffsets(bottomUnitOffsets, initialEnd);
    if (topUnitOffsets != null) {
      constructTopOffsets(getTopUnit(), topUnitOffsets, bottomUnitOffsets, initialEnd, getDefaultUnitWidth());
    }
  }

  void constructBottomOffsets(List<Offset> offsets, int initialEnd) {
    int marginUnitCount = myRightMarginBottomUnitCount;
    Date currentDate = myStartDate;
    int shift = 0;
    OffsetStep step = new OffsetStep();
    int prevEnd = initialEnd;
    do {
      TimeUnit concreteTimeUnit = getConcreteUnit(getBottomUnit(), currentDate);
      calculateNextStep(step, concreteTimeUnit, currentDate);
      Date endDate = concreteTimeUnit.adjustRight(currentDate);
      if (endDate.compareTo(myViewportStartDate) <= 0) {
        shift = (int) (step.parrots * getDefaultUnitWidth());
      }
      int offsetEnd = (int) (step.parrots * getDefaultUnitWidth()) - shift;
      Offset offset = Offset.createFullyClosed(concreteTimeUnit, myStartDate, currentDate, endDate, 
          prevEnd, initialEnd + offsetEnd, step.dayMask);
      prevEnd = initialEnd + offsetEnd;
      offsets.add(offset);
      currentDate = endDate;

      boolean hasNext = true;
      if (offsetEnd > getChartWidth()) {
        hasNext &= marginUnitCount-- > 0;
      }
      if (hasNext && myEndDate != null) {
        hasNext &= currentDate.before(myEndDate);
      }
      if (!hasNext) {
        return;
      }
    } while (true);
  }

  private void constructTopOffsets(TimeUnit timeUnit, List<Offset> topOffsets, List<Offset> bottomOffsets,
      int initialEnd, int baseUnitWidth) {
    int lastBottomOffset = bottomOffsets.get(bottomOffsets.size() - 1).getOffsetPixels();
    OffsetLookup offsetLookup = new OffsetLookup();
    Date currentDate = myStartDate;
    int prevEnd = initialEnd;
    int offsetEnd;
    do {
      TimeUnit concreteTimeUnit = getConcreteUnit(timeUnit, currentDate);
      Date endDate = concreteTimeUnit.adjustRight(currentDate);
      int bottomOffsetLowerBound = offsetLookup.lookupOffsetByEndDate(endDate, bottomOffsets);
      if (bottomOffsetLowerBound >= 0) {
        offsetEnd = bottomOffsets.get(bottomOffsetLowerBound).getOffsetPixels();
      } else {
        if (-bottomOffsetLowerBound > bottomOffsets.size()) {
          offsetEnd = lastBottomOffset + 1;
        } else {
          Offset ubOffset = bottomOffsetLowerBound <= -2 ? bottomOffsets.get(-bottomOffsetLowerBound - 2) : null;
          Date ubEndDate = ubOffset == null ? myStartDate : ubOffset.getOffsetEnd();
          int ubEndPixel = ubOffset == null ? 0 : ubOffset.getOffsetPixels();
          WorkingUnitCounter counter = new WorkingUnitCounter(GPCalendarCalc.PLAIN, baseUnit);
          offsetEnd = ubEndPixel + counter.run(ubEndDate, endDate).getLength() * baseUnitWidth;
        }
      }
      topOffsets.add(Offset.createFullyClosed(concreteTimeUnit, myStartDate, currentDate, endDate, prevEnd, initialEnd
          + offsetEnd, DayMask.WORKING));
      prevEnd = initialEnd + offsetEnd;
      currentDate = endDate;

    } while (offsetEnd <= lastBottomOffset && (myEndDate == null || currentDate.before(myEndDate)));
  }

  protected void calculateNextStep(OffsetStep step, TimeUnit timeUnit, Date startDate) {
    float offsetStep = getOffsetStep(timeUnit);
    step.dayMask = getCalendar().getDayMask(startDate);
    if ((step.dayMask & DayMask.WORKING) == 0) {
      offsetStep = offsetStep / myWeekendDecreaseFactor;
    }
    step.parrots += offsetStep;
  }


  public static class FactoryImpl extends OffsetBuilder.Factory {
    @Override
    public OffsetBuilder build() {
      preBuild();
      return new OffsetBuilderImpl(this);
    }
  }
}