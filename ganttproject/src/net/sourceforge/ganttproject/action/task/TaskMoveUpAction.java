/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Dmitry Barashev, GanttProject Team

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
import net.sourceforge.ganttproject.GanttTreeTableModel;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

/**
 * Move selected tasks up
 */
public class TaskMoveUpAction extends TaskActionBase {

    private final GanttTreeTableModel myTreeTableModel;

    public TaskMoveUpAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade, GanttTree2 tree, GanttTreeTableModel treeTableModel) {
        super("task.move.up", taskManager, selectionManager, uiFacade, tree);
        myTreeTableModel = treeTableModel;
    }

    @Override
    protected String getIconFilePrefix() {
        return "up_";
    }

    @Override
    protected boolean isEnabled(List<Task> selection) {
        final DefaultMutableTreeNode[] cdmtn = getTree().getSelectedNodes();
        if(cdmtn == null) {
            return false;
        }
        for (int i = cdmtn.length - 1; i >= 0; i--) {
            DefaultMutableTreeNode parent = GanttTree2.getParentNode(cdmtn[i]);
            if (parent == null || parent.getIndex(cdmtn[i]) - 1 < 0) {
                // We cannot move this node up
                return false;
            }
        }
        return true;
    }

    @Override
    protected void run(List<Task> selection) throws Exception {
        final DefaultMutableTreeNode[] cdmtn = getTree().getSelectedNodes();
        for (int i = 0; i < cdmtn.length; i++) {
            DefaultMutableTreeNode parent = GanttTree2.getParentNode(cdmtn[i]);
            int index = parent.getIndex(cdmtn[i]) - 1;

            if (index >= 0) {
                Task task = (Task) cdmtn[i].getUserObject();
                DefaultMutableTreeNode[] child = new DefaultMutableTreeNode[cdmtn[i].getChildCount()];

                if (task.getExpand()) {
                    for (int j = 0; j < cdmtn[i].getChildCount(); j++) {
                        child[j] = (DefaultMutableTreeNode) cdmtn[i].getChildAt(j);
                    }

                    for (int j = 0; j < child.length; j++) {
                        child[j].removeFromParent();
                        myTreeTableModel.nodesWereRemoved(cdmtn[i], new int[] { 0 }, new Object[] { child });
                    }
                }

                cdmtn[i].removeFromParent();
                myTreeTableModel.nodesWereRemoved(parent, new int[] { index + 1 }, new Object[] { cdmtn });

                parent.insert(cdmtn[i], index);
                myTreeTableModel.nodesWereInserted(parent, new int[] { index });

                if (task.getExpand()) {
                    for (int j = 0; j < child.length; j++) {
                        cdmtn[i].insert(child[j], j);
                        myTreeTableModel.nodesWereInserted(cdmtn[i], new int[] { j });
                    }
                }
                forwardScheduling();
            }

        }
    }
}
