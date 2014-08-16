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

import java.util.List;

import junit.framework.TestCase;

import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.chart.TaskRendererImpl2.VerticalPartitioning;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class TaskRendererImplTest extends TestCase {
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

}
