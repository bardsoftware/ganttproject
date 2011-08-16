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
package net.sourceforge.ganttproject.test.task;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

public class TestLocalCriticalPath extends TaskTestCase {
    /*
     * This test creates the following hierarchy of tasks and dependencies:
     * t1       --+
     * t2       <-+ --+
     *   n3           |
     *   n2           |
     *     d1 --+     |
     *     d2 <-+ --+ |
     *   n1<--------+ |
     * t3       <-----+
     *
     * The critical path is t1->t2->t3 and inside t2 it is d1->d2->n1
     */
    public void testDeepNestedTask() throws Exception {
        Task t1 = createTask();
        Task t2 = createTask();
        Task t3 = createTask();
        createDependency(t3, t2);
        createDependency(t2, t1);

        Task n1 = createTask();
        n1.move(t2);
        Task n2 = createTask();
        n2.move(t2);
        Task n3 = createTask();
        n3.move(t2);

        Task d1 = createTask();
        d1.move(n2);
        Task d2 = createTask();
        d2.move(n2);
        createDependency(d2, d1);
        createDependency(n1, d2);

        TaskManager mgr = getTaskManager();
        mgr.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
        mgr.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(t2);
        mgr.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
        Set<Task> criticalTasks = new HashSet<Task>(Arrays.asList(
                mgr.getAlgorithmCollection().getCriticalPathAlgorithm().getCriticalTasks()));

        assertTrue(criticalTasks.contains(t1));
        assertTrue(criticalTasks.contains(t2));
        assertTrue(criticalTasks.contains(t3));

        assertTrue(criticalTasks.contains(n1));
        assertTrue(criticalTasks.contains(d1));
        assertTrue(criticalTasks.contains(d2));

        assertFalse(criticalTasks.contains(n2));
        assertFalse(criticalTasks.contains(n3));
    }

    /*
     * This test creates the following hierarchy of tasks and dependencies:
     * t1       ---+
     *   t11  --+  |
     *   t12  <-+  |
     * t2   <---+  |
     * t3   <------+
     *
     * The critical path is t1->t3 and a local critical path inside t1 is t11->t12
     * However there is t11->t2 dependency which goes outside the local critical path.
     */
    public void testDependencyOutside() throws Exception {
        Task t1 = createTask();
        Task t11 = createTask();
        t11.move(t1);
        Task t12 = createTask();
        t12.move(t1);

        Task t2 = createTask();
        Task t3 = createTask();
        t3.setDuration(getTaskManager().createLength(3));

        createDependency(t12, t11);
        createDependency(t2, t11);
        createDependency(t3, t1);

        TaskManager mgr = getTaskManager();
        mgr.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
        mgr.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(t1);
        mgr.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
        Set<Task> criticalTasks = new HashSet<Task>(Arrays.asList(
                mgr.getAlgorithmCollection().getCriticalPathAlgorithm().getCriticalTasks()));
        assertTrue(criticalTasks.contains(t12));
        assertTrue(criticalTasks.contains(t11));
        assertTrue(criticalTasks.contains(t12));
        assertTrue(criticalTasks.contains(t3));
        assertFalse(criticalTasks.contains(t2));
    }

    public void testDependencyFromOutside() throws Exception {
        Task t1 = createTask();
        Task t2 = createTask();
        Task t21 = createTask();
        t21.move(t2);
        Task t22 = createTask();
        t22.move(t2);
        createDependency(t22, t21);
        Task t3 = createTask();
        createDependency(t3, t2);
        createDependency(t21, t1);
        TaskManager mgr = getTaskManager();
        mgr.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
        mgr.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(t2);
        mgr.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();

        Set<Task> criticalTasks = new HashSet<Task>(Arrays.asList(
                mgr.getAlgorithmCollection().getCriticalPathAlgorithm().getCriticalTasks()));
        assertTrue(criticalTasks.contains(t3));
        assertTrue(criticalTasks.contains(t2));
        assertTrue(criticalTasks.contains(t1));
        assertTrue(criticalTasks.contains(t21));
        assertTrue(criticalTasks.contains(t22));
    }
}
