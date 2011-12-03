/*
 * Created on 08.11.2004
 */
package net.sourceforge.ganttproject.time.gregorian;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskLengthImpl;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitGraph;
import net.sourceforge.ganttproject.time.TimeUnitPair;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * @author bard
 */
public class GPTimeUnitStack implements TimeUnitStack {
    private static GanttLanguage i18n = GanttLanguage.getInstance();

    private static TimeUnitGraph ourGraph = new TimeUnitGraph();

    private static final TimeUnit HOUR = ourGraph.createAtomTimeUnit("hour");
    public static final TimeUnit DAY;

    public static final TimeUnit WEEK;

    public static final TimeUnit MONTH;

    public static final TimeUnit QUARTER;

    public static final TimeUnit YEAR;

    private final TimeUnitPair[] myPairs;

    public static final TimeUnit WEEK_AS_BOTTOM_UNIT;

    static {
        TimeUnit atom = ourGraph.createAtomTimeUnit("atom");
        DAY = ourGraph.createDateFrameableTimeUnit("day", atom, 1,
                new FramerImpl(Calendar.DATE));
        DAY.setTextFormatter(new DayTextFormatter());
        MONTH = ourGraph.createTimeUnitFunctionOfDate("month", DAY,
                new FramerImpl(Calendar.MONTH));
        MONTH.setTextFormatter(new MonthTextFormatter());
        WEEK = ourGraph.createDateFrameableTimeUnit("week", DAY, 7,
                new WeekFramerImpl());
        WEEK.setTextFormatter(new WeekTextFormatter(i18n.getText("week")
                + " {0}"));
        WEEK_AS_BOTTOM_UNIT = ourGraph.createDateFrameableTimeUnit("week", DAY,
                7, new WeekFramerImpl());
        WEEK_AS_BOTTOM_UNIT.setTextFormatter(new WeekTextFormatter("{0}"));
        QUARTER = ourGraph.createTimeUnitFunctionOfDate("quarter", MONTH,
                new FramerImpl(Calendar.MONTH));
        QUARTER.setTextFormatter(new QuarterTextFormatter());
        YEAR = ourGraph.createTimeUnitFunctionOfDate("year", DAY,
                new FramerImpl(Calendar.YEAR));
        YEAR.setTextFormatter(new YearTextFormatter());
    }

    public GPTimeUnitStack() {
        myPairs = new TimeUnitPair[] {
                new TimeUnitPair(WEEK, DAY, this, 65),
                new TimeUnitPair(WEEK, DAY, this, 55),
                new TimeUnitPair(MONTH, DAY, this, 44),
                new TimeUnitPair(MONTH, DAY, this, 34),
                new TimeUnitPair(MONTH, WEEK_AS_BOTTOM_UNIT, this, 24),
                new TimeUnitPair(MONTH, WEEK_AS_BOTTOM_UNIT, this, 21),
                new TimeUnitPair(YEAR, WEEK_AS_BOTTOM_UNIT, this, 13),
                new TimeUnitPair(YEAR, WEEK_AS_BOTTOM_UNIT, this, 8),
                new TimeUnitPair(YEAR, MONTH, this, 5),
                new TimeUnitPair(YEAR, MONTH, this, 3),
                /*
                 * The last pair is reused for the next steps,
                 * so it is needed only once.
                 */
                /*new TimeUnitPair(YEAR, QUARTER, this, 1)*/ };
    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public TimeUnit getDefaultTimeUnit() {
        return DAY;
    }

    @Override
    public TimeUnitPair[] getTimeUnitPairs() {
        return myPairs;
    }

    @Override
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

    @Override
    public DateFormat getTimeFormat() {
        if (HOUR.isConstructedFrom(getDefaultTimeUnit())) {
            return DateFormat.getTimeInstance(DateFormat.SHORT);
        }
        return null;
    }

    @Override
    public TimeUnit findTimeUnit(String code) {
        assert code != null;
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

    @Override
    public String encode(TimeUnit timeUnit) {
        if (timeUnit == HOUR) {
            return "h";
        }
        if (timeUnit == DAY) {
            return "d";
        }
        if (timeUnit == WEEK_AS_BOTTOM_UNIT) {
            return "w";
        }
        throw new IllegalArgumentException();
    }

    @Override
    public TaskLength createDuration(TimeUnit timeUnit, Date startDate, Date endDate) {
        TaskLength result;
        int sign = 1;
        if (endDate.before(startDate)) {
            sign = -1;
            Date temp = endDate;
            endDate = startDate;
            startDate = temp;
        }
        int unitCount = 0;
        for (; startDate.before(endDate); unitCount++) {
            startDate = timeUnit.adjustRight(startDate);
        }
        result = new TaskLengthImpl(timeUnit, unitCount*sign);
        return result;
    }
}
