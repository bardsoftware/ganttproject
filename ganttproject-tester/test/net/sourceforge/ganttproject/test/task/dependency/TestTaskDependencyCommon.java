package net.sourceforge.ganttproject.test.task.dependency;

import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
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

    public void testFindDependencyByTask() throws Exception {
      TaskManager taskMgr = getTaskManager();
      Task task1 = taskMgr.createTask();
      Task task2 = taskMgr.createTask();
      Task task3 = taskMgr.createTask();

      TaskDependency dep12 = taskMgr.getDependencyCollection()
          .createDependency(task2, task1, new FinishStartConstraintImpl());
      TaskDependency dep13 = taskMgr.getDependencyCollection()
          .createDependency(task3, task1, new FinishStartConstraintImpl());

      assertEquals(dep12, task2.getDependenciesAsDependant().getDependency(task1));
      assertEquals(dep12, task1.getDependenciesAsDependee().getDependency(task2));
      assertNull(task2.getDependenciesAsDependant().getDependency(task3));
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

    public void testCreatingDependencyBetweenSuperAndSubTask() {
        /*
         * SuperTask
         *  Task 1
         *  Task 2
         *
         * Making a dependency between SuperTask and Task 1 should fail!
         * Making a dependency between SuperTask and Task 2 should fail! (Currently it does not!)
         */
        TaskManager taskMgr = getTaskManager();
        Task superTask = taskMgr.createTask(10);
        Task task1 = taskMgr.createTask(1);
        Task task2 = taskMgr.createTask(2);

        TaskContainmentHierarchyFacade taskHierarchy = getTaskManager().getTaskHierarchy();
        taskHierarchy.move(task1, superTask);
        taskHierarchy.move(task2, superTask);

        // Test whether it is possible to create a dependency between SuperTask and Task1
        boolean canCreate1 = taskMgr.getDependencyCollection().canCreateDependency(superTask, task1);
        assertFalse("Taskmanager thinks the dependency between SuperTask and Task1 can be created", canCreate1);
        boolean canCreate1b = taskMgr.getDependencyCollection().canCreateDependency(task1, superTask);
        assertFalse("Taskmanager thinks the dependency between Task1 and SuperTask can be created", canCreate1b);
        TaskDependency dep1 = null;
        try {
            dep1 = taskMgr.getDependencyCollection().createDependency(superTask, task1);
        } catch (TaskDependencyException e) {
        }
        assertNull("Created the dependency between the SuperTask and Task1!", dep1);

        // Test whether it is possible to create a dependency between SuperTask and Task2
        boolean canCreate2 = taskMgr.getDependencyCollection().canCreateDependency(superTask, task2);
        assertFalse("Taskmanager thinks the dependency between SuperTask and Task2 can be created", canCreate2);
        boolean canCreate2b = taskMgr.getDependencyCollection().canCreateDependency(task2, superTask);
        assertFalse("Taskmanager thinks the dependency between Task2 and SuperTask can be created", canCreate2b);
        TaskDependency dep2 = null;
        try {
            dep2 = taskMgr.getDependencyCollection().createDependency(superTask, task2);
        } catch (TaskDependencyException e) {
        }
        assertNull("Created the dependency between the SuperTask and Task2!", dep2);
    }

    public void testDefaultHardness() {
      TaskManager taskMgr = getTaskManager();
      Task task1 = taskMgr.createTask();
      Task task2 = taskMgr.createTask();
      TaskDependency strongDependency = taskMgr.getDependencyCollection().createDependency(task1, task2);
      assertEquals(TaskDependency.Hardness.STRONG, strongDependency.getHardness());

      taskMgr.getDependencyHardnessOption().setValue(TaskDependency.Hardness.RUBBER.toString());
      Task task3 = taskMgr.createTask();
      Task task4 = taskMgr.createTask();
      TaskDependency rubberDependency = taskMgr.getDependencyCollection().createDependency(task3, task4);
      assertEquals(TaskDependency.Hardness.RUBBER, rubberDependency.getHardness());
    }
}
