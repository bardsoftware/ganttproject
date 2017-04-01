/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.resource;

import net.sourceforge.ganttproject.ResourceTreeTable;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIUtil;

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ResourceMoveUpAction extends GPAction implements TreeSelectionListener {
  private final ResourceTreeTable myTable;

  public ResourceMoveUpAction(ResourceTreeTable table) {
    super("resource.move.up");
    myTable = table;
    setEnabled(false);
    table.getTree().getTreeSelectionModel().addTreeSelectionListener(this);
  }

  @Override
  public void valueChanged(TreeSelectionEvent e) {
    setEnabled(myTable.canMoveSelectionUp());
  }

  @Override
  protected String getIconFilePrefix() {
    return "up_";
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    myTable.upResource();
  }

  @Override
  public GPAction asToolbarAction() {
    final ResourceMoveUpAction result = new ResourceMoveUpAction(myTable);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    this.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if ("enabled".equals(evt.getPropertyName())) {
          result.setEnabled((Boolean)evt.getNewValue());
        }
      }
    });
    result.setEnabled(this.isEnabled());
    return result;
  }
}
