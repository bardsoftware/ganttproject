package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard Date: 14.02.2004 Time: 15:42:21 To
 * change this template use File | Settings | File Templates.
 */
public interface TaskDependencyCollection extends
        MutableTaskDependencyCollection {

    // void addDependency(TaskDependency dep) throws TaskDependencyException;
    // void removeDependency(TaskDependency dep);
    TaskDependency[] getDependencies();

    TaskDependency[] getDependencies(Task task);

    TaskDependency[] getDependenciesAsDependant(Task dependant);

    TaskDependency[] getDependenciesAsDependee(Task dependee);

    TaskDependencyCollectionMutator createMutator();

    boolean canCreateDependency(Task myDependant, Task dependee);
}
