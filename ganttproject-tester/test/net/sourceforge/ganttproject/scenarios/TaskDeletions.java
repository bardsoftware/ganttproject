package net.sourceforge.ganttproject.scenarios;

import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import biz.ganttproject.core.calendar.AlwaysWorkingTimeCalendarImpl;
import biz.ganttproject.core.calendar.GPCalendarCalc;

import biz.ganttproject.core.option.*;

import biz.ganttproject.core.time.TimeUnitStack;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;

import com.google.common.base.Suppliers;
import com.google.common.base.Supplier;

import net.sourceforge.ganttproject.action.task.TaskDeleteAction;

import net.sourceforge.ganttproject.action.task.TaskNewAction;
import net.sourceforge.ganttproject.action.zoom.ZoomActionSet;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.GanttChart;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.*;

import net.sourceforge.ganttproject.gui.scrolling.ScrollingManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.*;

import net.sourceforge.ganttproject.undo.GPUndoListener;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import net.sourceforge.ganttproject.undo.UndoManagerImpl;

import javax.swing.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class TaskDeletions extends ScenarioTestCase {
    public void testTaskDeleteAction() {
        Task[] tasks;
        ArrayList<Task> selection;
        TaskManager taskManager = getTaskManager();
        TaskDeleteAction taskDeleteAction = makeDeleteAction();
        TaskNewAction taskNewAction = makeNewAction();

        taskNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        selection = new ArrayList<Task> ();
        selection.add(taskManager.getTask(0));

        taskDeleteAction.selectionChanged(selection);

        assertEquals(2, taskManager.getTaskCount());

        taskDeleteAction.actionPerformed(null);

        assertEquals(1, taskManager.getTaskCount());
    }
}
