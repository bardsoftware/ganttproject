package net.sourceforge.ganttproject.task.dependency;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface TaskDependencyCollectionMutator extends
        MutableTaskDependencyCollection {
    void commit();
}
