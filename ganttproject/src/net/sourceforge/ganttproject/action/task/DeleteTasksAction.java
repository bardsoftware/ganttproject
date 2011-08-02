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

public class DeleteTasksAction extends TaskActionBase {

    private final GanttTree2 myTree;

    public DeleteTasksAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade, GanttTree2 tree) {
        super("deleteTask", taskManager, selectionManager, uiFacade);
        myTree = tree;
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
    protected void run(List<Task> selection) throws Exception {
        final DefaultMutableTreeNode[] cdmtn = myTree.getSelectedNodes();
        Choice choice = getUIFacade().showConfirmationDialog(getI18n("msg19"), getI18n("question"));

        if (choice == Choice.YES) {
            getUIFacade().getUndoManager().undoableEdit("Task removed", new Runnable() {
                public void run() {
                    myTree.stopEditing();
                    for (int i = 0; i < cdmtn.length; i++) {
                        if (cdmtn[i] != null && cdmtn[i] instanceof TaskNode) {
                            Task ttask = (Task) (cdmtn[i].getUserObject());
                            myTree.removeCurrentNode(cdmtn[i]);
                            ttask.delete();
                        }
                    }
                }
            });
            getUIFacade().getGanttChart().reset();
            getUIFacade().getResourceChart().reset();
            getTaskManager().getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
        }
    }

}
