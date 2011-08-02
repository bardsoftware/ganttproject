package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.Task;

class RangeSearchFromKey extends SearchKey {
    public RangeSearchFromKey(Task task) {
        super(-1, task.getTaskID(), 0);
    }
}
