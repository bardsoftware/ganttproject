/*
Copyright 2014 BarD Software s.r.o

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
package net.sourceforge.ganttproject.chart;

import io.milton.event.NewFolderEvent;

import java.util.List;

import junit.framework.TestCase;
import biz.ganttproject.core.calendar.WeekendCalendarImpl;

import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.chart.TaskRendererImpl2.VerticalPartitioning;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskRendererImplTest extends TaskTestCase {
  private List<Task> createTasks(TaskManager taskManager, int count) {
    List<Task> result = Lists.newArrayList();
    for (int i = 0; i < count; i++) {
      result.add(taskManager.newTaskBuilder().withId(i).build());
    }
    return result;
  }

  public void testVerticalPartitioningNoCollapsed() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    List<Task> allTasks = createTasks(taskManager, 10);
    {
      VerticalPartitioning partitioning = new TaskRendererImpl2.VerticalPartitioning(allTasks.subList(0, 10));
      partitioning.build(taskManager.getTaskHierarchy());
      assertTrue(partitioning.aboveViewport.isEmpty());
      assertTrue(partitioning.belowViewport.isEmpty());
    }
    {
      VerticalPartitioning partitioning = new TaskRendererImpl2.VerticalPartitioning(allTasks.subList(0, 5));
      partitioning.build(taskManager.getTaskHierarchy());
      assertTrue(partitioning.aboveViewport.isEmpty());
      assertEquals(5, partitioning.belowViewport.size());
    }
    {
      VerticalPartitioning partitioning = new TaskRendererImpl2.VerticalPartitioning(allTasks.subList(5, 10));
      partitioning.build(taskManager.getTaskHierarchy());
      assertEquals(5, partitioning.aboveViewport.size());
      assertTrue(partitioning.belowViewport.isEmpty());
    }
    {
      VerticalPartitioning partitioning = new TaskRendererImpl2.VerticalPartitioning(allTasks.subList(3, 7));
      partitioning.build(taskManager.getTaskHierarchy());
      assertEquals(3, partitioning.aboveViewport.size());
      assertEquals(3, partitioning.belowViewport.size());
    }
  }

  public void testVerticalPartitioningWithCollapsedTasks() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    List<Task> allTasks = createTasks(taskManager, 10);
    allTasks.get(2).move(allTasks.get(1));
    allTasks.get(1).move(allTasks.get(0));
    allTasks.get(0).setExpand(false);
    {
      VerticalPartitioning partitioning = new TaskRendererImpl2.VerticalPartitioning(allTasks.subList(4, 10));
      partitioning.build(taskManager.getTaskHierarchy());
      assertEquals(2, partitioning.aboveViewport.size());
      assertEquals(0, partitioning.aboveViewport.get(0).getTaskID());
      assertEquals(3, partitioning.aboveViewport.get(1).getTaskID());
      assertTrue(partitioning.belowViewport.isEmpty());
    }
    allTasks.get(7).move(allTasks.get(6));
    allTasks.get(9).move(allTasks.get(8));
    {
      VerticalPartitioning partitioning = new TaskRendererImpl2.VerticalPartitioning(allTasks.subList(4, 6));
      partitioning.build(taskManager.getTaskHierarchy());
      assertEquals(2, partitioning.aboveViewport.size());
      assertEquals(0, partitioning.aboveViewport.get(0).getTaskID());
      assertEquals(3, partitioning.aboveViewport.get(1).getTaskID());
      assertEquals(2, partitioning.belowViewport.size());
      assertEquals(6, partitioning.belowViewport.get(0).getTaskID());
      assertEquals(8, partitioning.belowViewport.get(1).getTaskID());
    }
  }

  // Tests algorithm which partitions task activities into "before viewport", "inside viewport"
  // and "after viewport" parts
  public void testSplitOnBounds() {
    Task task = createTask(TestSetupHelper.newFriday(), 5);
    {
      // Split on frame with frame start = task start
      List<TaskActivity> activities = TaskRendererImpl2.splitOnBounds(
          task.getActivities(), TestSetupHelper.newFriday().getTime(), TestSetupHelper.newMonday().getTime());
      assertEquals(2, activities.size());
      assertTrue(activities.get(0).isFirst());
      assertEquals(TestSetupHelper.newFriday().getTime(), activities.get(0).getStart());
      assertEquals(TestSetupHelper.newMonday().getTime(), activities.get(0).getEnd());

      assertTrue(activities.get(1).isLast());
      assertEquals(TestSetupHelper.newMonday().getTime(), activities.get(1).getStart());
      assertEquals(TestSetupHelper.newWendesday().getTime(), activities.get(1).getEnd());
    }
    {
      // Task crosses both frame borders
      List<TaskActivity> activities = TaskRendererImpl2.splitOnBounds(
          task.getActivities(), TestSetupHelper.newSaturday().getTime(), TestSetupHelper.newTuesday().getTime());
      assertEquals(3, activities.size());
      assertTrue(activities.get(0).isFirst());
      assertEquals(TestSetupHelper.newFriday().getTime(), activities.get(0).getStart());
      assertEquals(TestSetupHelper.newSaturday().getTime(), activities.get(0).getEnd());

      assertFalse(activities.get(1).isFirst());
      assertFalse(activities.get(1).isLast());
      assertEquals(TestSetupHelper.newSaturday().getTime(), activities.get(1).getStart());
      assertEquals(TestSetupHelper.newTuesday().getTime(), activities.get(1).getEnd());

      assertTrue(activities.get(2).isLast());
      assertEquals(TestSetupHelper.newTuesday().getTime(), activities.get(2).getStart());
      assertEquals(TestSetupHelper.newWendesday().getTime(), activities.get(2).getEnd());
    }
    {
      // Split on frame with frame end = task end
      List<TaskActivity> activities = TaskRendererImpl2.splitOnBounds(
          task.getActivities(), TestSetupHelper.newMonday().getTime(), TestSetupHelper.newWendesday().getTime());
      assertEquals(2, activities.size());
      assertTrue(activities.get(0).isFirst());
      assertEquals(TestSetupHelper.newFriday().getTime(), activities.get(0).getStart());
      assertEquals(TestSetupHelper.newMonday().getTime(), activities.get(0).getEnd());

      assertTrue(activities.get(1).isLast());
      assertEquals(TestSetupHelper.newMonday().getTime(), activities.get(1).getStart());
      assertEquals(TestSetupHelper.newWendesday().getTime(), activities.get(1).getEnd());
    }
  }

}
