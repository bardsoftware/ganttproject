/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2010 Dmitry Barashev

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
