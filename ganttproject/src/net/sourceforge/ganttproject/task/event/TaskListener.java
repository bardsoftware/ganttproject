package net.sourceforge.ganttproject.task.event;

import java.util.EventListener;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface TaskListener extends EventListener {

    void taskScheduleChanged(TaskScheduleEvent e);

    void dependencyAdded(TaskDependencyEvent e);

    void dependencyRemoved(TaskDependencyEvent e);

    void taskAdded(TaskHierarchyEvent e);

    void taskRemoved(TaskHierarchyEvent e);

    void taskMoved(TaskHierarchyEvent e);

    void taskPropertiesChanged(TaskPropertyEvent e);

    void taskProgressChanged(TaskPropertyEvent e);

    void taskModelReset();
}
