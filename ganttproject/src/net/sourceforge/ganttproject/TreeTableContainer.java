/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Enumeration;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jdesktop.swing.treetable.DefaultTreeTableModel;

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TreeUiFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.util.collect.Pair;

/**
 *
 * @author dbarashev (Dmitry Barashev)
 *
 * @param <ModelObject>
 * @param <TreeTableClass>
 * @param <TreeTableModelClass>
 */
public abstract class TreeTableContainer<ModelObject, TreeTableClass extends GPTreeTableBase, TreeTableModelClass extends DefaultTreeTableModel>
        extends JPanel implements TreeUiFacade<ModelObject> {
    private final TreeTableClass myTreeTable;
    private final TreeTableModelClass myTreeTableModel;

    private class ExpandCollapseAction extends GPAction {
        ExpandCollapseAction() {
            super("tree.expand");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TreePath currentSelection = getTree().getSelectionPath();
            if (currentSelection != null) {
                if (getTree().isCollapsed(currentSelection)) {
                    getTree().expandPath(currentSelection);
                } else {
                    getTree().collapsePath(currentSelection);
                }
            }
        }
    }
    public TreeTableContainer(Pair<TreeTableClass, TreeTableModelClass> tableAndModel) {
        super(new BorderLayout());
        myTreeTableModel = tableAndModel.second();
        myTreeTable = tableAndModel.first();
        myTreeTable.getTree().getModel().addTreeModelListener(new ChartUpdater());
        ExpandCollapseAction expandAction = new ExpandCollapseAction();
        for (KeyStroke ks : GPAction.getAllKeyStrokes(expandAction.getID())) {
            UIUtil.pushAction(myTreeTable, ks, expandAction);
        }
        this.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        getTreeTable().getTable().requestFocusInWindow();
                    }
                });
            }
        });

    }

    protected JTree getTree() {
        return getTreeTable().getTree();
    }

    protected TreeTableClass getTreeTable() {
        return myTreeTable;
    }

    protected TreeTableModelClass getTreeModel() {
        return myTreeTableModel;
    }


    @Override
    public Component getTreeComponent() {
        return this;
    }

    @Override
    public TableHeaderUIFacade getVisibleFields() {
        return myTreeTable.getVisibleFields();
    }

    @Override
    public boolean isExpanded(ModelObject modelObject) {
        DefaultMutableTreeNode treeNode = getNode(modelObject);
        return treeNode == null ? false : !myTreeTable.getTree().isCollapsed(new TreePath(treeNode.getPath()));
    }

    @Override
    public void setExpanded(ModelObject modelObject) {
        DefaultMutableTreeNode treeNode = getNode(modelObject);
        if (treeNode != null) {
            myTreeTable.getTree().expandPath(new TreePath(treeNode.getPath()));
        }
    }

    public int getRowHeight() {
        return myTreeTable.getTable().getRowHeight();
    }

    protected DefaultMutableTreeNode getNode(ModelObject modelObject) {
        for (Enumeration<TreeNode> nodes = getRootNode().preorderEnumeration(); nodes.hasMoreElements();) {
            DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode) nodes.nextElement();
            if (nextNode.getUserObject().equals(modelObject)) {
                return nextNode;
            }
        }
        return null;
    }

    protected DefaultMutableTreeNode getSelectedNode() {
        TreePath currentSelection = getTreeTable().getTree().getSelectionPath();
        return (currentSelection == null) ? null :
            (DefaultMutableTreeNode) currentSelection.getLastPathComponent();
    }

    protected abstract DefaultMutableTreeNode getRootNode();
    protected abstract Chart getChart();

    private class ChartUpdater implements TreeModelListener {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            getChart().reset();
        }
        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            getChart().reset();
        }
        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            getChart().reset();
        }
        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            getChart().reset();
        }
    }

}
