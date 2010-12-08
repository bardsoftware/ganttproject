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

public class UnlinkTasksAction extends TaskActionBase {

    public UnlinkTasksAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade) {
        super(taskManager, selectionManager, uiFacade);
    }

    protected String getIconFilePrefix() {
        return "unlink_";
    }

    protected String getLocalizedName() {
        return getI18n("unlink");
    }

    protected boolean isEnabled(List<Task> selection) {
        Iterator<Task> it = selection.iterator();
        while (it.hasNext()) {
            Task nextTask = it.next();
            if (nextTask.getDependencies().hasLinks(selection)) {
                return true;
            }
        }
        return false;
    }

    protected void run(List<Task> selection) throws Exception {
        for (int i=0; i<selection.size(); i++) {
            Task nextTask = selection.get(i);
            nextTask.getDependencies().clear(selection);
        }
        // Update (un)link buttons
        getSelectionManager().fireSelectionChanged();
    }
}
