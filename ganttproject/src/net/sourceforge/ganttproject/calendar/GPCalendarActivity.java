/*
 * Created on 18.10.2004
 */
package net.sourceforge.ganttproject.calendar;

import java.util.Date;

/**
 * @author bard
 */
public interface GPCalendarActivity {
    Date getStart();

    Date getEnd();

    boolean isWorkingTime();
}
