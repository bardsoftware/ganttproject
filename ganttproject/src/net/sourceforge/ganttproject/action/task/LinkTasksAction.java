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

import java.util.Iterator;
import java.util.List;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class LinkTasksAction extends TaskActionBase {
    public LinkTasksAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade) {
        super(taskManager, selectionManager, uiFacade);
    }

    protected String getIconFilePrefix() {
        return "link_";
    }

    protected String getLocalizedName() {
        return getI18n("link");
    }


    protected void run(List<Task> selection) throws TaskDependencyException {
        for (int i=0; i<selection.size()-1; i++) {
            Task dependant = selection.get(i+1);
            Task dependee = selection.get(i);
            if (getTaskManager().getDependencyCollection().canCreateDependency(dependant, dependee)) {
                getTaskManager().getDependencyCollection().createDependency(dependant, dependee);
            }
        }
        // Update (un)link buttons
        getSelectionManager().fireSelectionChanged();
    }                

    protected boolean isEnabled(List<Task> selection) {
        if(selection.size() <= 1) {
            return false;
        }
        Iterator<Task> it = selection.iterator();
        while (it.hasNext()) {
            Task nextTask = it.next();
            if (nextTask.getDependencies().hasLinks(selection) == false ) {
                return true;
            }
        }
        return false;
    }
}
