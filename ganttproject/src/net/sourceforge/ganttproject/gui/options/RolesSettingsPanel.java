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
package net.sourceforge.ganttproject.gui.options;

import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.RolesTableModel;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Panel to edit the roles for resources of the project.
 *
 * @author athomas
 */
public class RolesSettingsPanel extends GeneralOptionPanel {

    private final RolesTableModel myRolesModel;

    private final JTable rolesTable;

    public RolesSettingsPanel(IGanttProject project) {
        super(GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "resourceRole")), GanttLanguage.getInstance().getText(
                "settingsRoles"));

        myRolesModel = new RolesTableModel(project.getRoleManager());
        rolesTable = new JTable(myRolesModel);
        // Set PreferredScrollableViewportSize to 0, so the (parent) dialog scrollbars are never visible/used.
        // (this prevents having two scrollbars next to each other)
        rolesTable.setPreferredScrollableViewportSize(new Dimension(0, 0));
        rolesTable.setRowHeight(23);
        rolesTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        rolesTable.getColumnModel().getColumn(1).setPreferredWidth(370);

        vb.add(new JScrollPane(rolesTable));

        applyComponentOrientation(language.getComponentOrientation());
    }

    /** This method checks if the value has changed, and asks for commit changes. */
    public boolean applyChanges(boolean askForApply) {
        bHasChange = myRolesModel.hasChanges();
        if (bHasChange) {
            myRolesModel.applyChanges();
        }
        return bHasChange;
    }

    /** Initialize the component. */
    public void initialize() {
        // automatic initialize with the role model
    }
}
