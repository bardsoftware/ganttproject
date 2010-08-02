/*
 * Created on 18.10.2004
 */
package net.sourceforge.ganttproject.calendar;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.parser.HolidayTagHandler;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.gregorian.FramerImpl;

/**
 * @author bard
 */
public class WeekendCalendarImpl extends GPCalendarBase implements GPCalendar {

    private final Calendar myCalendar = CalendarFactory.newCalendar();

    private final FramerImpl myFramer = new FramerImpl(Calendar.DAY_OF_WEEK);

    private DayType[] myTypes = new DayType[7];
    
    private boolean myOnlyShowWeekends = false;

    private int myWeekendDaysCount;

    private Set publicHolidaysArray = new LinkedHashSet();

    private final Set myStableHolidays = new LinkedHashSet();

    private AlwaysWorkingTimeCalendarImpl myRestlessCalendar = new AlwaysWorkingTimeCalendarImpl();

    public WeekendCalendarImpl() {
        for (int i = 0; i < myTypes.length; i++) {
            myTypes[i] = GPCalendar.DayType.WORKING;
        }
        setWeekDayType(GregorianCalendar.SATURDAY, GPCalendar.DayType.WEEKEND);
        setWeekDayType(GregorianCalendar.SUNDAY, GPCalendar.DayType.WEEKEND);
    }

    public List/* <GPCalendarActivity> */getActivities(Date startDate,
            final Date endDate) {
        if (getWeekendDaysCount() == 0 && publicHolidaysArray.isEmpty() && myStableHolidays.isEmpty()) {
            return myRestlessCalendar.getActivities(startDate, endDate);
        }
        List result = new ArrayList();
        Date curDayStart = myFramer.adjustLeft(startDate);
        boolean isWeekendState = isNonWorkingDay(curDayStart);
        // System.err.println("getActivities(): start="+startDate+"
        // end="+endDate);
        while (curDayStart.before(endDate)) {
            // System.err.println("curDayStart="+curDayStart);
            Date changeStateDayStart = getStateChangeDate(curDayStart, endDate, !isWeekendState);
            // System.err.println("changeStateDayStart="+changeStateDayStart);
            if (changeStateDayStart.before(endDate)) {
                result.add(new CalendarActivityImpl(curDayStart,
                        changeStateDayStart, !isWeekendState));
                curDayStart = changeStateDayStart;
                isWeekendState = !isWeekendState;
                continue;
            } else {
                result.add(new CalendarActivityImpl(curDayStart, endDate,
                        !isWeekendState));
                break;
            }
        }
        return result;

    }

    public boolean isWeekend(Date curDayStart) {
        if(myOnlyShowWeekends)
            return false;

        myCalendar.setTime(curDayStart);
        int dayOfWeek = myCalendar.get(Calendar.DAY_OF_WEEK);
        return myTypes[dayOfWeek - 1] == GPCalendar.DayType.WEEKEND;
    }

    private Date getStateChangeDate(Date startDate, Date limitDate, boolean changeToWeekend) {
        Date nextDayStart = myFramer.adjustRight(startDate);
        if (!(changeToWeekend ^ isNonWorkingDay(nextDayStart))) {
            return nextDayStart;
        } else {
            if (getWeekendDaysCount() > 0 || limitDate != null && limitDate.compareTo(nextDayStart) > 0) {
                return getStateChangeDate(nextDayStart, limitDate, changeToWeekend);
            }
            else {
                return limitDate;
            }
        }
    }

    private Date getStateChangeDate(Date startDate, TimeUnit timeUnit,
            boolean changeToWeekend, boolean moveRightNotLeft) {
        Date nextUnitStart = moveRightNotLeft ? timeUnit.adjustRight(startDate)
                : timeUnit.jumpLeft(startDate);
        if (!(changeToWeekend ^ isNonWorkingDay(nextUnitStart))) {
            return nextUnitStart;
        } else {

            return getStateChangeDate(nextUnitStart, timeUnit, changeToWeekend,
                    moveRightNotLeft);
        }

    }

    protected List getActivitiesForward(Date startDate, TimeUnit timeUnit,
            long unitCount) {
        List result = new ArrayList();
        Date unitStart = timeUnit.adjustLeft(startDate);
        while (unitCount > 0) {
            boolean isWeekendState = isNonWorkingDay(unitStart);
            if (isWeekendState) {
                Date workingUnitStart = getStateChangeDate(unitStart, timeUnit,
                        false, true);
                result.add(new CalendarActivityImpl(unitStart,
                        workingUnitStart, false));
                unitStart = workingUnitStart;
                continue;
            }
            Date nextUnitStart = timeUnit.adjustRight(unitStart);
            result
                    .add(new CalendarActivityImpl(unitStart, nextUnitStart,
                            true));
            unitStart = nextUnitStart;
            unitCount--;
        }
        return result;
    }

