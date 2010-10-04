package net.sourceforge.ganttproject.task.dependency;

import java.util.List;

import net.sourceforge.ganttproject.task.Task;

public interface TaskDependencySlice {
    TaskDependency[] toArray();
    
    void clear();
    void clear(List<Task> selection);
}
