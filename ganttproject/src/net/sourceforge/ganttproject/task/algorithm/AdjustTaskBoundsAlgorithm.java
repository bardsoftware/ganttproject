/*
 LICENSE:
 
 This program is free software; you can redistribute it and/or modify  
 it under the terms of the GNU General Public License as published by  
 the Free Software Foundation; either version 2 of the License, or     
 (at your option) any later version.                                   
 
 Copyright (C) 2004, GanttProject Development Team
 */
package net.sourceforge.ganttproject.task.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

/**
 * @author bard
 */
public abstract class AdjustTaskBoundsAlgorithm extends AlgorithmBase {
    public void run(Task task) {
        run(new Task[] { task });
    }

    public void run(Task[] tasks) {
        if (!isEnabled()) {
            return;
        }
        AlgorithmImpl algorithmImpl = new AlgorithmImpl();
        algorithmImpl.run(tasks);
    }

    public void adjustNestedTasks(Task supertask) throws TaskDependencyException {
        TaskContainmentHierarchyFacade containmentFacade = createContainmentFacade();
        List /*<Task>*/ nestedTasks = new ArrayList(Arrays.asList(containmentFacade.getNestedTasks(supertask)));
        if (nestedTasks.size()==0) {
            return;
        }
        SortTasksAlgorithm sortAlgorithm = new SortTasksAlgorithm();
        sortAlgorithm.sortTasksByStartDate(nestedTasks);
        Set modifiedTasks = new HashSet();
        for (int i=0; i<nestedTasks.size(); i++) {
            Task nextNested = (Task) nestedTasks.get(i);
            if (nextNested.getStart().getTime().before(supertask.getStart().getTime())) {
                TaskMutator mutator = nextNested.createMutatorFixingDuration();
                mutator.setStart(supertask.getStart());
                mutator.commit();
                //
                modifiedTasks.add(nextNested);
            }
            if (nextNested.getEnd().getTime().after(supertask.getEnd().getTime())) {
                TaskMutator mutator = nextNested.createMutatorFixingDuration();
                mutator.shift(supertask.getManager().createLength(supertask.getDuration().getTimeUnit(), nextNested.getEnd().getTime(), supertask.getEnd().getTime()));
                mutator.commit();
                //
                modifiedTasks.add(nextNested);
            }
        }
        run((Task[])modifiedTasks.toArray(new Task[0]));
        RecalculateTaskScheduleAlgorithm alg = new RecalculateTaskScheduleAlgorithm(this) {
            protected TaskContainmentHierarchyFacade createContainmentFacade() {
                return AdjustTaskBoundsAlgorithm.this.createContainmentFacade();
            }
        };
        alg.run(modifiedTasks);
    }
    protected abstract TaskContainmentHierarchyFacade createContainmentFacade();

    private class AlgorithmImpl {
		
    private Set myModifiedTasks = new HashSet();

    public void run(Task[] tasks) {
        HashSet taskSet = new HashSet(Arrays.asList(tasks));
        myModifiedTasks.addAll(taskSet);
        TaskContainmentHierarchyFacade containmentFacade = createContainmentFacade();
        while (!taskSet.isEmpty()) {
            recalculateSupertaskScheduleBottomUp(taskSet, containmentFacade);
            taskSet.clear();
            for (Iterator modified = myModifiedTasks.iterator(); modified
                    .hasNext();) {
                Task nextTask = (Task) modified.next();
                Task supertask = containmentFacade.getContainer(nextTask);
                if (supertask != null) {
                    taskSet.add(supertask);
                }
            }
            myModifiedTasks.clear();
        }
        myModifiedTasks.clear();
    }
    

    private void recalculateSupertaskScheduleBottomUp(Set supertasks,
            TaskContainmentHierarchyFacade containmentFacade) {
        for (Iterator it = supertasks.iterator(); it.hasNext();) {
            Task nextSupertask = (Task) it.next();
            recalculateSupertaskSchedule(nextSupertask, containmentFacade);
        }
    }

    private void recalculateSupertaskSchedule(final Task supertask,
            final TaskContainmentHierarchyFacade containmentFacade) {
        Task[] nested = containmentFacade.getNestedTasks(supertask);
        if (nested.length == 0) {
            return;
        }
        GanttCalendar maxEnd = null;
        GanttCalendar minStart = null;
        for (int i = 0; i < nested.length; i++) {
            Task nextNested = nested[i];
            GanttCalendar nextStart = nextNested.getStart();
            if (minStart == null || nextStart.compareTo(minStart) < 0) {
                minStart = nextStart;
            }
            GanttCalendar nextEnd = nextNested.getEnd();
            if (maxEnd == null || nextEnd.compareTo(maxEnd) > 0) {
                maxEnd = nextEnd;
            }
        }
        TaskMutator mutator = supertask.createMutator();
        if (minStart.compareTo(supertask.getStart()) != 0) {
        	//System.err.println("recalculating supertask="+supertask.getTaskID()+" start="+minStart);
            //modifyTaskStart(supertask, minStart);
        	mutator.setStart(minStart);
        	myModifiedTasks.add(supertask);
        }
        if (maxEnd.compareTo(supertask.getEnd()) != 0) {
            //modifyTaskEnd(supertask, maxEnd);
        	mutator.setEnd(maxEnd);
        	myModifiedTasks.add(supertask);
        }
        mutator.commit();
    }

	}    

}
