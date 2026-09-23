/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Dmitry Barashev

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
package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * A baseline: a snapshot of every task's start, duration, milestone flag and summary flag,
 * taken at one moment and kept in memory.
 *
 * @author nbohn
 */
public class GanttPreviousState {
  private final List<GanttPreviousStateTask> myTasks;

  private String myName;

  public GanttPreviousState(String name, List<GanttPreviousStateTask> tasks) {
    myName = name;
    myTasks = tasks;
  }

  public void setName(String name) {
    myName = name;
  }

  public String getName() {
    return myName;
  }

  public List<GanttPreviousStateTask> getTasks() {
    return Collections.unmodifiableList(myTasks);
  }

  public static List<GanttPreviousStateTask> createTasks(TaskManager taskManager) {
    List<GanttPreviousStateTask> result = new ArrayList<GanttPreviousStateTask>();
    for (Task t : taskManager.getTasks()) {
      GanttPreviousStateTask baselineTask = new GanttPreviousStateTask(t.getTaskID(), t.getStart().clone(),
          t.getDuration().getLength(), t.isMilestone(), taskManager.getTaskHierarchy().hasNestedTasks(t));
      result.add(baselineTask);
    }
    return result;
  }
}
