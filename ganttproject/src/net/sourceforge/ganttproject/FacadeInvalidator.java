/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

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

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;

class FacadeInvalidator implements TreeModelListener, ProjectEventListener {
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

    public void projectModified() {
        // TODO Auto-generated method stub
    }

    public void projectSaved() {
        // TODO Auto-generated method stub
    }

    public void projectClosed() {
        isValid = false;
    }
}