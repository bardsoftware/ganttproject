/*
 * Created on 09.11.2004
 */
package net.sourceforge.ganttproject.time;

import net.sourceforge.ganttproject.task.TaskLength;

import java.text.DateFormat;
import java.util.Date;

/**
 * @author bard
 */
public interface TimeUnitStack {
    TimeUnit getDefaultTimeUnit();

    TimeFrame createTimeFrame(Date startDate, TimeUnit topUnit,
            TimeUnit bottomUnit);

    TimeUnitPair[] getTimeUnitPairs();

    String getName();

    DateFormat[] getDateFormats();
    DateFormat getTimeFormat();
    TimeUnit findTimeUnit(String code);
    String encode(TimeUnit timeUnit);
    TaskLength createDuration(TimeUnit timeUnit, Date startDate, Date endDate);
}
