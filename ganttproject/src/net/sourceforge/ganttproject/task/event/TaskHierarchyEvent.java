package net.sourceforge.ganttproject.task.event;

import java.util.EventObject;

import net.sourceforge.ganttproject.task.Task;

public class TaskHierarchyEvent extends EventObject {
    private final Task myNewContainer;

    private final Task myTask;

    private final Task myOldContainer;

    public TaskHierarchyEvent(Object source, Task myTask, Task myOldContainer,
            Task myNewContainer) {
        super(source);
        this.myNewContainer = myNewContainer;
        this.myTask = myTask;
        this.myOldContainer = myOldContainer;
    }

    public Task getTask() {
        return myTask;
    }

    public Task getOldContainer() {
        return myOldContainer;
    }

    public Task getNewContainer() {
        return myNewContainer;
    }
}
