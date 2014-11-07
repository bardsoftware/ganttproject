/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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

import java.util.Collections;
import java.util.List;

import biz.ganttproject.core.time.TimeDuration;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManagerImpl;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class ShiftTaskTreeAlgorithm {
  public static final boolean DEEP = true;

  public static final boolean SHALLOW = false;

  private final TaskManagerImpl myTaskManager;
  private final RecalculateTaskScheduleAlgorithm myRescheduleAlgorithm;

  public ShiftTaskTreeAlgorithm(TaskManagerImpl taskManager, RecalculateTaskScheduleAlgorithm rescheduleAlgorithm) {
    myTaskManager = taskManager;
    myRescheduleAlgorithm = rescheduleAlgorithm;
  }

  public void run(List<Task> tasks, TimeDuration shift, boolean deep) throws AlgorithmException {
    myTaskManager.setEventsEnabled(false);
    try {
      for (Task t : tasks) {
        shiftTask(t, shift, deep);
      }
      try {
        myTaskManager.getAlgorithmCollection().getScheduler().run();
      } catch (TaskDependencyException e) {
        throw new AlgorithmException("Failed to reschedule the following tasks tasks after move:\n" + tasks, e);
      }
    } finally {
      myTaskManager.setEventsEnabled(true);
    }
  }

  public void run(Task rootTask, TimeDuration shift, boolean deep) throws AlgorithmException {
    run(Collections.singletonList(rootTask), shift, deep);
  }

  private void shiftTask(Task rootTask, TimeDuration shift, boolean deep) {
    if (rootTask != myTaskManager.getRootTask()) {
      rootTask.shift(shift);
    }
    if (deep) {
      Task[] nestedTasks = rootTask.getManager().getTaskHierarchy().getNestedTasks(rootTask);
      for (int i = 0; i < nestedTasks.length; i++) {
        Task next = nestedTasks[i];
        shiftTask(next, shift, true);
      }
    }
  }
}
