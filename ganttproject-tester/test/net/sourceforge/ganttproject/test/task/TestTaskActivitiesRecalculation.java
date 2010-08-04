package net.sourceforge.ganttproject.test.task;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskMutator;

/**
 * @author bard
 */
public class TestTaskActivitiesRecalculation extends TaskTestCase {
    public void testRecalculateOnChangingDurationByMutator() {
        Task task = getTaskManager().createTask();
        {
            task.setStart(new GanttCalendar(2000, 0, 3));
            task.setDuration(getTaskManager().createLength(1));
            TaskActivity[] activities = task.getActivities();
            assertEquals("Unexpected length of activities", 1,
                    activities.length);
            assertEquals("Unexpected end of the las activity",
                    new GanttCalendar(2000, 0, 4).getTime(), activities[0]
                            .getEnd());
        }
        //
        {
            TaskMutator mutator = task.createMutator();
            mutator.setDuration(getTaskManager().createLength(2));
            TaskActivity[] activities = task.getActivities();
            assertEquals("Unexpected length of activities", 1,
                    activities.length);
            assertEquals("Unexpected end of the last activity",
                    new GanttCalendar(2000, 0, 5).getTime(), activities[0]
                            .getEnd());
        }
    }
    
    public void testRecalculateOnChangingStartByFixingDurationMutator() {
        Task task = getTaskManager().createTask();
        {
            task.setStart(new GanttCalendar(2000, 0, 3));
            task.setDuration(getTaskManager().createLength(3));
        }
        {
            TaskMutator mutator = task.createMutatorFixingDuration();
            mutator.setStart(new GanttCalendar(2000, 0, 4));
            mutator.commit();
            TaskActivity[] activities = task.getActivities();
            assertEquals("Unexpected length of activities", 1,
                    activities.length);
            assertEquals("Unexpected end of the last activity",
                    new GanttCalendar(2000, 0, 7).getTime(), activities[0]
                            .getEnd());
        }
    }
}
