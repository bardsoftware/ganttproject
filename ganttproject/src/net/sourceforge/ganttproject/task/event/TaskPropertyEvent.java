package net.sourceforge.ganttproject.task.event;

import java.util.EventObject;

import net.sourceforge.ganttproject.task.Task;

public class TaskPropertyEvent extends EventObject {

    private Task myTask;

    public TaskPropertyEvent(Object source) {
        super(source);
        myTask = (Task) source;
    }

    public Task getTask() {
        return myTask;
    }

}
