package net.sourceforge.ganttproject.task.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public abstract class RecalculateTaskScheduleAlgorithm extends AlgorithmBase {

    private Set myMarkedTasks = new HashSet();

    private SortedMap myDistance2dependencyList = new TreeMap();

    private Set myModifiedTasks = new HashSet();

    private final AdjustTaskBoundsAlgorithm myAdjuster;

    private int myEntranceCounter;

    private boolean isRunning;

    public RecalculateTaskScheduleAlgorithm(AdjustTaskBoundsAlgorithm adjuster) {
        myAdjuster = adjuster;
    }

    public void run(Task changedTask) throws TaskDependencyException {
        if (!isEnabled()) {
            return;
        }
        isRunning = true;
        myEntranceCounter++;
        myMarkedTasks.clear();
        buildDistanceGraph(changedTask);
        fulfilDependencies();
        myDistance2dependencyList.clear();
        myModifiedTasks.add(changedTask);
        myAdjuster.run((Task[]) myModifiedTasks.toArray(new Task[0]));
        myDistance2dependencyList.clear();
        myModifiedTasks.clear();
        myEntranceCounter--;

        isRunning = false;
    }

	public void run(Set taskSet) throws TaskDependencyException {
        if (!isEnabled()) {
            return;
        }
        isRunning = true;
        myEntranceCounter++;
        myMarkedTasks.clear();
        for (Iterator tasks = taskSet.iterator(); tasks.hasNext();) {
        	Task nextTask = (Task) tasks.next();
	        buildDistanceGraph(nextTask);
	        fulfilDependencies();
	        myDistance2dependencyList.clear();
	        myModifiedTasks.add(nextTask);
        }
        myAdjuster.run((Task[]) myModifiedTasks.toArray(new Task[0]));
        myDistance2dependencyList.clear();
        myModifiedTasks.clear();
        myEntranceCounter--;

        isRunning = false;

	}

    public void run() throws TaskDependencyException {
    	if (!isEnabled()) {
    		return;
    	}
    	myDistance2dependencyList.clear();
        isRunning = true;
        TaskContainmentHierarchyFacade facade = createContainmentFacade();
        Set independentTasks = new HashSet();
        traverse(facade, facade.getRootTask(), independentTasks);
        for (Iterator it = independentTasks.iterator(); it.hasNext();) {
            Task next = (Task) it.next();
            buildDistanceGraph(next);
        }
        fulfilDependencies();
        myDistance2dependencyList.clear();
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private void traverse(TaskContainmentHierarchyFacade facade, Task root,
            Set independentTasks) {
        TaskDependency[] asDependant = root.getDependenciesAsDependant()
                .toArray();
        if (asDependant.length == 0) {
            independentTasks.add(root);
        }
        Task[] nestedTasks = facade.getNestedTasks(root);
        for (int i = 0; i < nestedTasks.length; i++) {
            traverse(facade, nestedTasks[i], independentTasks);
        }
    }

    private void fulfilDependencies() throws TaskDependencyException {
        // System.err.println("[RecalculateTaskSchedule]
        // >>>fulfilDependencies()");
        for (Iterator distances = myDistance2dependencyList.entrySet()
                .iterator(); distances.hasNext();) {
            Map.Entry nextEntry = (Map.Entry) distances.next();
            List nextDependenciesList = (List) nextEntry.getValue();
            for (int i = 0; i < nextDependenciesList.size(); i++) {
                TaskDependency nextDependency = (TaskDependency) nextDependenciesList
                        .get(i);
                TaskDependencyConstraint nextConstraint = nextDependency
                        .getConstraint();
                TaskDependencyConstraint.Collision collision = nextConstraint
                        .getCollision();
                if (collision.isActive()) {
                    fulfilConstraints(nextDependency);
                    nextDependency.getDependant().applyThirdDateConstraint();
                }
            }
        }
        // System.err.println("[RecalculateTaskSchedule]
        // <<<fulfilDependencies()");
    }

    private void fulfilConstraints(TaskDependency dependency)
            throws TaskDependencyException {
        Task dependant = dependency.getDependant();
        TaskDependency[] depsAsDependant = dependant
                .getDependenciesAsDependant().toArray();
        if (depsAsDependant.length > 0) {
            ArrayList startLaterVariations = new ArrayList();
            ArrayList startEarlierVariations = new ArrayList();
            ArrayList noVariations = new ArrayList();
            //
            for (int i = 0; i < depsAsDependant.length; i++) {
                TaskDependency next = depsAsDependant[i];
                TaskDependencyConstraint.Collision nextCollision = next
                        .getConstraint().getCollision();
                GanttCalendar acceptableStart = nextCollision
                        .getAcceptableStart();
                switch (nextCollision.getVariation()) {
                case TaskDependencyConstraint.Collision.START_EARLIER_VARIATION: {
                    startEarlierVariations.add(acceptableStart);
                    break;
                }
                case TaskDependencyConstraint.Collision.START_LATER_VARIATION: {
                    startLaterVariations.add(acceptableStart);
                    break;
                }
                case TaskDependencyConstraint.Collision.NO_VARIATION: {
                    noVariations.add(acceptableStart);
                    break;
                }
                }
            }
            if (noVariations.size() > 1) {
                throw new TaskDependencyException(
                        "Failed to fulfill constraints of task="
                                + dependant
                                + ". There are "
                                + noVariations.size()
                                + " constraints which don't allow for task start variation");
            }
            //
            Collections.sort(startEarlierVariations, GanttCalendar.COMPARATOR);
            Collections.sort(startLaterVariations, GanttCalendar.COMPARATOR);
            //
            GanttCalendar solution;
            GanttCalendar earliestStart = (GanttCalendar) (startEarlierVariations
                    .size() == 0 ? null : startEarlierVariations.get(0));
            GanttCalendar latestStart = (GanttCalendar) (startLaterVariations
                    .size() >= 0 ? startLaterVariations
                    .get(startLaterVariations.size() - 1) : null);
            if (earliestStart == null && latestStart == null) {
                solution = dependant.getStart();
            } else {
                if (earliestStart == null && latestStart != null) {
                    earliestStart = latestStart;
                } else if (earliestStart != null && latestStart == null) {
                    latestStart = earliestStart;
                }
                if (earliestStart.compareTo(latestStart) < 0) {
                    throw new TaskDependencyException(
                            "Failed to fulfill constraints of task="
                                    + dependant);
                }
            }
            if (noVariations.size() > 0) {
                GanttCalendar notVariableStart = (GanttCalendar) noVariations
                        .get(0);
                if (notVariableStart.compareTo(earliestStart) < 0
                        || notVariableStart.compareTo(latestStart) > 0) {
                    throw new TaskDependencyException(
                            "Failed to fulfill constraints of task="
                                    + dependant);
                }
                solution = notVariableStart;
            } else {
                solution = latestStart;
            }
            //
            modifyTaskStart(dependant, solution);
        }
    }

    private void modifyTaskStart(Task task, GanttCalendar newStart) {
        TaskMutator mutator = task.createMutatorFixingDuration();
        mutator.setStart(newStart);
        mutator.commit();
        myModifiedTasks.add(task);
    }

    private void buildDistanceGraph(Task changedTask) {
        TaskDependency[] depsAsDependee = changedTask
                .getDependenciesAsDependee().toArray();
        buildDistanceGraph(depsAsDependee, 1);
    }

    private void buildDistanceGraph(TaskDependency[] deps, int distance) {
        if (deps.length == 0) {
            return;
        }
        Integer key = new Integer(distance);
        List depsList = (List) myDistance2dependencyList.get(key);
        if (depsList == null) {
            depsList = new ArrayList();
            myDistance2dependencyList.put(key, depsList);
        }
        depsList.addAll(Arrays.asList(deps));
        for (int i = 0; i < deps.length; i++) {
            Task dependant = deps[i].getDependant();
            TaskDependency[] nextStepDeps = dependant
                    .getDependenciesAsDependee().toArray();
            buildDistanceGraph(nextStepDeps, ++distance);
        }
    }

    protected abstract TaskContainmentHierarchyFacade createContainmentFacade();

}
