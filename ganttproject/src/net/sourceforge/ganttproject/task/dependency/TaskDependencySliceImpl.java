package net.sourceforge.ganttproject.task.dependency;

import java.util.List;
import net.sourceforge.ganttproject.task.Task;


public class TaskDependencySliceImpl implements TaskDependencySlice {
    private final Task myTask;

    private final TaskDependencyCollection myDependencyCollection;

    public TaskDependencySliceImpl(Task task,
            TaskDependencyCollection dependencyCollection) {
        myTask = task;
        myDependencyCollection = dependencyCollection;
    }

    @Override
    public TaskDependency[] toArray() {
        return myDependencyCollection.getDependencies(myTask);
    }

    @Override
    public void clear() {
        TaskDependency[] deps = toArray();
        for (int i = 0; i < deps.length; i++) {
            deps[i].delete();
        }
    }

    /** Unlinks only tasks that are selected and leaves links to not selected tasks. */
    @Override
    public void clear(List<Task> selection) {
        TaskDependency[] deps = toArray();
        for (int i = 0; i < deps.length; i++) {
            if (selection.contains(deps[i].getDependant()) && selection.contains(deps[i].getDependee())) {
                deps[i].delete();
            }
        }
    }

    @Override
    public boolean hasLinks(List<Task> selection) {
        TaskDependency[] deps = toArray();
        for (int i = 0; i < deps.length; i++) {
            if (selection.contains(deps[i].getDependant()) && selection.contains(deps[i].getDependee())) {
                return true;
            }
        }
        return false;
    }

    protected Task getTask() {
        return myTask;
    }

    protected TaskDependencyCollection getDependencyCollection() {
        return myDependencyCollection;
    }
}
