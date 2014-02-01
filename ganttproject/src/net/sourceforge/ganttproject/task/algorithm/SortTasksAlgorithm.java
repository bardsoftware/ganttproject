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
import java.util.Comparator;
import java.util.List;

import biz.ganttproject.core.time.TimeDuration;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

/**
 * @author bard
 */
public class SortTasksAlgorithm {
  private Comparator<TaskActivity> mySortActivitiesByStartDateComparator = new Comparator<TaskActivity>() {
    @Override
    public int compare(TaskActivity leftTask, TaskActivity rightTask) {
      int result = 0;
      if (!leftTask.equals(rightTask)) {
        result = leftTask.getStart().compareTo(rightTask.getStart());
        if (result == 0) {
          float longResult = 0;
          TimeDuration leftLength = leftTask.getDuration();
          TimeDuration rightLength = rightTask.getDuration();
          if (leftLength.getTimeUnit().isConstructedFrom(rightLength.getTimeUnit())) {
            longResult = leftLength.getLength(rightLength.getTimeUnit()) - rightLength.getLength();
          } else if (rightLength.getTimeUnit().isConstructedFrom(leftLength.getTimeUnit())) {
            longResult = leftLength.getLength() - rightLength.getLength(leftLength.getTimeUnit());
          } else {
            throw new IllegalArgumentException("Lengths=" + leftLength + " and " + rightLength + " are not compatible");
          }
          if (longResult != 0) {
            result = (int) (longResult / Math.abs(longResult));
          }
        }
      }
      return result;
    }

  };

  private Comparator<Task> mySortTasksByStartDateComparator = new Comparator<Task>() {
    @Override
    public int compare(Task leftTask, Task rightTask) {
      int result = 0;
      if (!leftTask.equals(rightTask)) {
        result = leftTask.getStart().compareTo(rightTask.getStart());
        if (result == 0) {
          float longResult = 0;
          TimeDuration leftLength = leftTask.getDuration();
          TimeDuration rightLength = rightTask.getDuration();
          if (leftLength.getTimeUnit().isConstructedFrom(rightLength.getTimeUnit())) {
            longResult = leftLength.getLength(rightLength.getTimeUnit()) - rightLength.getLength();
          } else if (rightLength.getTimeUnit().isConstructedFrom(leftLength.getTimeUnit())) {
            longResult = leftLength.getLength() - rightLength.getLength(leftLength.getTimeUnit());
          } else {
            throw new IllegalArgumentException("Lengths=" + leftLength + " and " + rightLength + " are not compatible");
          }
          if (longResult != 0) {
            result = (int) (longResult / Math.abs(longResult));
          }
        }
      }
      return result;
    }

  };

  public void sortByStartDate(List<TaskActivity> tasks) {
    Collections.sort(tasks, mySortActivitiesByStartDateComparator);
  }

  public void sortTasksByStartDate(List<Task> tasks) {
    Collections.sort(tasks, mySortTasksByStartDateComparator);
  }

}
