package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TaskDependencySliceAsDependant extends TaskDependencySliceImpl {
    @Override
    public TaskDependency[] toArray() {
        return getDependencyCollection().getDependenciesAsDependant(getTask());
    }

    public TaskDependencySliceAsDependant(Task task,
            TaskDependencyCollection dependencyCollection) {
        super(task, dependencyCollection);
    }
}
