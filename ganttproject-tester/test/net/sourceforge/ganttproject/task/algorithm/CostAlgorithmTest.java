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
package net.sourceforge.ganttproject.task.algorithm;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.TestSetupHelper.TaskManagerBuilder;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.CostStub;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

import java.math.BigDecimal;

/**
 * Tests for cost calculations
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CostAlgorithmTest extends TaskTestCase {
  public void testSupertaskCost() {
    Task supertask = createTask();
    Task subtask1 = createTask();
    Task subtask2 = createTask();
    TaskContainmentHierarchyFacade hierarchy = getTaskManager().getTaskHierarchy();
    hierarchy.move(subtask1, supertask);
    hierarchy.move(subtask2, supertask);
    supertask.setCost(new CostStub(BigDecimal.ZERO, true));
    subtask1.setCost(new CostStub(BigDecimal.valueOf(5), false));
    subtask2.setCost(new CostStub(BigDecimal.valueOf(15), false));
    assertEquals(BigDecimal.valueOf(20), supertask.getCost().getValue());

    supertask.setCost(new CostStub(BigDecimal.valueOf(10), false));
    assertEquals(BigDecimal.valueOf(10), supertask.getCost().getValue());
  }

  public void testResourceCost() {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    setTaskManager(builder.build());
    HumanResource joe = new HumanResource("Joe", 1, builder.getResourceManager());
    joe.setStandardPayRate(BigDecimal.valueOf(5));

    HumanResource jane = new HumanResource("Jane", 2, builder.getResourceManager());
    jane.setStandardPayRate(BigDecimal.valueOf(10));

    builder.getResourceManager().add(joe);
    builder.getResourceManager().add(jane);

    Task t = createTask();
    t.setDuration(t.getManager().createLength(2));
    t.getAssignmentCollection().addAssignment(joe).setLoad(100f);
    t.getAssignmentCollection().addAssignment(jane).setLoad(50f);
    t.setCost(new CostStub(BigDecimal.ZERO, true));
    assertEquals(BigDecimal.valueOf(20f), t.getCost().getValue());

    t.setCost(new CostStub(BigDecimal.valueOf(10), false));
    assertEquals(BigDecimal.valueOf(10), t.getCost().getValue());
  }

  public void testResourceTotalCost() {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    setTaskManager(builder.build());
    HumanResource joe = new HumanResource("Joe", 1, builder.getResourceManager());
    joe.setStandardPayRate(BigDecimal.valueOf(5));

    assertEquals(BigDecimal.ZERO, joe.getTotalCost());

    builder.getResourceManager().add(joe);

    Task t = createTask();
    t.setDuration(t.getManager().createLength(2));
    t.getAssignmentCollection().addAssignment(joe).setLoad(100f);
    assertEquals(BigDecimal.valueOf(10), joe.getTotalCost());

    t = createTask();
    t.setDuration(t.getManager().createLength(4));
    t.getAssignmentCollection().addAssignment(joe).setLoad(50f);
    assertEquals(BigDecimal.valueOf(20), joe.getTotalCost());

    t = createTask();
    t.setDuration(t.getManager().createLength(10));
    t.getAssignmentCollection().addAssignment(joe).setLoad(0f);
    assertEquals(BigDecimal.valueOf(20), joe.getTotalCost());
  }

}
