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
package net.sourceforge.ganttproject.task.event;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import java.util.EventObject;

public class TaskHierarchyEvent extends EventObject {
  private final Task myNewContainer;

  private final Task myTask;

  private final Task myOldContainer;
  private final int myIndexAtNew;
  private final TaskManager.EventSource mySource;

  public TaskHierarchyEvent(TaskManager.EventSource source, Task myTask, Task myOldContainer, Task myNewContainer, int indexAtNew) {
    super(source);
    this.myNewContainer = myNewContainer;
    this.myTask = myTask;
    this.myOldContainer = myOldContainer;
    this.myIndexAtNew = indexAtNew;
    this.mySource = source;
  }

  public Task getTask() {
    return myTask;
  }

  public Task getOldContainer() {
    return myOldContainer;
  }

  public Task getNewContainer() {
    return myNewContainer;
  }

  public int getIndexAtNew() {
    return myIndexAtNew;
  }

  public TaskManager.EventSource getTaskSource() {
    return mySource;
  }
}
