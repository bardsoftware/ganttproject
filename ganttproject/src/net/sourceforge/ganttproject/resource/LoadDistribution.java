/*
 * This code is provided under the terms of GPL version 3.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.DefaultListModel;

import biz.ganttproject.core.calendar.GanttDaysOff;

import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

/**
 * Represents load of of one particular resource in the given time range
 */
public class LoadDistribution {
  public static class Load {
    public float load;

    public final Task refTask;

    Load(Date startDate, Date endDate, float load, Task ref) {
      this.load = load;
      this.refTask = ref;
      this.startDate = startDate;
      this.endDate = endDate;
    }

    @Override
    public String toString() {
      return "start=" + this.startDate + " load=" + this.load + " refTask = " + this.refTask;
    }

    public boolean isResourceUnavailable() {
      return load == -1;
    }

    public final Date startDate;
    public final Date endDate;
  }

  private final List<Load> myTasksLoads = new ArrayList<Load>();

  public LoadDistribution(HumanResource resource) {
    ResourceAssignment[] assignments = resource.getAssignments();
    for (ResourceAssignment assignment : assignments) {
      processAssignment(assignment);
    }
    processDaysOff(resource);
  }

  private void processDaysOff(HumanResource resource) {
    DefaultListModel daysOff = resource.getDaysOff();
    if (daysOff != null) {
      for (int l = 0; l < daysOff.size(); l++) {
        GanttDaysOff dayOff = (GanttDaysOff) daysOff.get(l);
        Date dayOffStart = dayOff.getStart().getTime();
        Date dayOffEnd = dayOff.getFinish().getTime();
        myTasksLoads.add(new Load(dayOffStart, dayOffEnd, -1, null));
      }
    }
  }

  private void processAssignment(ResourceAssignment assignment) {
    Task task = assignment.getTask();
    for (TaskActivity ta : task.getActivities()) {
      if (ta.getIntensity() != 0) {
        myTasksLoads.add(new Load(ta.getStart(), ta.getEnd(), assignment.getLoad(), task));
      }
    }
  }

  /**
   * @return a list of lists of <code>Load</code> instances. Each list contains
   *         a set of <code>Load</code>
   */
  public List<Load> getTasksLoads() {
    return myTasksLoads;
  }
}
