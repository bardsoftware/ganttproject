package net.sourceforge.ganttproject.task.event;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public abstract class TaskListenerAdapter implements TaskListener {
    public void taskScheduleChanged(TaskScheduleEvent e) {
    }

    public void dependencyAdded(TaskDependencyEvent e) {
    }

    public void dependencyRemoved(TaskDependencyEvent e) {
    }

    public void taskAdded(TaskHierarchyEvent e) {
    }

    public void taskRemoved(TaskHierarchyEvent e) {
    }

    public void taskMoved(TaskHierarchyEvent e) {
    }

    public void taskPropertiesChanged(TaskPropertyEvent e) {
    }

    public void taskProgressChanged(TaskPropertyEvent e) {
    }

    public void taskModelReset() {
    }
    
}
