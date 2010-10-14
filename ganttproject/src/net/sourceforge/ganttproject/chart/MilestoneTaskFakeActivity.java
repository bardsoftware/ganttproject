package net.sourceforge.ganttproject.chart;

import java.util.Date;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskLength;

class MilestoneTaskFakeActivity implements TaskActivity {
    private final Task myTask;

    MilestoneTaskFakeActivity(Task task) {
        assert task.isMilestone();
        myTask = task;
    }

    public TaskLength getDuration() {
        return myTask.getManager().createLength(1);
    }

    public Date getEnd() {
        return myTask.getEnd().getTime();
    }

    public float getIntensity() {
        return 1;
    }

    public Date getStart() {
        return myTask.getStart().getTime();
    }

    public Task getTask() {
        return myTask;
    }

    public boolean isFirst() {
        return true;
    }

    public boolean isLast() {
        return true;
    }

    @Override
    public String toString() {
        return "Milestone activity ["+getStart()+"-"+getEnd()+"]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MilestoneTaskFakeActivity) {
            return ((MilestoneTaskFakeActivity)obj).myTask.equals(myTask);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return myTask.hashCode();
    }


}