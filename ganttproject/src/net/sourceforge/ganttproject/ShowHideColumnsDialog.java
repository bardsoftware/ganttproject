/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;

import biz.ganttproject.core.table.ColumnList;

import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.tableView.ColumnManagerPanel;

public class ShowHideColumnsDialog {

  private final UIFacade myUIfacade;
  private final CustomPropertyManager myManager;
  private final ColumnList myVisibleFields;

  public ShowHideColumnsDialog(UIFacade facade, ColumnList visibleFields, CustomPropertyManager manager) {
    myUIfacade = facade;
    myVisibleFields = visibleFields;
    myManager = manager;
  }

  public void show() {
    final ColumnManagerPanel panel = new ColumnManagerPanel(myManager, myVisibleFields);
    JComponent component = (JComponent) panel.createComponent();
    myUIfacade.createDialog(component, new Action[] { new OkAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        panel.commitCustomPropertyEdit();
      }
    } }, "Custom Fields Manager").show();
  }
}
