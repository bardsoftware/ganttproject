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

    public static final int NORMAL = 0;

    public static final int LATER = 1;

    public static final int EARLIER = 2;

    private int myId;

    private GanttCalendar myStart;

    private int myDuration;

    private boolean isMilestone;

    private boolean hasNested;

    private int myState = NORMAL;
	
	//to know if the previousTask
	//has to be completely displayed
	private boolean isAPart = false;

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
//			if (myId == 1)
//				System.out.println (myStart.newAdd(i).getTime() + " is workingDay : " + calendar.isNonWorkingDay(myStart.newAdd(i).getTime()));
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

    public void setState(int state) {
        myState = state;
    }
	
    public int getState() {
        return myState;
    }
	
	public void setState (Task task, GPCalendar calendar) {
		if (task.getEnd().before(getEnd(calendar)))
			setState(GanttPreviousStateTask.EARLIER);
		else if (task.getEnd().after(getEnd(calendar)))
			setState(GanttPreviousStateTask.LATER);
		else
			setState(GanttPreviousStateTask.NORMAL);
	}
	public void setIsAPart (boolean isAPart) {
		this.isAPart = isAPart;
	}
	public boolean isAPart () {
		return isAPart;
	}
}
