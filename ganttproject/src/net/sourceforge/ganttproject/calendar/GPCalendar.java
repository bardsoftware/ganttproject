/*
 * Created on 18.10.2004
 */
package net.sourceforge.ganttproject.calendar;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
 */
public interface GPCalendar {
    List<GPCalendarActivity> getActivities(Date startDate, Date endDate);

    List<GPCalendarActivity>  getActivities(Date startDate, TimeUnit timeUnit, long l);

    void setWeekDayType(int day, DayType type);

    DayType getWeekDayType(int day);

    /** 
     * @return true when weekends are only shown and taken into
     *  account for the task scheduling.
     */
    public boolean getOnlyShowWeekends();

    /** 
     * @param onlyShowWeekends must be set to true if weekends are
     *  only shown and not taken into account for the task scheduling
     */
    public void setOnlyShowWeekends(boolean onlyShowWeekends);

    void setPublicHoliDayType(int month, int date);

    public void setPublicHoliDayType(Date curDayStart);

    public boolean isPublicHoliDay(Date curDayStart);

    public boolean isNonWorkingDay(Date curDayStart);

    public DayType getDayTypeDate(Date curDayStart);

    public void setPublicHolidays(URL calendar, GanttProject gp);

    public Collection<Date> getPublicHolidays();

    final class DayType {
        public static final DayType WORKING = new DayType();

        public static final DayType WEEKEND = new DayType();

        public static final DayType HOLIDAY = new DayType();
    }

    Date findClosestWorkingTime(Date time);

    /**
     * Adds <code>shift</code> period to <code>input</code> date taking into
     * account this calendar working/non-working time If input date corresponds
     * to Friday midnight and this calendar if configured to have a weekend on
     * Saturday and Sunday then adding a shift of "1 day" will result to the
     * midnight of the next Monday
     */
    Date shiftDate(Date input, TaskLength shift);

    GPCalendar PLAIN = new AlwaysWorkingTimeCalendarImpl();
    String EXTENSION_POINT_ID = "net.sourceforge.ganttproject.calendar";

}
