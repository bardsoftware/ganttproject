/*
 * Created on 31.10.2004
 */
package net.sourceforge.ganttproject.test.task.calendar;

import biz.ganttproject.core.time.impl.GregorianTimeUnitStack;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;

/**
 * @author bard
 */
public class TestSetLength extends TestWeekendCalendar {
  public void testTaskStartingOnFridayLastingTwoDaysEndsOnTuesday() {
    Task t = getTaskManager().newTaskBuilder()
        .withStartDate(TestSetupHelper.newFriday().getTime())
        .build();
    t.setDuration(getTaskManager().createLength(GregorianTimeUnitStack.DAY,
        2));
    assertEquals(
        "unXpected end of task which starts on friday and is 2 days long",
        TestSetupHelper.newTuesday(), t.getEnd());
  }

  public void testTaskStartingOnSaturdayLastingOneDayEndsOnTuesday() {
    Task t = getTaskManager().newTaskBuilder()
        .withStartDate(TestSetupHelper.newSaturday().getTime())
        .build();
    t.setDuration(getTaskManager().createLength(GregorianTimeUnitStack.DAY,
        1));
    assertEquals(
        "unXpected end of task which starts on saturday and is 1 day long",
        TestSetupHelper.newTuesday(), t.getEnd());
  }

  public void testTaskStartingOnSundayLastingOneDayEndsOnTuesday() {
    Task t = getTaskManager().newTaskBuilder()
        .withStartDate(TestSetupHelper.newSunday().getTime())
        .build();
    t.setDuration(getTaskManager().createLength(GregorianTimeUnitStack.DAY,
        1));
    assertEquals(
        "unXpected end of task which starts on sunday and is 1 day long",
        TestSetupHelper.newTuesday(), t.getEnd());

  }

}
