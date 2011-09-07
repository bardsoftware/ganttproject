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

import java.util.HashSet;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.GanttTreeTableModel;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

/**
 * Unindent several nodes that are selected
 */
public class TaskUnindentAction extends TaskActionBase {

    private final GanttTreeTableModel myTreeTableModel;

    public TaskUnindentAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade,
            GanttTree2 tree, GanttTreeTableModel treeTableModel) {
        super("task.unindent", taskManager, selectionManager, uiFacade, tree);
        myTreeTableModel = treeTableModel;
    }

    @Override
    protected String getIconFilePrefix() {
        return "unindent_";
    }

    @Override
    protected boolean isEnabled(List<Task> selection) {
        final DefaultMutableTreeNode[] cdmtn = getTree().getSelectedNodes();
        if(cdmtn == null) {
            return false;
        }
        final DefaultMutableTreeNode root = getTree().getRoot();
        for(DefaultMutableTreeNode node : cdmtn) {
            if(root.equals(GanttTree2.getParentNode(node))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void run(List<Task> selection) throws Exception {
        final DefaultMutableTreeNode[] cdmtn = getTree().getSelectedNodes();
        final TreePath[] selectedPaths = new TreePath[cdmtn.length];

        // Information about previous node is needed to determine if current node had sibling that was moved.
        DefaultMutableTreeNode previousParent = new DefaultMutableTreeNode();
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode();

        HashSet<Task> targetContainers = new HashSet<Task>();
        for (int i = 0; i < cdmtn.length; i++) {

            // We use information about previous father to determine new index of the node in the tree.
            if (i > 0) {
                previousParent = parent;
            }
            parent = GanttTree2.getParentNode(cdmtn[i]);

            // Getting the fathers father !? The grandpa I think  :)
            DefaultMutableTreeNode newParent = GanttTree2.getParentNode(parent);
            // If no grandpa is available we must stop.
            if (newParent == null) {
                continue;
            }

            int oldIndex = parent.getIndex(cdmtn[i]);

            cdmtn[i].removeFromParent();
            myTreeTableModel.nodesWereRemoved(parent, new int[] { oldIndex }, new Object[] { cdmtn });

            targetContainers.add((Task) parent.getUserObject());
            // If node and previous node were siblings add current node after its previous sibling
            int newIndex;
            if (i > 0 && parent.equals(previousParent) ) {
                newIndex = newParent.getIndex(cdmtn[i-1]) + 1;
            } else {
                newIndex = newParent.getIndex(parent) + 1;
            }

            myTreeTableModel.insertNodeInto(cdmtn[i], newParent, newIndex);

            // Select again this node
            TreeNode[] treepath = cdmtn[i].getPath();
            TreePath path = new TreePath(treepath);
            // tree.setSelectionPath(path);
            selectedPaths[i] = path;

            getTree().expandRefresh(cdmtn[i]);

            if (parent.getChildCount() == 0) {
                ((Task) parent.getUserObject()).setProjectTask(false);
            }
        }
        getTaskManager().getAlgorithmCollection().getAdjustTaskBoundsAlgorithm().run(
                targetContainers.toArray(new Task[0]));
        forwardScheduling();
        getTree().setSelectionPaths(selectedPaths);
    }
}
