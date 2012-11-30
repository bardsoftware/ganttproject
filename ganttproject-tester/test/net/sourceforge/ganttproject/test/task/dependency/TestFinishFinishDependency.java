/*
Copyright 2012 GanttProject Team

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

import biz.ganttproject.core.calendar.WeekendCalendarImpl;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint.Collision;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

/**
 * Tests Finish-Finish dependency behavior.
 *
 * @author dbarashev
 */
public class TestFinishFinishDependency extends TaskTestCase {
  public void testNoLagDependency() throws Exception {
    getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(false);
    Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);
    Task successor = createTask(TestSetupHelper.newFriday(), 2);
    TaskDependency dep = getTaskManager().getDependencyCollection().createDependency(successor, predecessor);

    FinishFinishConstraintImpl constraint = new FinishFinishConstraintImpl();
    constraint.setTaskDependency(dep);
    Collision collision = constraint.getCollision();
    assertTrue(collision.isActive());
    assertEquals(TestSetupHelper.newMonday(), collision.getAcceptableStart());
    assertEquals(Collision.START_LATER_VARIATION, collision.getVariation());
  }

  public void testDependencyWithLag() throws Exception {
    getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(false);
    Task predecessor = createTask(TestSetupHelper.newTuesday(), 1);
    Task successor = createTask(TestSetupHelper.newFriday(), 2);
    TaskDependency dep = createDependency(successor, predecessor);
    dep.setDifference(2);

    FinishFinishConstraintImpl constraint = new FinishFinishConstraintImpl();
    constraint.setTaskDependency(dep);
    Collision collision = constraint.getCollision();
    assertTrue(collision.isActive());
    assertEquals(TestSetupHelper.newWendesday(), collision.getAcceptableStart());
    assertEquals(Collision.START_LATER_VARIATION, collision.getVariation());
  }

  public void testDependencyWithLagCrossingWeekend() throws Exception {
    setTaskManager(TestSetupHelper.newTaskManagerBuilder().withCalendar(new WeekendCalendarImpl()).build());
    getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(false);
    Task predecessor = createTask(TestSetupHelper.newFriday(), 1);
    Task successor = createTask(TestSetupHelper.newFriday(), 1);
    TaskDependency dep = createDependency(successor, predecessor);
    dep.setDifference(2);

    FinishFinishConstraintImpl constraint = new FinishFinishConstraintImpl();
    constraint.setTaskDependency(dep);
    Collision collision = constraint.getCollision();
    assertTrue(collision.isActive());
    assertEquals(TestSetupHelper.newTuesday(), collision.getAcceptableStart());
    assertEquals(Collision.START_LATER_VARIATION, collision.getVariation());
  }
}
