/*
 * Created on 18.10.2004
 */
package net.sourceforge.ganttproject.test.task.calendar;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.test.task.TaskTestCase;
import net.sourceforge.ganttproject.time.gregorian.GregorianTimeUnitStack;

/**
 * @author bard
 */
public class TestWeekendCalendar extends TaskTestCase {
    public void testTaskOverlappingWeekendIsTwoDaysShorter() {
        Task t = getTaskManager().createTask();
        t.setStart(newFriday());// Friday
        t.setEnd(newTuesday()); // Tuesday
        assertEquals("Unexpected length of task=" + t
                + " which overlaps weekend", 2f, t.getDuration().getLength(
                GregorianTimeUnitStack.DAY), 0.1);
    }

    protected TaskManager newTaskManager() {
        return TestSetupHelper.newTaskManagerBuilder().withCalendar(myWeekendCalendar).build();
    }

    private WeekendCalendarImpl myWeekendCalendar = new WeekendCalendarImpl();

    public void testNoWeekendsButHasHolidays() {
        WeekendCalendarImpl noWeekendsOneHolidayCalendar = new WeekendCalendarImpl();
        for (int i=1; i<=7; i++) {
            noWeekendsOneHolidayCalendar.setWeekDayType(i, GPCalendar.DayType.WORKING);
        }
        noWeekendsOneHolidayCalendar.setPublicHoliDayType(newMonday().getTime());
        TaskManager mgr = TestSetupHelper.newTaskManagerBuilder().withCalendar(noWeekendsOneHolidayCalendar).build();
        Task t = mgr.createTask();
        t.setStart(newFriday());
        t.setEnd(newWendesday());
        assertEquals(4.0f, t.getDuration().getLength(GregorianTimeUnitStack.DAY));

    }
}
