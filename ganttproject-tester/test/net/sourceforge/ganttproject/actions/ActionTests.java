package net.sourceforge.ganttproject.actions;

import java.util.ArrayList;

import net.sourceforge.ganttproject.action.resource.ResourceNewAction;
import net.sourceforge.ganttproject.action.task.TaskDeleteAction;

import net.sourceforge.ganttproject.action.task.TaskNewAction;
import net.sourceforge.ganttproject.task.*;

public class ActionTests extends ActionTestCase {
    public void testTaskDeleteActionWithSelectedTask() {
        start();

        ArrayList<Task> selection;
        TaskManager taskManager = getTaskManager();
        TaskDeleteAction taskDeleteAction = makeDeleteTaskAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        taskNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        selection = new ArrayList<Task> ();

        assertEquals(2, taskManager.getTaskCount());

        selection.add(taskManager.getTask(1));
        taskDeleteAction.selectionChanged(selection);
        taskDeleteAction.actionPerformed(null);

        assertEquals(1, taskManager.getTaskCount());

        stop();
    }

    public void testTaskDeleteActionWithoutSelectedTask(){
        start();

        TaskManager taskManager = getTaskManager();
        TaskDeleteAction taskDeleteAction = makeDeleteTaskAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        taskNewAction.actionPerformed(null);

        assertEquals(1, taskManager.getTaskCount());

        taskDeleteAction.selectionChanged(new ArrayList<Task>());
        taskDeleteAction.actionPerformed(null);

        assertEquals(1, taskManager.getTaskCount());

        stop();
    }
}
