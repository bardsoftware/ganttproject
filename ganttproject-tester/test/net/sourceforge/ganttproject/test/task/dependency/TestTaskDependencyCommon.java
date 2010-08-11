package net.sourceforge.ganttproject.test.task.dependency;

import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.TaskDependencySlice;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TestTaskDependencyCommon extends TaskTestCase {

    public void testNewDependencyAppearsInCollections()
            throws TaskDependencyException {
        TaskManager taskMgr = getTaskManager();
        Task task1 = taskMgr.createTask(1);
        Task task2 = taskMgr.createTask(2);
        TaskDependency dep = taskMgr
                .getDependencyCollection()
                .createDependency(task1, task2, new FinishStartConstraintImpl());

        assertDependenciesCollectionContainsDependency(task1, dep);
        assertDependenciesCollectionContainsDependency(task2, dep);
    }

    public void testDeletedDependencyDisappearsFromCollections()
            throws TaskDependencyException {
        TaskManager taskMgr = getTaskManager();
        Task task1 = taskMgr.createTask(1);
        Task task2 = taskMgr.createTask(2);
        TaskDependency dep = taskMgr
                .getDependencyCollection()
                .createDependency(task1, task2, new FinishStartConstraintImpl());

        dep.delete();
        assertDependenciesCollectionDoesntContainDependency(task1, dep);
        assertDependenciesCollectionDoesntContainDependency(task2, dep);
    }

    public void testOneDependencyDeletionDoesntAffectOthers()
            throws TaskDependencyException {
        TaskManager taskMgr = getTaskManager();
        Task task1 = taskMgr.createTask(1);
        Task task2 = taskMgr.createTask(2);
        Task task3 = taskMgr.createTask(3);

        TaskDependency dep1 = taskMgr
                .getDependencyCollection()
                .createDependency(task1, task2, new FinishStartConstraintImpl());
        TaskDependency dep2 = taskMgr
                .getDependencyCollection()
                .createDependency(task1, task3, new FinishStartConstraintImpl());

        dep1.delete();
        assertDependenciesCollectionContainsDependency(task1, dep2);
    }

    public void testDependenciesAsDependantDoesntContainDependenciesAsDependee()
            throws TaskDependencyException {
        TaskManager taskMgr = getTaskManager();
        Task task1 = taskMgr.createTask(1);
        Task task2 = taskMgr.createTask(2);
        Task task3 = taskMgr.createTask(3);

        TaskDependency dep1 = taskMgr
                .getDependencyCollection()
                .createDependency(task1, task2, new FinishStartConstraintImpl());
        TaskDependency dep2 = taskMgr
                .getDependencyCollection()
                .createDependency(task2, task3, new FinishStartConstraintImpl());

        assertDependencySliceContainsDependency(task2
                .getDependenciesAsDependant(), dep2);
        assertDependencySliceDoesntContainDependency(task2
                .getDependenciesAsDependant(), dep1);
    }

    protected void assertDependenciesCollectionContainsDependency(Task task,
            TaskDependency dependency) {
        assertDependencySliceContainsDependency(task.getDependencies(),
                dependency);
    }

    protected void assertDependenciesCollectionDoesntContainDependency(
            Task task, TaskDependency dependency) {
        assertDependencySliceDoesntContainDependency(task.getDependencies(),
                dependency);
    }

    protected void assertDependencySliceContainsDependency(
            TaskDependencySlice slice, TaskDependency dependency) {
        Set<TaskDependency> deps = new HashSet<TaskDependency>(Arrays.asList(slice.toArray()));
        assertTrue("Dependency=" + dependency
                + " has not been found in dependency slice=" + slice, deps
                .contains(dependency));
    }

    protected void assertDependencySliceDoesntContainDependency(
            TaskDependencySlice slice, TaskDependency dependency) {
        Set<TaskDependency> deps = new HashSet<TaskDependency>(Arrays.asList(slice.toArray()));
        assertTrue("Dependency=" + dependency
                + " has been found in dependency slice=" + slice, !deps
                .contains(dependency));
    }

    public void testImpossibleToAddDependencyTwice()
            throws TaskDependencyException {
        TaskManager taskMgr = getTaskManager();
        Task task1 = taskMgr.createTask(1);
        Task task2 = taskMgr.createTask(2);
        taskMgr.getDependencyCollection().createDependency(task1, task2,
                new FinishStartConstraintImpl());
        TaskDependency dep2 = null;
        try {
            dep2 = taskMgr.getDependencyCollection().createDependency(task1,
                    task2, new FinishStartConstraintImpl());
        } catch (TaskDependencyException e) {
        }
        assertNull("Created the dependency between the same tasks twice!", dep2);
    }
}
