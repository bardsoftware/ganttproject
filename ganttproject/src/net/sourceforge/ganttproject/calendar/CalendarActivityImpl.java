/*
 * Created on 18.10.2004
 */
package net.sourceforge.ganttproject.calendar;

import java.util.Date;

/**
 * @author bard
 */
public class CalendarActivityImpl implements GPCalendarActivity {

    private final boolean isWorkingTime;

    private final Date myEndDate;

    private final Date myStartDate;

    public CalendarActivityImpl(Date startDate, Date endDate,
            boolean isWorkingTime) {
        myStartDate = startDate;
        myEndDate = endDate;
        this.isWorkingTime = isWorkingTime;
    }

    public Date getStart() {
        return myStartDate;
    }

    public Date getEnd() {
        return myEndDate;
    }

    public boolean isWorkingTime() {
        return isWorkingTime;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return (isWorkingTime() ? "Working time: " : "Holiday: ") + "["
                + getStart() + ", " + getEnd() + "]";
    }
}
