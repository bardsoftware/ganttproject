/*
 * Created on 09.11.2004
 */
package net.sourceforge.ganttproject.time;

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
}
