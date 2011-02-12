package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.GanttCalendar;

/**
 * @author bard Date: 27.01.2004
 */
public interface TaskMutator extends MutableTask {
    int READ_UNCOMMITED = 0;

    int READ_COMMITED = 1;

    void setIsolationLevel(int level);

    void commit();

    void shift(float unitCount);

    int getCompletionPercentage();

    void setThird(GanttCalendar third, int thirdDateConstraint);
}
