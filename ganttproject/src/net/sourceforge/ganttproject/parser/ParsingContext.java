package net.sourceforge.ganttproject.parser;

import java.util.HashSet;
import java.util.Set;

import net.sourceforge.ganttproject.task.Task;

public class ParsingContext {
    public int getTaskID() {
        return myTaskID;
    }

    public void setTaskID(int id) {
        myTaskID = id;
    }

    void addTaskWithLegacyFixedStart(Task task) {
    	myFixedStartTasks.add(task);
    }
    
    Set getTasksWithLegacyFixedStart() {
    	return myFixedStartTasks;
    }
    
    private final Set myFixedStartTasks = new HashSet();
    private int myTaskID;
}
