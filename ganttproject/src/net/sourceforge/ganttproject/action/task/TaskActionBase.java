/*
 * Created on 23.10.2005
 */
package net.sourceforge.ganttproject.action.task;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager.Listener;

abstract class TaskActionBase extends GPAction implements Listener {
    private final TaskManager myTaskManager;
    private List<Task> mySelection;
    private final UIFacade myUIFacade;

    protected TaskActionBase(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade) {
        myTaskManager = taskManager;
        selectionManager.addSelectionListener(this);
        selectionChanged(selectionManager.getSelectedTasks());
        myUIFacade = uiFacade;
    }

    public void actionPerformed(ActionEvent e) {
        final List<Task> selection = new ArrayList<Task>(mySelection);
        myUIFacade.getUndoManager().undoableEdit(getLocalizedName(), new Runnable() {
            public void run() {
                try {
                    TaskActionBase.this.run(selection);
                } catch (Exception e) {
                    getUIFacade().showErrorDialog(e);
                }
            }
        });
    }
    public void selectionChanged(List<Task> currentSelection) {
        setEnabled(isEnabled(currentSelection));
        mySelection = currentSelection;
    }
	public void userInputConsumerChanged(Object newConsumer) {
	}

    protected TaskManager getTaskManager() {
        return myTaskManager;
    }

    protected UIFacade getUIFacade() {
        return myUIFacade;
    }
    protected abstract boolean isEnabled(List<Task> selection);
    protected abstract void run(List<Task> selection) throws Exception ;
}
