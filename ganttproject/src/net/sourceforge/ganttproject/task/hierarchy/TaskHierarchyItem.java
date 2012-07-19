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
package net.sourceforge.ganttproject.task.hierarchy;

import java.util.ArrayList;

import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.task.Task;

public class TaskHierarchyItem {
  private Task myTask;

  private TaskHierarchyItem myContainerItem;

  private TaskHierarchyItem myFirstNestedItem;

  private TaskHierarchyItem myNextSiblingItem;

  private static final TaskHierarchyItem[] EMPTY_ARRAY = new TaskHierarchyItem[0];

  public TaskHierarchyItem(Task myTask, TaskHierarchyItem containerItem) {
    this.myTask = myTask;
    this.myContainerItem = containerItem;
    if (myContainerItem != null) {
      myContainerItem.addNestedItem(this);
    }
  }

  public Task getTask() {
    return myTask;
  }

  public TaskHierarchyItem getContainerItem() {
    return myContainerItem;
  }

  public TaskHierarchyItem[] getNestedItems() {
    TaskHierarchyItem[] result;
    if (myFirstNestedItem == null) {
      result = EMPTY_ARRAY;
    } else {
      ArrayList<TaskHierarchyItem> tempList = new ArrayList<TaskHierarchyItem>();
      for (TaskHierarchyItem nested = myFirstNestedItem; nested != null; nested = nested.myNextSiblingItem) {
        tempList.add(nested);
      }
      result = Lists.reverse(tempList).toArray(EMPTY_ARRAY);
    }
    return result;
  }

  public void addNestedItem(TaskHierarchyItem nested) {
    nested.myNextSiblingItem = myFirstNestedItem;
    nested.myContainerItem = this;
    myFirstNestedItem = nested;
  }

  public void delete() {
    if (myContainerItem != null) {
      TaskHierarchyItem previousSibling = myContainerItem.myFirstNestedItem;
      if (this == previousSibling) {
        myContainerItem.myFirstNestedItem = myNextSiblingItem;
      } else {
        for (; previousSibling.myNextSiblingItem != this; previousSibling = previousSibling.myNextSiblingItem)
          ;
        previousSibling.myNextSiblingItem = myNextSiblingItem;
      }
      myContainerItem = null;
    }
    myNextSiblingItem = null;
  }
}
