package net.sourceforge.ganttproject.task.dependency;

import java.util.List;

public interface TaskDependencySlice {
    TaskDependency[] toArray();
    
    void clear();
    void clear(List selection);
}
