package net.sourceforge.ganttproject.delay;

import net.sourceforge.ganttproject.task.Task;

/**
 * @author bbaranne
 */
public interface DelayObserver {
    public void setDelay(Task task, Delay delay);
}
