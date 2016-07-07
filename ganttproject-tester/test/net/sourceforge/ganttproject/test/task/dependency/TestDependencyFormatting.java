/*
Copyright 2016 BarD Software s.r.o

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
package net.sourceforge.ganttproject.test.task.dependency;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskProperties;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

/**
 * Tests utilities for formatting task dependency into a string.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TestDependencyFormatting extends TaskTestCase {
  private static Function<Integer, Task> createTaskIndex(final TaskManager taskManager) {
    return new Function<Integer, Task>() {
      @Override
      public Task apply(Integer id) {
        return taskManager.getTask(id);
      }
    };
  }
  public void testBasicDependencyFormatting() {
    Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);
    Task successor = createTask(TestSetupHelper.newWendesday(), 2);
    getTaskManager().getDependencyCollection().createDependency(successor, predecessor);
    assertEquals(String.format("%d", predecessor.getTaskID()),
        TaskProperties.formatPredecessors(successor, ";", true));

    Task predecessor2 = createTask(TestSetupHelper.newTuesday(), 1);
    getTaskManager().getDependencyCollection().createDependency(successor, predecessor2);
    assertEquals(String.format("%d;%d", predecessor.getTaskID(), predecessor2.getTaskID()),
        TaskProperties.formatPredecessors(successor, ";", true));
  }

  public void testDependencyTypeFormatting() {
    Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);
    Task successor = createTask(TestSetupHelper.newWendesday(), 2);
    getTaskManager().getDependencyCollection().createDependency(successor, predecessor);
    assertEquals(String.format("%d", predecessor.getTaskID()),
        TaskProperties.formatPredecessors(successor, ";", true));

    Task predecessor2 = createTask(TestSetupHelper.newWendesday(), 1);
    getTaskManager().getDependencyCollection().createDependency(successor, predecessor2, new FinishFinishConstraintImpl());
    assertEquals(String.format("%d;%d-FF", predecessor.getTaskID(), predecessor2.getTaskID()),
        TaskProperties.formatPredecessors(successor, ";", true));
  }

  public void testFullDependencyFormatting() {
    Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);
    Task successor = createTask(TestSetupHelper.newWendesday(), 2);
    getTaskManager().getDependencyCollection().createDependency(successor, predecessor);
    assertEquals(String.format("%d", predecessor.getTaskID()),
        TaskProperties.formatPredecessors(successor, ";", true));

    Task predecessor2 = createTask(TestSetupHelper.newMonday(), 1);
    TaskDependency dep2 = getTaskManager().getDependencyCollection().createDependency(successor, predecessor2);
    dep2.setDifference(1);
    assertEquals(String.format("%d;%d-FS=P1D", predecessor.getTaskID(), predecessor2.getTaskID()),
        TaskProperties.formatPredecessors(successor, ";", true));

    Task predecessor3 = createTask(TestSetupHelper.newWendesday(), 1);
    getTaskManager().getDependencyCollection().createDependency(successor, predecessor3, new FinishFinishConstraintImpl(), TaskDependency.Hardness.RUBBER);
    assertEquals(String.format("%d;%d-FS=P1D;%d-FF>P0D", predecessor.getTaskID(), predecessor2.getTaskID(), predecessor3.getTaskID()),
        TaskProperties.formatPredecessors(successor, ";", true));

    dep2.setHardness(TaskDependency.Hardness.RUBBER);
    assertEquals(String.format("%d;%d-FS>P1D;%d-FF>P0D", predecessor.getTaskID(), predecessor2.getTaskID(), predecessor3.getTaskID()),
        TaskProperties.formatPredecessors(successor, ";", true));
  }

  private static TaskDependency parseDependency(String depSpec, final Task successor, Function<Integer, Task> taskIndex) {
    Map<Integer, Supplier<TaskDependency>> out = Maps.newHashMap();
    TaskProperties.parseDependency(depSpec, successor, taskIndex, out);
    return out.values().iterator().next().get();
  }
  public void testBasicDependencyParsing() {
    Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);
    Task successor = createTask(TestSetupHelper.newWendesday(), 2);

    TaskDependency dependency = parseDependency(
        String.format("%d", predecessor.getTaskID()), successor,
        createTaskIndex(getTaskManager()));
    assertEquals(successor, dependency.getDependant());
    assertEquals(predecessor, dependency.getDependee());
    assertEquals(TaskDependency.Hardness.STRONG, dependency.getHardness());
    assertEquals(0, dependency.getDifference());
    assertEquals(TaskDependencyConstraint.Type.finishstart, dependency.getConstraint().getType());
  }

  public void testDependencyTypeParsing() {
    Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);
    Task successor = createTask(TestSetupHelper.newWendesday(), 2);

    TaskDependency dependency = parseDependency(
        String.format("%d-FF", predecessor.getTaskID()), successor,
        createTaskIndex(getTaskManager()));
    assertEquals(successor, dependency.getDependant());
    assertEquals(predecessor, dependency.getDependee());
    assertEquals(TaskDependency.Hardness.STRONG, dependency.getHardness());
    assertEquals(0, dependency.getDifference());
    assertEquals(TaskDependencyConstraint.Type.finishfinish, dependency.getConstraint().getType());
  }

  public void testFullDependencyParsing() {
    Task successor = createTask(TestSetupHelper.newWendesday(), 2);
    {
      Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);

      TaskDependency dependency = parseDependency(
          String.format("%d-FS=P0D", predecessor.getTaskID()), successor,
          createTaskIndex(getTaskManager()));
      assertEquals(successor, dependency.getDependant());
      assertEquals(predecessor, dependency.getDependee());
      assertEquals(TaskDependency.Hardness.STRONG, dependency.getHardness());
      assertEquals(0, dependency.getDifference());
      assertEquals(TaskDependencyConstraint.Type.finishstart, dependency.getConstraint().getType());
    }
    {
      Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);

      TaskDependency dependency = parseDependency(
          String.format("%d-FS=P1D", predecessor.getTaskID()), successor,
          createTaskIndex(getTaskManager()));
      assertEquals(successor, dependency.getDependant());
      assertEquals(predecessor, dependency.getDependee());
      assertEquals(TaskDependency.Hardness.STRONG, dependency.getHardness());
      assertEquals(1, dependency.getDifference());
      assertEquals(TaskDependencyConstraint.Type.finishstart, dependency.getConstraint().getType());
    }
    {
      Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);

      TaskDependency dependency = parseDependency(
          String.format("%d-FS>P1D", predecessor.getTaskID()), successor,
          createTaskIndex(getTaskManager()));
      assertEquals(successor, dependency.getDependant());
      assertEquals(predecessor, dependency.getDependee());
      assertEquals(TaskDependency.Hardness.RUBBER, dependency.getHardness());
      assertEquals(1, dependency.getDifference());
      assertEquals(TaskDependencyConstraint.Type.finishstart, dependency.getConstraint().getType());
    }
  }

}
