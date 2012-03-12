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
package net.sourceforge.ganttproject.task.dependency;

import net.sourceforge.ganttproject.task.Task;

public class SearchKey implements Comparable<SearchKey> {
  static final int DEPENDANT = 1;

  static final int DEPENDEE = 2;

  final int myFirstTaskID;

  final int myType;

  final int mySecondTaskID;

  public SearchKey(int type, TaskDependencyImpl taskDependency) {
    myType = type;
    Task firstTask, secondTask;
    switch (type) {
    case DEPENDANT: {
      firstTask = taskDependency.getDependant();
      secondTask = taskDependency.getDependee();
      break;
    }
    case DEPENDEE: {
      firstTask = taskDependency.getDependee();
      secondTask = taskDependency.getDependant();
      break;
    }
    default: {
      throw new RuntimeException("Invalid type=" + type);
    }
    }
    myFirstTaskID = firstTask.getTaskID();
    mySecondTaskID = secondTask.getTaskID();
  }

  protected SearchKey(int type, int firstTaskID, int secondTaskID) {
    myType = type;
    myFirstTaskID = firstTaskID;
    mySecondTaskID = secondTaskID;
  }

  @Override
  public int compareTo(SearchKey rvalue) {
    int result = myFirstTaskID - rvalue.myFirstTaskID;
    if (result == 0) {
      result = myType - rvalue.myType;
    }
    if (result == 0) {
      result = mySecondTaskID - rvalue.mySecondTaskID;
    }
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    SearchKey rvalue = (SearchKey) obj;
    return myFirstTaskID == rvalue.myFirstTaskID && myType == rvalue.myType && mySecondTaskID == rvalue.mySecondTaskID;
  }

  @Override
  public int hashCode() {
    return 7 * myFirstTaskID + 11 * myType + 13 * mySecondTaskID;
  }
}
