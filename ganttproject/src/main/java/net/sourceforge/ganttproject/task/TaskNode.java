/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.task;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeDuration;


/**
 * This class is used to describe the hierarchy of the tasks.
 * 
 * @author bbaranne (Benoit Baranne)
 */
public class TaskNode extends DefaultMutableTreeTableNode {
  /**
   * The reference task
   */
  private final Task task;

  /**
   * Creates an instance of TaskNode with the given task as reference.
   * 
   * @param t
   *          Task of reference.
   */
  public TaskNode(Task t) {
    super(t);
    task = t;
  }

  /** @return the priority of the task. */
  public Task.Priority getPriority() {
    return task.getPriority();
  }

  /**
   * Sets the name of the task.
   * 
   * @param newName
   *          The name to be set.
   */
  public void setName(String newName) {
    task.setName(newName);
  }

  /** @return the name of the task. */
  public String getName() {
    return task.getName();
  }

  /**
   * Sets the start date of the task.
   * 
   * @param startDate
   *          The start date of the task to be set.
   */
  public void setStart(GanttCalendar startDate) {
    TaskMutator mutator = task.createMutatorFixingDuration();
    mutator.setStart(startDate);
    mutator.commit();
  }

  /** @return the start date of the task. */
  public GanttCalendar getStart() {
    return task.getStart();
  }

  /**
   * Sets the end date of the task.
   * 
   * @param endDate
   *          The end date of the task to be set.
   */
  public void setEnd(GanttCalendar endDate) {
    TaskMutator mutator = task.createMutator();
    mutator.setEnd(endDate);
    mutator.commit();
  }

  /** @return the end date of the task. */
  public GanttCalendar getEnd() {
    return task.getEnd();
  }

  /**
   * Sets the duration of the task.
   * 
   * @param length
   *          The duration to be set.
   */
  public void setDuration(TimeDuration length) {
    TaskMutator mutator = task.createMutator();
    mutator.setDuration(length);
    mutator.commit();
  }

  /** @return the duration of the task. */
  public int getDuration() {
    return (int) task.getDuration().getValue();
  }

  /**
   * Sets the completion percentage of the task.
   * 
   * @param percentage
   *          The percentage to be set.
   */
  public void setCompletionPercentage(int percentage) {
    task.setCompletionPercentage(percentage);
  }

  /** @return the completion percentage of the task. */
  public int getCompletionPercentage() {
    return task.getCompletionPercentage();
  }

  public void setTaskInfo(TaskInfo info) {
    task.setTaskInfo(info);
  }

  public TaskInfo getTaskInfo() {
    return task.getTaskInfo();
  }

  @Override
  public String toString() {
    return task.getName();
  }

  @Override
  public Object getUserObject() {
    return task;
  }

  public void applyThirdDateConstraint() {
    if (task.getThird() != null)
      switch (task.getThirdDateConstraint()) {
      case TaskImpl.EARLIESTBEGIN:
        if (task.getThird().after(getStart())) {
          task.setStart(task.getThird().clone());
        }
      }
  }
}
