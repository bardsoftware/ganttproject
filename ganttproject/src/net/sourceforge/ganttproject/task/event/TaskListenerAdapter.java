package net.sourceforge.ganttproject.task.event;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public abstract class TaskListenerAdapter implements TaskListener {
    @Override
    public void taskScheduleChanged(TaskScheduleEvent e) {
    }

    @Override
    public void dependencyAdded(TaskDependencyEvent e) {
    }

    @Override
    public void dependencyRemoved(TaskDependencyEvent e) {
    }

    @Override
    public void taskAdded(TaskHierarchyEvent e) {
    }

    @Override
    public void taskRemoved(TaskHierarchyEvent e) {
    }

    @Override
    public void taskMoved(TaskHierarchyEvent e) {
    }

    @Override
    public void taskPropertiesChanged(TaskPropertyEvent e) {
    }

    @Override
    public void taskProgressChanged(TaskPropertyEvent e) {
    }

    @Override
    public void taskModelReset() {
    }

}
