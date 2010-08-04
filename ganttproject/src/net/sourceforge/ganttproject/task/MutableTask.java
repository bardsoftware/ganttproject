package net.sourceforge.ganttproject.task;

import java.awt.Color;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.shape.ShapePaint;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 06.02.2004
 */
public interface MutableTask {
    void setName(String name);

    void setMilestone(boolean isMilestone);

    void setPriority(Task.Priority priority);

    void setStart(GanttCalendar start);

    void setEnd(GanttCalendar end);

    void setDuration(TaskLength length);

    void shift(TaskLength shift);

    void setCompletionPercentage(int percentage);

//    void setStartFixed(boolean isFixed);

//    void setFinishFixed(boolean isFixed);

    void setShape(ShapePaint shape);

    void setColor(Color color);

    void setNotes(String notes);

    void addNotes(String notes);

    void setExpand(boolean expand);

    /**
     * Sets the task as critical or not. The method is used be TaskManager after
     * having run a CriticalPathAlgorithm to set the critical tasks. When
     * painted, the tasks are rendered as critical using Task.isCritical(). So,
     * a task is set as critical only if critical path is displayed.
     * 
     * @param critical
     *            <code>true</code> if this is critical, <code>false</code>
     *            otherwise.
     */
    void setCritical(boolean critical);

    void setTaskInfo(TaskInfo taskInfo);
	
	void setProjectTask (boolean projectTask);

}
