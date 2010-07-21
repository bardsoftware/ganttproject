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

import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.RolesTableModel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.RoleManager;

/**
 * @author athomas Panel to edit the roles for resources of the project.
 */
public class RolesSettingsPanel extends GeneralOptionPanel {

    RolesTableModel myRolesModel;

    JTable rolesTable;

    private GanttProject appli;

    /** Constructor. */
    public RolesSettingsPanel(GanttProject parent) {
        super(GanttProject.correctLabel(GanttLanguage.getInstance().getText(
                "resourceRole")), GanttLanguage.getInstance().getText(
                "settingsRoles"), parent);

        appli = parent;
        myRolesModel = new RolesTableModel();
        rolesTable = new JTable(myRolesModel);
        rolesTable.setPreferredScrollableViewportSize(new Dimension(400, 350));
        rolesTable.setRowHeight(23);
        rolesTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        rolesTable.getColumnModel().getColumn(1).setPreferredWidth(370);

        vb.add(new JScrollPane(rolesTable));

        applyComponentOrientation(language.getComponentOrientation());
    }

    /** This method check if the value has changed, and assk for commit changes. */
    public boolean applyChanges(boolean askForApply) {
        System.err.println("[RolesSettingsPanel] applyChanges(): ");
        RoleManager roleManager = myRolesModel.getRoleManager();
        bHasChange = myRolesModel.hasChanges();
        if (!bHasChange) {
            System.err
                    .println("[RolesSettingsPanel] applyChanges(): no changes");
            return bHasChange;
        }
        myRolesModel.applyChanges();
        appli.setAskForSave(true);
        return bHasChange;
    }

    /** Initialize the component. */
    public void initialize() {
        // automatic initialize with the role model
    }

}
