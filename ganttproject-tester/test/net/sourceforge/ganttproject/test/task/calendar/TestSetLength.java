/*
 * Created on 31.10.2004
 */
package net.sourceforge.ganttproject.test.task.calendar;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.time.gregorian.GregorianTimeUnitStack;

/**
 * @author bard
 */
public class TestSetLength extends TestWeekendCalendar {
    public void testTaskStartingOnFridayLastingTwoDaysEndsOnTuesday() {
        Task t = getTaskManager().createTask();
        t.setStart(newFriday());
        t.setDuration(getTaskManager().createLength(GregorianTimeUnitStack.DAY,
                2));
        assertEquals(
                "unXpected end of task which starts on friday and is 2 days long",
                newTuesday(), t.getEnd());
    }

    public void testTaskStartingOnSaturdayLastingOneDayEndsOnTuesday() {
        Task t = getTaskManager().createTask();
        t.setStart(newSaturday());
        t.setDuration(getTaskManager().createLength(GregorianTimeUnitStack.DAY,
                1));
        assertEquals(
                "unXpected end of task which starts on saturday and is 1 day long",
                newTuesday(), t.getEnd());
    }

    public void testTaskStartingOnSundayLastingOneDayEndsOnTuesday() {
        Task t = getTaskManager().createTask();
        t.setStart(newSunday());
        t.setDuration(getTaskManager().createLength(GregorianTimeUnitStack.DAY,
                1));
        assertEquals(
                "unXpected end of task which starts on sunday and is 1 day long",
                newTuesday(), t.getEnd());

    }

}
