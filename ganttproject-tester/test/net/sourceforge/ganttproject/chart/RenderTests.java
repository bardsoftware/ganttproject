package net.sourceforge.ganttproject.chart;

import biz.ganttproject.core.calendar.GanttDaysOff;
import biz.ganttproject.core.chart.canvas.Canvas;
import biz.ganttproject.core.chart.canvas.Painter;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import net.sourceforge.ganttproject.action.resource.AssignmentToggleAction;
import net.sourceforge.ganttproject.action.resource.ResourceNewAction;
import net.sourceforge.ganttproject.action.task.TaskNewAction;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class RenderTests extends RenderTestCase {
    public void testTaskAndDayOff(){
        ResourceLoadRenderer renderer = makeResourceLoadRenderer();

        ResourceNewAction resourceNewAction = makeNewResourceAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        HumanResourceManager resourceManager = getHumanResourceManger();
        TaskManager taskManager = getTaskManager();

        resourceNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        Task task_0 = taskManager.getTask(0);
        task_0.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 20));
        task_0.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 21));

        HumanResource resource = resourceManager.getById(0);
        resource.addDaysOff(new GanttDaysOff(new Date(120, Calendar.MAY, 21), new Date(120, Calendar.MAY, 25)));

        AssignmentToggleAction assignmentToggleAction_0 = makeAssignmentToggleAction(resource, task_0);
        assignmentToggleAction_0.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_0.actionPerformed(null);

        renderer.render();
        Canvas temp = renderer.getCanvas();
        temp.paint(makePainter());

        assertEquals(2, getShapes().size());
        assertEquals("dayoff", getShapes().get(0).getStyle());
        assertEquals(44, getShapes().get(0).getTopY());
        assertEquals(54, getShapes().get(0).getBottomY());
        assertEquals(20, getShapes().get(0).getLeftX());
        assertEquals(64, getShapes().get(0).getRightX());
        assertEquals("load.normal.first.last", getShapes().get(1).getStyle());
        assertEquals(44, getShapes().get(1).getTopY());
        assertEquals(54, getShapes().get(1).getBottomY());
        assertEquals(0, getShapes().get(1).getLeftX());
        assertEquals(20, getShapes().get(1).getRightX());
    }

    public void testTasksTogether(){
        ResourceLoadRenderer renderer = makeResourceLoadRenderer();

        ResourceNewAction resourceNewAction = makeNewResourceAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        HumanResourceManager resourceManager = getHumanResourceManger();
        TaskManager taskManager = getTaskManager();

        resourceNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        Task task_0 = taskManager.getTask(0);
        task_0.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 20));
        task_0.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 22));

        Task task_1 = taskManager.getTask(1);
        task_1.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 22));
        task_1.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 25));

        HumanResource resource = resourceManager.getById(0);

        AssignmentToggleAction assignmentToggleAction_0 = makeAssignmentToggleAction(resource, task_0);
        assignmentToggleAction_0.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_0.actionPerformed(null);

        AssignmentToggleAction assignmentToggleAction_1 = makeAssignmentToggleAction(resource, task_1);
        assignmentToggleAction_1.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_1.actionPerformed(null);

        renderer.render();
        Canvas temp = renderer.getCanvas();
        temp.paint(makePainter());

        assertEquals(2, getShapes().size());
        assertEquals("load.normal.first", getShapes().get(0).getStyle());
        assertEquals(44, getShapes().get(0).getTopY());
        assertEquals(54, getShapes().get(0).getBottomY());
        assertEquals(0, getShapes().get(0).getLeftX());
        assertEquals(40, getShapes().get(0).getRightX());
        assertEquals("load.normal.last", getShapes().get(1).getStyle());
        assertEquals(44, getShapes().get(1).getTopY());
        assertEquals(54, getShapes().get(1).getBottomY());
        assertEquals(40, getShapes().get(1).getLeftX());
        assertEquals(60, getShapes().get(1).getRightX());
    }

    public void testTasksSeparate(){
        ResourceLoadRenderer renderer = makeResourceLoadRenderer();

        ResourceNewAction resourceNewAction = makeNewResourceAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        HumanResourceManager resourceManager = getHumanResourceManger();
        TaskManager taskManager = getTaskManager();

        resourceNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        Task task_0 = taskManager.getTask(0);
        task_0.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 19));
        task_0.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 21)); // Cut off first half since "current" day is 20th

        Task task_1 = taskManager.getTask(1);
        task_1.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 22));
        task_1.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 26));  // Will be split by a weekend

        HumanResource resource = resourceManager.getById(0);

        AssignmentToggleAction assignmentToggleAction_0 = makeAssignmentToggleAction(resource, task_0);
        assignmentToggleAction_0.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_0.actionPerformed(null);

        AssignmentToggleAction assignmentToggleAction_1 = makeAssignmentToggleAction(resource, task_1);
        assignmentToggleAction_1.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_1.actionPerformed(null);

        renderer.render();
        Canvas temp = renderer.getCanvas();
        temp.paint(makePainter());

        assertEquals(3, getShapes().size());
        assertEquals("load.normal.first.last", getShapes().get(0).getStyle());
        assertEquals(44, getShapes().get(0).getTopY());
        assertEquals(54, getShapes().get(0).getBottomY());
        assertEquals(0, getShapes().get(0).getLeftX());
        assertEquals(20, getShapes().get(0).getRightX());
        assertEquals("load.normal.first.last", getShapes().get(1).getStyle());
        assertEquals(44, getShapes().get(1).getTopY());
        assertEquals(54, getShapes().get(1).getBottomY());
        assertEquals(40, getShapes().get(1).getLeftX());
        assertEquals(60, getShapes().get(1).getRightX());
        assertEquals("load.normal.first.last", getShapes().get(2).getStyle());
        assertEquals(44, getShapes().get(2).getTopY());
        assertEquals(54, getShapes().get(2).getBottomY());
        assertEquals(64, getShapes().get(2).getLeftX());
        assertEquals(84, getShapes().get(2).getRightX());
    }

    public void testDifferentTaskLoads(){
        ResourceLoadRenderer renderer = makeResourceLoadRenderer();

        ResourceNewAction resourceNewAction = makeNewResourceAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        HumanResourceManager resourceManager = getHumanResourceManger();
        TaskManager taskManager = getTaskManager();

        resourceNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        Task task_0 = taskManager.getTask(0);
        task_0.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 20));
        task_0.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 21));

        Task task_1 = taskManager.getTask(1);
        task_1.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 21));
        task_1.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 22));

        Task task_2 = taskManager.getTask(2);
        task_2.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 22));
        task_2.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 25));

        Task task_3 = taskManager.getTask(3);
        task_3.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 22));
        task_3.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 25));

        HumanResource resource = resourceManager.getById(0);

        AssignmentToggleAction assignmentToggleAction_0 = makeAssignmentToggleAction(resource, task_0);
        assignmentToggleAction_0.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_0.actionPerformed(null);

        resource.getAssignments()[0].setLoad(50);

        AssignmentToggleAction assignmentToggleAction_1 = makeAssignmentToggleAction(resource, task_1);
        assignmentToggleAction_1.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_1.actionPerformed(null);

        AssignmentToggleAction assignmentToggleAction_2 = makeAssignmentToggleAction(resource, task_2);
        assignmentToggleAction_2.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_2.actionPerformed(null);

        AssignmentToggleAction assignmentToggleAction_3 = makeAssignmentToggleAction(resource, task_3);
        assignmentToggleAction_3.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_3.actionPerformed(null);

        renderer.render();
        Canvas temp = renderer.getCanvas();
        temp.paint(makePainter());

        assertEquals(3, getShapes().size());
        assertEquals("load.underload.first", getShapes().get(0).getStyle());
        assertEquals(44, getShapes().get(0).getTopY());
        assertEquals(54, getShapes().get(0).getBottomY());
        assertEquals(0, getShapes().get(0).getLeftX());
        assertEquals(20, getShapes().get(0).getRightX());
        assertEquals("load.normal", getShapes().get(1).getStyle());
        assertEquals(44, getShapes().get(1).getTopY());
        assertEquals(54, getShapes().get(1).getBottomY());
        assertEquals(20, getShapes().get(1).getLeftX());
        assertEquals(40, getShapes().get(1).getRightX());
        assertEquals("load.overload.last", getShapes().get(2).getStyle());
        assertEquals(44, getShapes().get(2).getTopY());
        assertEquals(54, getShapes().get(2).getBottomY());
        assertEquals(40, getShapes().get(2).getLeftX());
        assertEquals(60, getShapes().get(2).getRightX());
    }

    public void testMultipleResources(){
        ResourceLoadRenderer renderer = makeResourceLoadRenderer();

        ResourceNewAction resourceNewAction = makeNewResourceAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        HumanResourceManager resourceManager = getHumanResourceManger();
        TaskManager taskManager = getTaskManager();

        resourceNewAction.actionPerformed(null);
        resourceNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        Task task = taskManager.getTask(0);
        task.setStart(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 20));
        task.setEnd(CalendarFactory.createGanttCalendar(2020, Calendar.MAY, 21));

        HumanResource resource_0 = resourceManager.getById(0);
        HumanResource resource_1 = resourceManager.getById(1);

        AssignmentToggleAction assignmentToggleAction_0 = makeAssignmentToggleAction(resource_0, task);
        assignmentToggleAction_0.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_0.actionPerformed(null);

        AssignmentToggleAction assignmentToggleAction_1 = makeAssignmentToggleAction(resource_1, task);
        assignmentToggleAction_1.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction_1.actionPerformed(null);

        renderer.render();
        Canvas temp = renderer.getCanvas();
        temp.paint(makePainter());

        assertEquals(2, getShapes().size());
        assertEquals("load.normal.first.last", getShapes().get(0).getStyle());
        assertEquals(44, getShapes().get(0).getTopY());
        assertEquals(54, getShapes().get(0).getBottomY());
        assertEquals(0, getShapes().get(0).getLeftX());
        assertEquals(20, getShapes().get(0).getRightX());
        assertEquals("load.normal.first.last", getShapes().get(1).getStyle());
        assertEquals(54, getShapes().get(1).getTopY());
        assertEquals(64, getShapes().get(1).getBottomY());
        assertEquals(0, getShapes().get(1).getLeftX());
        assertEquals(20, getShapes().get(1).getRightX());
    }
}
