/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Thomas Alexandre, GanttProject Team

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

import biz.ganttproject.core.time.GanttCalendar;
import net.sourceforge.ganttproject.task.TaskImpl;
import net.sourceforge.ganttproject.task.TaskManagerImpl;
import net.sourceforge.ganttproject.task.TaskMutator;

import java.io.Serializable;

/**
 * Class that generate a task
 */
public class GanttTask extends TaskImpl implements Serializable {

  /**
   * @param name
   *          of the new Task
   * @param start
   *          date of the new Task
   * @param length
   *          of the new Task
   * @param taskManager
   *          to use when creating the new task
   * @param taskID
   *          contains the id to be used for the new task, or -1 to generate a
   *          unique one.
   */
  public GanttTask(String name, GanttCalendar start, long length, TaskManagerImpl taskManager, int taskID) {
    super(taskManager, taskID);
    TaskMutator mutator = createMutator();
    mutator.setName(name);
    mutator.setStart(start);
    mutator.setDuration(taskManager.createLength(length));
    mutator.commit();
    enableEvents(true);
  }

  /**
   * Will make a copy of the given GanttTask
   *
   * @param copy
   *          task to copy
   */
  public GanttTask(TaskManagerImpl manager, TaskImpl copy) {
    super(manager, copy, false);
    enableEvents(true);
  }

  /** @deprecated Use TimeUnit class instead and method getDuration() */
  @Deprecated
  public int getLength() {
    return getDuration().getLength();
  }

  /** @deprecated Use setDuration() */
  @Deprecated
  public void setLength(int l) {
    if (l <= 0) {
      throw new IllegalArgumentException("Length of task must be >=0. You've passed length=" + l + " to task=" + this);
    }
    TaskMutator mutator = createMutator();
    mutator.setDuration(getManager().createLength(getDuration().getTimeUnit(), l));
    mutator.commit();
  }
}
