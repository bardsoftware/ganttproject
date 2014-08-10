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

import java.math.BigDecimal;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.TestSetupHelper.TaskManagerBuilder;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

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
    supertask.getCost().setCalculated(true);
    subtask1.getCost().setCalculated(false);
    subtask1.getCost().setValue(BigDecimal.valueOf(5));
    subtask2.getCost().setCalculated(false);
    subtask2.getCost().setValue(BigDecimal.valueOf(15));
    assertEquals(BigDecimal.valueOf(20), supertask.getCost().getValue());

    supertask.getCost().setCalculated(false);
    supertask.getCost().setValue(BigDecimal.valueOf(10));
    assertEquals(BigDecimal.valueOf(10), supertask.getCost().getValue());
  }

  public void testResourceCost() {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    setTaskManager(builder.build());
    HumanResource joe = new HumanResource("Joe", 1, builder.getResourceManager());
    joe.setStandardPayRate(BigDecimal.valueOf(5));

    HumanResource jane = new HumanResource("Jane", 1, builder.getResourceManager());
    jane.setStandardPayRate(BigDecimal.valueOf(10));

    builder.getResourceManager().add(joe);
    builder.getResourceManager().add(jane);

    Task t = createTask();
    t.setDuration(t.getManager().createLength(2));
    t.getAssignmentCollection().addAssignment(joe).setLoad(100f);
    t.getAssignmentCollection().addAssignment(jane).setLoad(50f);
    t.getCost().setCalculated(true);
    assertEquals(BigDecimal.valueOf(20f), t.getCost().getValue());

    t.getCost().setCalculated(false);
    t.getCost().setValue(BigDecimal.valueOf(10));
    assertEquals(BigDecimal.valueOf(10), t.getCost().getValue());
  }

}
