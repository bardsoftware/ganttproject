/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.chart.mouse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.chart.TaskChartModelFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.TaskManager;

import org.easymock.EasyMock;

import biz.ganttproject.core.calendar.WeekendCalendarImpl;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Canvas.Rectangle;

/**
 * Tests for {@link ChangeTaskProgressRuler}.
 *
 * @author Dmitry Barashev
 */
public class ChangeTaskProgressRulerTest extends TestCase {

    private static Task createTask(TaskManager taskManager) {
        Task result = taskManager.createTask();
        result.move(taskManager.getRootTask());
        result.setName(String.valueOf(result.getTaskID()));
        return result;
    }

    public void testSimpleRuler() {
        TaskChartModelFacade mockChartModel = EasyMock.createMock(TaskChartModelFacade.class);
        TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder()
            .withCalendar(new WeekendCalendarImpl()).build();
        Task task = createTask(taskManager);
        task.setStart(TestSetupHelper.newMonday());
        task.setDuration(taskManager.createLength(2));

        Canvas primitives = new Canvas();
        Rectangle r = primitives.createRectangle(0, 0, 100, 10);

        assertEquals(1, task.getActivities().size());
        primitives.bind(r, task.getActivities().get(0));

        EasyMock.expect(mockChartModel.getTaskRectangles(task)).andReturn(Collections.singletonList(r));
        EasyMock.replay(mockChartModel);
        ChangeTaskProgressRuler ruler = new ChangeTaskProgressRuler(task, mockChartModel);
        assertEquals(0, ruler.getProgress(-1));
        assertEquals(0, ruler.getProgress(0));
        assertEquals(1, ruler.getProgress(1));
        assertEquals(50, ruler.getProgress(50));
        assertEquals(99, ruler.getProgress(99));
        assertEquals(100, ruler.getProgress(100));
        assertEquals(100, ruler.getProgress(101));
        EasyMock.verify(mockChartModel);
    }

    public void testRulerOverWeekend() {
        TaskChartModelFacade mockChartModel = EasyMock.createMock(TaskChartModelFacade.class);
        TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder()
            .withCalendar(new WeekendCalendarImpl()).build();
        Task task = createTask(taskManager);
        task.setStart(TestSetupHelper.newFriday());
        task.setDuration(taskManager.createLength(2));

        List<TaskActivity> activities = task.getActivities();
        assertEquals(3, activities.size());

        Canvas primitives = new Canvas();
        Rectangle r0 = primitives.createRectangle(0, 0, 100, 10);
        primitives.bind(r0, activities.get(0));

        Rectangle r1 = primitives.createRectangle(100, 0, 10, 10);
        primitives.bind(r1, activities.get(1));

        Rectangle r2 = primitives.createRectangle(110, 0, 100, 10);
        primitives.bind(r2, activities.get(2));

        EasyMock.expect(mockChartModel.getTaskRectangles(task)).andReturn(Arrays.asList(r0, r1, r2));
        EasyMock.replay(mockChartModel);

        ChangeTaskProgressRuler ruler = new ChangeTaskProgressRuler(task, mockChartModel);
        assertEquals(0, ruler.getProgress(-1));
        assertEquals(0, ruler.getProgress(0));
        assertEquals(1, ruler.getProgress(2));
        assertEquals(25, ruler.getProgress(50));
        assertEquals(50, ruler.getProgress(100));
        assertEquals(50, ruler.getProgress(101));
        assertEquals(50, ruler.getProgress(109));
        assertEquals(50, ruler.getProgress(110));
        assertEquals(75, ruler.getProgress(160));
        assertEquals(100, ruler.getProgress(219));
        assertEquals(100, ruler.getProgress(211));
        assertEquals(100, ruler.getProgress(212));

        EasyMock.verify(mockChartModel);
    }
}
