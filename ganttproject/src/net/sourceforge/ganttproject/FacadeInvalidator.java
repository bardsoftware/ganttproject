/*
 * Created on 29.09.2005
 */
package net.sourceforge.ganttproject;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;

class FacadeInvalidator extends ProjectEventListener.Stub implements TreeModelListener {
    boolean isValid;

    public FacadeInvalidator(TreeModel treeModel) {
        isValid = false;
        treeModel.addTreeModelListener(this);
    }

    boolean isValid() {
        return isValid;
    }

    void reset() {
        isValid = true;
    }

    public void treeNodesChanged(TreeModelEvent e) {
        isValid = false;
    }

    public void treeNodesInserted(TreeModelEvent e) {
        isValid = false;
    }

    public void treeNodesRemoved(TreeModelEvent e) {
        isValid = false;
    }

    public void treeStructureChanged(TreeModelEvent e) {
        isValid = false;
    }

    public void projectClosed() {
        isValid = false;
    }
}