/*
GanttProject is an opensource project management tool.
Copyright (C) 2009 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.test.task.dependency;


import biz.ganttproject.core.time.GanttCalendar;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.constraint.StartFinishConstraintImpl;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

public class TestStartFinishDepedency extends TaskTestCase {
    public void testSimpleFinishStartDependency() throws Exception {
        Task t1 = createTask();
        Task t2 = createTask();

        GanttCalendar dependeeStart = TestSetupHelper.newFriday();
        t1.setStart(dependeeStart);
        TaskDependency dep = createDependency(t2, t1);
        dep.setConstraint(new StartFinishConstraintImpl());

        assertEquals(dependeeStart, t2.getEnd());
    }

    public void testFinishStartDependencyWithLag() throws Exception {
        Task t1 = createTask();
        Task t2 = createTask();

        GanttCalendar dependeeStart = TestSetupHelper.newFriday();
        t1.setStart(dependeeStart);
        TaskDependency dep = createDependency(t2, t1);
        dep.setDifference(3);
        dep.setConstraint(new StartFinishConstraintImpl());

        assertEquals(TestSetupHelper.newMonday(), t2.getEnd());
    }

    public void testSFChain() throws Exception {
        Task t1 = createTask();
        Task t2 = createTask();
        Task t3 = createTask();

        t1.setStart(TestSetupHelper.newMonday());
        TaskDependency dep_t2_t1 = createDependency(t2, t1);
        dep_t2_t1.setConstraint(new StartFinishConstraintImpl());

        TaskDependency dep_t3_t2 = createDependency(t3, t2);
        dep_t3_t2.setConstraint(new StartFinishConstraintImpl());

        assertEquals(TestSetupHelper.newSunday(), t2.getStart());
        assertEquals(TestSetupHelper.newSaturday(), t3.getStart());
    }

}
