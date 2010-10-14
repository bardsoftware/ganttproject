/*
 * Created on 08.11.2004
 */
package net.sourceforge.ganttproject.time.gregorian;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitGraph;
import net.sourceforge.ganttproject.time.TimeUnitPair;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * @author bard
 */
public class GPTimeUnitStack implements TimeUnitStack {
    private TimeUnitGraph ourGraph = new TimeUnitGraph();

    private final TimeUnit HOUR = ourGraph.createAtomTimeUnit("hour");
    public final TimeUnit DAY;

    public final TimeUnit WEEK;

    public final TimeUnit MONTH;

    public TimeUnit QUARTER;

    public TimeUnit YEAR = null;

    private final TimeUnitPair[] myPairs;

    private TimeUnit MONTH_FROM_WEEKS;

    public final TimeUnit WEEK_AS_BOTTOM_UNIT;

    /**
     *
     */
    public GPTimeUnitStack(GanttLanguage i18n) {
        TimeUnit atom = ourGraph.createAtomTimeUnit("atom");
        DAY = ourGraph.createDateFrameableTimeUnit("day", atom, 1,
                new FramerImpl(Calendar.DATE));
        DAY.setTextFormatter(new DayTextFormatter());
        MONTH = ourGraph.createTimeUnitFunctionOfDate("month", DAY,
                new FramerImpl(Calendar.MONTH));
        MONTH.setTextFormatter(new MonthTextFormatter());
        WEEK = ourGraph.createDateFrameableTimeUnit("week", DAY, 7,
                new WeekFramerImpl());
        MONTH_FROM_WEEKS = ourGraph.createTimeUnitFunctionOfDate(
                "month_from_weeks", WEEK, new FramerImpl(Calendar.MONTH));
        WEEK.setTextFormatter(new WeekTextFormatter(i18n.getText("week")
                + " {0}"));
        WEEK_AS_BOTTOM_UNIT = ourGraph.createDateFrameableTimeUnit("week", DAY,
                7, new WeekFramerImpl());
        WEEK_AS_BOTTOM_UNIT.setTextFormatter(new WeekTextFormatter("{0}"));
        YEAR = ourGraph.createTimeUnitFunctionOfDate("year", DAY,
                new FramerImpl(Calendar.YEAR));
        YEAR.setTextFormatter(new YearTextFormatter());
        myPairs = new TimeUnitPair[] { new MyTimeUnitPair(WEEK, DAY),
                new MyTimeUnitPair(WEEK, DAY), new MyTimeUnitPair(MONTH, DAY),
                new MyTimeUnitPair(MONTH, WEEK_AS_BOTTOM_UNIT),
                new MyTimeUnitPair(MONTH, WEEK_AS_BOTTOM_UNIT),
                new MyTimeUnitPair(MONTH, WEEK_AS_BOTTOM_UNIT),
                new MyTimeUnitPair(MONTH, WEEK_AS_BOTTOM_UNIT),
                new MyTimeUnitPair(YEAR, WEEK_AS_BOTTOM_UNIT),
                new MyTimeUnitPair(YEAR, WEEK_AS_BOTTOM_UNIT) };
    }

    public TimeFrame createTimeFrame(Date baseDate, TimeUnit topUnit,
            TimeUnit bottomUnit) {
        // if (topUnit instanceof TimeUnitFunctionOfDate) {
        // topUnit = ((TimeUnitFunctionOfDate)topUnit).createTimeUnit(baseDate);
        // }
        return new TimeFrameImpl(baseDate, topUnit, bottomUnit);
    }

    public String getName() {
        return "default";
    }

    public TimeUnit getDefaultTimeUnit() {
        return DAY;
    }

    public TimeUnitPair[] getTimeUnitPairs() {
        return myPairs;
    }

    private class MyTimeUnitPair extends TimeUnitPair {
        MyTimeUnitPair(TimeUnit topUnit, TimeUnit bottomUnit) {
            super(topUnit, bottomUnit, GPTimeUnitStack.this);
        }
    }

    public DateFormat[] getDateFormats() {
        DateFormat[] result;
        if (HOUR.isConstructedFrom(getDefaultTimeUnit())) {
            result = new DateFormat[] {
                DateFormat.getDateInstance(DateFormat.MEDIUM),
                DateFormat.getDateInstance(DateFormat.MEDIUM),
                DateFormat.getDateInstance(DateFormat.SHORT),
            };
        }
        else {
            result = new DateFormat[] {
                DateFormat.getDateInstance(DateFormat.LONG),
                DateFormat.getDateInstance(DateFormat.MEDIUM),
                DateFormat.getDateInstance(DateFormat.SHORT),
            };
        }
        return result;
    }

    public DateFormat getTimeFormat() {
        if (HOUR.isConstructedFrom(getDefaultTimeUnit())) {
            return DateFormat.getTimeInstance(DateFormat.SHORT);
        }
        return null;
    }

    public TimeUnit findTimeUnit(String code) {
        assert code!=null;
        code = code.trim();
        if (isHour(code)) {
            return HOUR;
        }
        if (isDay(code)) {
            return DAY;
        }
        if (isWeek(code)) {
            return WEEK_AS_BOTTOM_UNIT;
        }
        return null;
    }

    private boolean isWeek(String code) {
        return "w".equalsIgnoreCase(code);
    }

    private boolean isDay(String code) {
        return "d".equalsIgnoreCase(code);
    }

    private boolean isHour(String code) {
        return "h".equalsIgnoreCase(code);
    }

    public String encode(TimeUnit timeUnit) {
        if (timeUnit==HOUR) {
            return "h";
        }
        if (timeUnit==DAY) {
            return "d";
        }
        if (timeUnit==WEEK_AS_BOTTOM_UNIT) {
            return "w";
        }
        throw new IllegalArgumentException();
    }

}
