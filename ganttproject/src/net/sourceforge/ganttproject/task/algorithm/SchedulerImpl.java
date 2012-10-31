/*
Copyright 2012 GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.task.algorithm;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeDuration;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskMutator;
import net.sourceforge.ganttproject.task.algorithm.DependencyGraph.DependencyEdge;
import net.sourceforge.ganttproject.task.algorithm.DependencyGraph.ImplicitSubSuperTaskDependency;
import net.sourceforge.ganttproject.task.algorithm.DependencyGraph.Node;

/**
 * This class walk the dependency graph and updates start and end dates of tasks
 * according to information returned by dependency edges.
 *
 * @author dbarashev
 */
public class SchedulerImpl extends AlgorithmBase {
  private final DependencyGraph myGraph;
  private boolean isRunning;
  private final Supplier<TaskContainmentHierarchyFacade> myTaskHierarchy;

  public SchedulerImpl(DependencyGraph graph, Supplier<TaskContainmentHierarchyFacade> taskHierarchy) {
    myGraph = graph;
    myGraph.addListener(new DependencyGraph.Listener() {
      @Override
      public void onChange() {
        run();
      }
    });
    myTaskHierarchy = taskHierarchy;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    if (isEnabled()) {
      run();
    }
  }

  @Override
  public void run() {
    if (!isEnabled() || isRunning) {
      return;
    }
    isRunning = true;
    try {
      doRun();
    } finally {
      isRunning = false;
    }
  }

  private void doRun() {
    int layers = myGraph.checkLayerValidity();
    for (int i = 1; i < layers; i++) {
      Collection<Node> layer = myGraph.getLayer(i);
      for (Node node : layer) {
        Range<Date> startRange = Ranges.all();
        Range<Date> endRange = Ranges.all();

        List<Date> subtaskRanges = Lists.newArrayList();
        List<DependencyEdge> incoming = node.getIncoming();
        for (DependencyEdge edge : incoming) {
          if (!edge.refresh()) {
            continue;
          }
          if (edge instanceof ImplicitSubSuperTaskDependency) {
            subtaskRanges.add(edge.getStartRange().upperEndpoint());
            subtaskRanges.add(edge.getEndRange().lowerEndpoint());
          } else {
            startRange = startRange.intersection(edge.getStartRange());
            endRange = endRange.intersection(edge.getEndRange());
          }
          if (startRange.isEmpty() || endRange.isEmpty()) {
            GPLogger.logToLogger("both start and end ranges were calculated as empty for task=" + node.getTask() + ". Skipping it");
          }
        }

        if (!subtaskRanges.isEmpty()) {
          Range<Date> subtasks = Ranges.encloseAll(subtaskRanges);
          startRange = startRange.intersection(subtasks);
          endRange = endRange.intersection(subtasks);
        }
        if (startRange.hasLowerBound()) {
          modifyTaskStart(node.getTask(), startRange.lowerEndpoint());
        }
        if (endRange.hasUpperBound()) {
          modifyTaskEnd(node.getTask(), endRange.upperEndpoint());
        }
      }
    }
  }

  private void modifyTaskEnd(Task task, Date newEnd) {
    if (task.getEnd().getTime().equals(newEnd)) {
      return;
    }
    GanttCalendar newEndCalendar = CalendarFactory.createGanttCalendar(newEnd);
    if (getDiagnostic() != null) {
      getDiagnostic().info("changing end date of the task #" + task.getTaskID() + " " + task.getName() + ". Was:" + task.getStart() + " Now:" + newEndCalendar);
    }
    TaskMutator mutator = task.createMutator();
    mutator.setEnd(newEndCalendar);
    mutator.commit();
  }

  private void modifyTaskStart(Task task, Date newStart) {
    if (task.getStart().getTime().equals(newStart)) {
      return;
    }
    GanttCalendar newStartCalendar = CalendarFactory.createGanttCalendar(newStart);
    if (getDiagnostic() != null) {
      getDiagnostic().info("changing start date of the task #" + task.getTaskID() + " " + task.getName() + ". Was:" + task.getStart() + " Now:" + newStartCalendar);
    }
    TaskMutator mutator = task.createMutator();
    if (myTaskHierarchy.get().hasNestedTasks(task)) {
      mutator.setStart(newStartCalendar);
      mutator.commit();
    } else {
      TimeDuration shift = task.getManager().createLength(task.getDuration().getTimeUnit(), task.getStart().getTime(), newStart);
      mutator.shift(shift);
      mutator.commit();
    }
  }
}
