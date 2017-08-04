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
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

/**
 * Tests for cost calculations
 *
 * @author schch (Christoph Schneider)
 */
public class LoadAlgorithmTest extends TaskTestCase {
  public void testSupertaskLoad() {
    Task supertask = createTask();
    Task subtask1 = createTask();
    Task subtask2 = createTask();
    TaskContainmentHierarchyFacade hierarchy = getTaskManager().getTaskHierarchy();
    hierarchy.move(subtask1, supertask);
    hierarchy.move(subtask2, supertask);

    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    setTaskManager(builder.build());
    HumanResource joe = new HumanResource("Joe", 1, builder.getResourceManager());

    HumanResource jane = new HumanResource("Jane", 1, builder.getResourceManager());

    builder.getResourceManager().add(joe);
    builder.getResourceManager().add(jane);

    subtask1.setDuration(subtask1.getManager().createLength(5));
    subtask2.setDuration(subtask1.getManager().createLength(10));
    assertEquals(0.0, supertask.getLoad().getValue());

    subtask1.getAssignmentCollection().addAssignment(joe).setLoad(100f);
    subtask1.getAssignmentCollection().addAssignment(jane).setLoad(50f);
    assertEquals(7.5, supertask.getLoad().getValue());
    assertEquals(7.5, subtask1.getLoad().getValue());
    assertEquals(0.0, subtask2.getLoad().getValue());

    subtask2.getAssignmentCollection().addAssignment(jane).setLoad(50f);
    assertEquals(12.5, supertask.getLoad().getValue());
    assertEquals(7.5, subtask1.getLoad().getValue());
    assertEquals(5.0, subtask2.getLoad().getValue());
  }

  public void testTaskLoad() {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    setTaskManager(builder.build());
    HumanResource joe = new HumanResource("Joe", 1, builder.getResourceManager());

    HumanResource jane = new HumanResource("Jane", 1, builder.getResourceManager());

    builder.getResourceManager().add(joe);
    builder.getResourceManager().add(jane);

    Task t = createTask();
    t.setDuration(t.getManager().createLength(2));
    assertEquals(0.0, t.getLoad().getValue());

    t.getAssignmentCollection().addAssignment(joe).setLoad(100f);
    t.getAssignmentCollection().addAssignment(jane).setLoad(50f);
    assertEquals(3.0, t.getLoad().getValue());
  }

}
