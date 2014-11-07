/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.task.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import biz.ganttproject.core.time.GanttCalendar;

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
    run(Arrays.asList(tasks));
  }


  @Override
  protected boolean isEnabled() {
    return false;
  }

  public void run(Collection<Task> tasks) {
    if (!isEnabled()) {
      return;
    }
    AlgorithmImpl algorithmImpl = new AlgorithmImpl();
    algorithmImpl.run(tasks);
  }

  public void adjustNestedTasks(Task supertask) throws TaskDependencyException {
    if (!isEnabled()) {
      return;
    }
    TaskContainmentHierarchyFacade containmentFacade = createContainmentFacade();
    List<Task> nestedTasks = new ArrayList<Task>(Arrays.asList(containmentFacade.getNestedTasks(supertask)));
    if (nestedTasks.size() == 0) {
      return;
    }
    SortTasksAlgorithm sortAlgorithm = new SortTasksAlgorithm();
    sortAlgorithm.sortTasksByStartDate(nestedTasks);
    Set<Task> modifiedTasks = new HashSet<Task>();
    for (Task nestedTask : nestedTasks) {
      if (nestedTask.getStart().getTime().before(supertask.getStart().getTime())) {
        TaskMutator mutator = nestedTask.createMutatorFixingDuration();
        mutator.setStart(supertask.getStart());
        mutator.commit();

        modifiedTasks.add(nestedTask);
      }
      if (nestedTask.getEnd().getTime().after(supertask.getEnd().getTime())) {
        TaskMutator mutator = nestedTask.createMutatorFixingDuration();
        mutator.shift(supertask.getManager().createLength(supertask.getDuration().getTimeUnit(),
            nestedTask.getEnd().getTime(), supertask.getEnd().getTime()));
        mutator.commit();

        modifiedTasks.add(nestedTask);
      }
    }
    run(modifiedTasks.toArray(new Task[0]));
    RecalculateTaskScheduleAlgorithm alg = new RecalculateTaskScheduleAlgorithm(this) {
      @Override
      protected TaskContainmentHierarchyFacade createContainmentFacade() {
        return AdjustTaskBoundsAlgorithm.this.createContainmentFacade();
      }
    };
    alg.run(modifiedTasks);
  }

  protected abstract TaskContainmentHierarchyFacade createContainmentFacade();

  private class AlgorithmImpl {

    private Set<Task> myModifiedTasks = new HashSet<Task>();

    public void run(Collection<Task> tasks) {
      HashSet<Task> taskSet = new HashSet<Task>(tasks);
      myModifiedTasks.addAll(taskSet);
      TaskContainmentHierarchyFacade containmentFacade = createContainmentFacade();
      while (!taskSet.isEmpty()) {
        recalculateSupertaskScheduleBottomUp(taskSet, containmentFacade);
        taskSet.clear();
        for (Task modifiedTask : myModifiedTasks) {
          Task supertask = containmentFacade.getContainer(modifiedTask);
          if (supertask != null) {
            taskSet.add(supertask);
          }
        }
        myModifiedTasks.clear();
      }
    }

    private void recalculateSupertaskScheduleBottomUp(Set<Task> supertasks,
        TaskContainmentHierarchyFacade containmentFacade) {
      for (Task supertask : supertasks) {
        recalculateSupertaskSchedule(supertask, containmentFacade);
      }
    }

    private void recalculateSupertaskSchedule(final Task supertask,
        final TaskContainmentHierarchyFacade containmentFacade) {
      Task[] nestedTasks = containmentFacade.getNestedTasks(supertask);
      if (nestedTasks.length == 0) {
        return;
      }

      GanttCalendar maxEnd = null;
      GanttCalendar minStart = null;
      for (Task nestedTask : nestedTasks) {
        GanttCalendar nextStart = nestedTask.getStart();
        if (minStart == null || nextStart.compareTo(minStart) < 0) {
          minStart = nextStart;
        }
        GanttCalendar nextEnd = nestedTask.getEnd();
        if (maxEnd == null || nextEnd.compareTo(maxEnd) > 0) {
          maxEnd = nextEnd;
        }
      }
      TaskMutator mutator = supertask.createMutator();
      if (minStart.compareTo(supertask.getStart()) != 0) {
        mutator.setStart(minStart);
        myModifiedTasks.add(supertask);
      }
      if (maxEnd.compareTo(supertask.getEnd()) != 0) {
        mutator.setEnd(maxEnd);
        myModifiedTasks.add(supertask);
      }
      mutator.commit();
    }
  }
}
