/*
 * Created on 18.10.2004
 */
package net.sourceforge.ganttproject.test.task.calendar;

import java.util.Calendar;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.test.task.TaskTestCase;
import net.sourceforge.ganttproject.time.gregorian.GregorianTimeUnitStack;

/**
 * @author bard
 * @author Maarten Bezemer
 */
public class TestWeekendCalendar extends TaskTestCase {
    public void testTaskOverlappingWeekendIsTwoDaysShorter() {
        Task t = getTaskManager().createTask();
        t.setStart(newFriday());// Friday
        t.setEnd(newTuesday()); // Tuesday
        assertEquals("Unexpected length of task=" + t
                + " which overlaps weekend", 2f, t.getDuration().getLength(
                GregorianTimeUnitStack.DAY), 0.1);

        // Now run same test, but with weekends set to show only
        getTaskManager().getCalendar().setOnlyShowWeekends(true);
        // Recalculate task end with new setting
        t.setEnd(null);
        assertEquals("Unexpected length of task=" + t
                + " which overlaps weekend (should still be two)", 2f, t
                .getDuration().getLength(GregorianTimeUnitStack.DAY), 0.1);
        assertEquals("End day should have been changed to Sunday", Calendar.SUNDAY, t.getEnd().getDayWeek());
        
        // Now set day again to Tuesday and see whether the length now is 4 days
        t.setEnd(newTuesday()); // Tuesday        
        assertEquals("Unexpected length of task=" + t
                + " which overlaps weekend (which is set to show only)", 4f, t
                .getDuration().getLength(GregorianTimeUnitStack.DAY), 0.1);
    }

    protected TaskManager newTaskManager() {
        return TaskManager.Access.newInstance(null, new TaskManagerConfigImpl() {
            public GPCalendar getCalendar() {
                return myWeekendCalendar;
            }
        });
    }

    private WeekendCalendarImpl myWeekendCalendar = new WeekendCalendarImpl();

    public void testNoWeekendsButHasHolidays() {
        TaskManager mgr = TaskManager.Access.newInstance(null, new TaskManagerConfigImpl() {
            private WeekendCalendarImpl myNoWeekendsCalendar = new WeekendCalendarImpl();
            {
                for (int i=1; i<=7; i++) {
                    myNoWeekendsCalendar.setWeekDayType(i, GPCalendar.DayType.WORKING);
                }
                myNoWeekendsCalendar.setPublicHoliDayType(newMonday().getTime());
            }
            @Override
            public GPCalendar getCalendar() {
                return myNoWeekendsCalendar;
            }
        });
        Task t = mgr.createTask();
        t.setStart(newFriday());
        t.setEnd(newWendesday());
        assertEquals(4.0f, t.getDuration().getLength(GregorianTimeUnitStack.DAY));

    }
}
