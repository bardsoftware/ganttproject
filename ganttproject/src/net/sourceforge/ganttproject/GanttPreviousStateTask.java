/**
 * 
 */
package net.sourceforge.ganttproject;

import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.task.Task;

/**
 * @author nbohn
 * 
 */
public class GanttPreviousStateTask {

    private int myId;

    private GanttCalendar myStart;

    private int myDuration;

    private boolean isMilestone;

    private boolean hasNested;

    public GanttPreviousStateTask(int id, GanttCalendar start, int duration,
            boolean isMilestone, boolean hasNested) {
        myId = id;
        myStart = start;
        myDuration = duration;
        this.isMilestone = isMilestone;
        this.hasNested = hasNested;
    }

    public int getId() {
        return myId;
    }

    public GanttCalendar getStart() {
        return myStart;
    }

    public GanttCalendar getEnd(GPCalendar calendar) {
        int duration = myDuration;
        GanttCalendar end = myStart.newAdd(myDuration);
		for (int i = 0; i < duration; i++) {
			if (calendar.isNonWorkingDay(myStart.newAdd(i).getTime())) {
                end.add(1);
                duration++;
            }
        }
        return end;
    }
	

    public int getDuration() {
        return myDuration;
    }

    public boolean isMilestone() {
        return isMilestone;
    }

    public boolean hasNested() {
        return hasNested;
    }
}
