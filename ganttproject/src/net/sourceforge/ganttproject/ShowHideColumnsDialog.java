/* LICENSE: GPL2
Copyright (C) 2009 Dmitry Barashev

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

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;

import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.tableView.ColumnManagerPanel;


public class ShowHideColumnsDialog {

    private final UIFacade myUIfacade;
    private final CustomPropertyManager myManager;
    private final TableHeaderUIFacade myVisibleFields;

    public ShowHideColumnsDialog(UIFacade facade, TableHeaderUIFacade visibleFields, CustomPropertyManager manager) {
        myUIfacade = facade;
        myVisibleFields = visibleFields;
        myManager = manager;
    }

    public void show() {
        final ColumnManagerPanel panel = new ColumnManagerPanel(myManager, myVisibleFields);
        JComponent component = (JComponent) panel.createComponent();
        myUIfacade.createDialog(component, new Action[] {new OkAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel.commitCustomPropertyEdit();
            }
        }}, "Custom Fields Manager").show();
    }
}