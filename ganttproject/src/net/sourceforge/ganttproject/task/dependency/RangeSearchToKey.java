package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.Task;

public class RangeSearchToKey extends SearchKey {
    public RangeSearchToKey(Task task) {
        super(3, task.getTaskID(), 0);
    }
}
