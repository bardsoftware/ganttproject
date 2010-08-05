package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TaskDependencySliceAsDependee extends TaskDependencySliceImpl {
    public TaskDependency[] toArray() {
        return getDependencyCollection().getDependenciesAsDependee(getTask());
    }

    public TaskDependencySliceAsDependee(Task task,
            TaskDependencyCollection dependencyCollection) {
        super(task, dependencyCollection);
    }
}
