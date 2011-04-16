/***************************************************************************
 RolesSettingsPanel.java
 ------------------------------------------
 begin                : 27 juin 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.gui.options;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.RolesTableModel;
import net.sourceforge.ganttproject.language.GanttLanguage;
/**
 * @author athomas Panel to edit the roles for resources of the project.
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
        rolesTable.setRowHeight(23);
        rolesTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        rolesTable.getColumnModel().getColumn(1).setPreferredWidth(370);

        vb.add(new JScrollPane(rolesTable));

        applyComponentOrientation(language.getComponentOrientation());
    }

    /** This method checks if the value has changed, and asks for commit changes. */
    public boolean applyChanges(boolean askForApply) {
        boolean hasChange = myRolesModel.hasChanges();
        if (hasChange) {
            myRolesModel.applyChanges();
        }
        return hasChange;
    }

    /** Initialize the component. */
    public void initialize() {
        // automatic initialize with the role model
    }
}
