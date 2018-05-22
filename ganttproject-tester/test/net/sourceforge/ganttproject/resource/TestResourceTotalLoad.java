/*
Copyright 2017 Christoph Schneider, BarD Software s.r.o

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
import net.sourceforge.ganttproject.TestSetupHelper.TaskManagerBuilder;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

public class TestResourceTotalLoad extends TaskTestCase {

  public void testResourceTotalLoad() {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    setTaskManager(builder.build());
    HumanResource joe = new HumanResource("Joe", 1, builder.getResourceManager());

    assertEquals(0.0, joe.getTotalLoad());

    builder.getResourceManager().add(joe);

    Task t = createTask();
    t.setDuration(t.getManager().createLength(2));
    t.getAssignmentCollection().addAssignment(joe).setLoad(100f);
    // two days at 100% load
    assertEquals(2.0, joe.getTotalLoad());

    t = createTask();
    t.setDuration(t.getManager().createLength(4));
    t.getAssignmentCollection().addAssignment(joe).setLoad(75f);
    // add another 4 days at 75% load
    assertEquals(5.0, joe.getTotalLoad());

    t = createTask();
    t.setDuration(t.getManager().createLength(10));
    t.getAssignmentCollection().addAssignment(joe).setLoad(0f);
    // add another 10 days at 0% load
    assertEquals(5.0, joe.getTotalLoad());
  }

}
