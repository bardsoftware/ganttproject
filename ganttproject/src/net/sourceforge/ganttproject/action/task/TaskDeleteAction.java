/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

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

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIFacade.Choice;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

public class TaskDeleteAction extends TaskActionBase {

    public TaskDeleteAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade,
            GanttTree2 tree) {
        super("task.delete", taskManager, selectionManager, uiFacade, tree);
    }

    @Override
    protected String getIconFilePrefix() {
        return "delete_";
    }

    @Override
    protected boolean isEnabled(List<Task> selection) {
        return !selection.isEmpty();
    }

    @Override
    protected boolean askUserPermission(List<Task> selection) {
        Choice choice = getUIFacade().showConfirmationDialog(getI18n("msg19"), getI18n("question"));
        return choice == Choice.YES;
    }

    @Override
    protected void run(List<Task> selection) throws Exception {
        final DefaultMutableTreeNode[] cdmtn = getTree().getSelectedNodes();
        getTree().stopEditing();
        for (DefaultMutableTreeNode node : cdmtn) {
            if (node != null && node instanceof TaskNode) {
                Task task = (Task) node.getUserObject();
                getTree().removeCurrentNode(node);
                task.delete();
            }
        }
        getUIFacade().getGanttChart().reset();
        getUIFacade().getResourceChart().reset();
        getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
    }
}