    protected List getActivitiesBackward(Date startDate, TimeUnit timeUnit,
            long unitCount) {
        List result = new LinkedList();
        Date unitStart = timeUnit.adjustLeft(startDate);
        while (unitCount > 0) {
            Date prevUnitStart = timeUnit.jumpLeft(unitStart);
            boolean isWeekendState = isNonWorkingDay(prevUnitStart);
            if (isWeekendState) {
                Date lastWorkingUnitStart = getStateChangeDate(prevUnitStart,
                        timeUnit, false, false);
                Date firstWeekendUnitStart = timeUnit
                        .adjustRight(lastWorkingUnitStart);
                Date lastWeekendUnitEnd = unitStart;
                result.add(0, new CalendarActivityImpl(firstWeekendUnitStart,
                        lastWeekendUnitEnd, false));
                unitStart = firstWeekendUnitStart;
            } else {
                result.add(0, new CalendarActivityImpl(prevUnitStart,
                        unitStart, true));
                unitCount--;
                unitStart = prevUnitStart;
            }
        }
        return result;
    }

    public void setWeekDayType(int day, DayType type) {
        if (type != myTypes[day - 1]) {
            myWeekendDaysCount += (type == DayType.WEEKEND ? 1 : -1);
        }
        myTypes[day - 1] = type;
    }

    public DayType getWeekDayType(int day) {
        return myTypes[day - 1];
    }
    
    public boolean getOnlyShowWeekends() {
        return myOnlyShowWeekends;
    }
    
    public void setOnlyShowWeekends(boolean onlyShowWeekends) {
        myOnlyShowWeekends = onlyShowWeekends;
    }
    
    private int getWeekendDaysCount()
    {
        if(myOnlyShowWeekends)
            return 0;
        else
            return myWeekendDaysCount;
    }

    public Date findClosestWorkingTime(Date time) {
        if (getWeekendDaysCount() == 0) {
            return time;
        }
        if (!isNonWorkingDay(time)) {
            return time;
        }
        return getStateChangeDate(time, null, false);
    }

    public void setPublicHoliDayType(int month, int date) {
        setPublicHoliDayType(new GanttCalendar(1, month - 1, date).getTime());
        myStableHolidays.add(new GanttCalendar(1, month - 1, date).getTime());
    }

    public void setPublicHoliDayType(Date curDayStart) {
        publicHolidaysArray.add(curDayStart);
    }

    public boolean isPublicHoliDay(Date curDayStart) {
        boolean result = publicHolidaysArray.contains(curDayStart);
        if (!result) {
            result = myStableHolidays.contains(new GanttCalendar(1, curDayStart.getMonth(), curDayStart.getDate()).getTime());
        }
        return result;
    }

    public DayType getDayTypeDate(Date curDayStart) {
        myCalendar.setTime(curDayStart);
        int dayOfWeek = myCalendar.get(Calendar.DAY_OF_WEEK);
        if (isPublicHoliDay(curDayStart))
            return GPCalendar.DayType.HOLIDAY;
        else if (getWeekDayType(dayOfWeek) == GPCalendar.DayType.WORKING)
            return GPCalendar.DayType.WORKING;
        else
            return GPCalendar.DayType.WEEKEND;
    }

    public boolean isNonWorkingDay(Date curDayStart) {
        if (isWeekend(curDayStart) || isPublicHoliDay(curDayStart))
            return true;
        else
            return false;
    }

    public void setPublicHolidays(URL calendar, GanttProject gp) {
        publicHolidaysArray.clear();
        if (calendar != null) {
            XMLCalendarOpen opener = new XMLCalendarOpen();

            HolidayTagHandler dependencyHandler = new HolidayTagHandler(gp);

            opener.addTagHandler(dependencyHandler);
            opener.addParsingListener(dependencyHandler);
            try {
                opener.load(calendar.openStream());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public Collection getPublicHolidays() {
        return publicHolidaysArray;
    }

    public List getActivities(Date startingFrom, TaskLength period) {
        return getActivities(startingFrom, period.getTimeUnit(), period
                .getLength());
    }

}
