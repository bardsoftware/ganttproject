package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public interface MutableTaskDependencyCollection {
    void clear();

    /**
     * Creates a dependency with a
     * {@link net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl
     * FinishStartConstraintImpl} between the dependant and the dependee
     *
     * @param dependant
     *            task which is depending on dependee
     * @param dependee
     *            task where dependant is depending on
     * @return an object representing the dependency
     * @throws TaskDependencyException
     *             if the dependency is not allowed to be created (already
     *             created, results in looping, etc)
     */
    TaskDependency createDependency(Task dependant, Task dependee)
            throws TaskDependencyException;

    /**
     * Creates a dependency with the given constraint between the dependant and
     * the dependee
     *
     * @param dependant
     *            task which is depending on dependee
     * @param dependee
     *            task where dependant is depending on
     * @param constraint
     *            the {@link net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint
     *            constraint} between the two depending tasks
     *
     * @return an object representing the dependency
     * @throws TaskDependencyException
     *             if the dependency is not allowed to be created (already
     *             created, results in looping, etc)
     */
    TaskDependency createDependency(Task dependant, Task dependee,
            TaskDependencyConstraint constraint) throws TaskDependencyException;

    void deleteDependency(TaskDependency dependency);
}
