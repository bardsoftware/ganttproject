package net.sourceforge.ganttproject.task.dependency;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface EventDispatcher {
    void fireDependencyAdded(TaskDependency dep);

    void fireDependencyRemoved(TaskDependency dep);
}
