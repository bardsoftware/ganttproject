/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
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

import com.google.common.base.Preconditions;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import java.util.List;

class FacadeInvalidator extends ProjectEventListener.Stub implements TreeModelListener {
  private final List<GanttProjectBase.RowHeightAligner> myRowAligners;
  boolean isValid;

  public FacadeInvalidator(TreeModel treeModel, List<GanttProjectBase.RowHeightAligner> rowHeightAligners) {
    isValid = false;
    treeModel.addTreeModelListener(this);
    myRowAligners = Preconditions.checkNotNull(rowHeightAligners);
  }

  boolean isValid() {
    return isValid;
  }

  void reset() {
    isValid = true;
  }

  @Override
  public void treeNodesChanged(TreeModelEvent e) {
    isValid = false;
  }

  @Override
  public void treeNodesInserted(TreeModelEvent e) {
    isValid = false;
  }

  @Override
  public void treeNodesRemoved(TreeModelEvent e) {
    isValid = false;
  }

  @Override
  public void treeStructureChanged(TreeModelEvent e) {
    isValid = false;
  }

  @Override
  public void projectClosed() {
    isValid = false;
  }

  @Override
  public void projectOpened() {
    for (GanttProjectBase.RowHeightAligner aligner : myRowAligners) {
      aligner.optionsChanged();
    }
  }
}