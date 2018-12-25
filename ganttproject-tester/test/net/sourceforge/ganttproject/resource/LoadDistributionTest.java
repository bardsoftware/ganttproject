/*
Copyright 2018 Dmitry Barashev, BarD Software s.r.o

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
package net.sourceforge.ganttproject.resource;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.CustomColumnsManager;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.ResourceAssignmentMutator;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

import java.util.List;
import java.util.Map;

/**
 * @author dbarashev@bardsoftware.com
 */
public class LoadDistributionTest extends TaskTestCase {
  public void testEmptyLoadDistribution() {
    HumanResourceManager resourceManager = new HumanResourceManager(null, new CustomColumnsManager());
    HumanResource humanResource = new HumanResource("Foo", 1, resourceManager);
    resourceManager.add(humanResource);

    LoadDistribution ld = new LoadDistribution(humanResource);
    List<LoadDistribution.Load> loads = ld.getLoads();
    assertEquals(1, loads.size());
    assertEquals(0.0f, loads.get(0).load);
    assertNull(loads.get(0).startDate);
    assertNull(loads.get(0).endDate);
  }

  public void testSingleTaskNoWeekendDistribution() {
    HumanResourceManager resourceManager = new HumanResourceManager(null, new CustomColumnsManager());
    HumanResource humanResource = new HumanResource("Foo", 1, resourceManager);
    resourceManager.add(humanResource);

    Task task = createTask(TestSetupHelper.newMonday(), 1);
    ResourceAssignmentMutator mutableAssignments = task.getAssignmentCollection().createMutator();
    ResourceAssignment assignment = mutableAssignments.addAssignment(humanResource);
    assignment.setLoad(100.0f);
    mutableAssignments.commit();

    LoadDistribution ld = new LoadDistribution(humanResource);
    List<LoadDistribution.Load> loads = ld.getLoads();
    assertEquals(3, loads.size());
    assertEquals(0.0f, loads.get(0).load);
    assertNull(loads.get(0).startDate);
    assertEquals(null, loads.get(0).endDate);

    assertEquals(100.0f, loads.get(1).load);
    assertEquals(TestSetupHelper.newMonday().getTime(), loads.get(1).startDate);
    assertNull(loads.get(1).endDate);

    assertEquals(0.0f, loads.get(2).load);
    assertEquals(TestSetupHelper.newTuesday().getTime(), loads.get(2).startDate);
    assertNull(loads.get(2).endDate);

    Map<Task, List<LoadDistribution.Load>> task2loads = ld.getSeparatedTaskLoads();
    assertEquals(1, task2loads.size());
    List<LoadDistribution.Load> taskLoads = task2loads.get(task);
    assertEquals(1, taskLoads.size());
    assertEquals(100.0f, taskLoads.get(0).load);
    assertEquals(TestSetupHelper.newMonday().getTime(), taskLoads.get(0).startDate);
    assertEquals(TestSetupHelper.newTuesday().getTime(), taskLoads.get(0).endDate);
  }
}
