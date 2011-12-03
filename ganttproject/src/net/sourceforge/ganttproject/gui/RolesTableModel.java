/***************************************************************************
 RolesTableModel.java
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
package net.sourceforge.ganttproject.gui;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;

/**
 * Model of table for roles
 *
 * @author athomas
 */
public class RolesTableModel extends AbstractTableModel {

    private final String[] columnNames = { GanttLanguage.getInstance().getText("id"),
            GanttLanguage.getInstance().getText("resourceRole") };

    private final Object[][] data = new Object[100][];

    private final RoleManager myRoleManager;

    public RolesTableModel(RoleManager roleManager) {
        myRoleManager = roleManager;

        Role[] roles = getRoleManager().getProjectLevelRoles();
        int i;
        for (i = 0; i < roles.length; i++) {
            data[i] = new Object[] { String.valueOf(i), roles[i].getName() };
        }
        for (int j = i; j < data.length; j++) {
            data[j] = new Object[2];
        }
    }

    /** @return the number of columns */
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /** @return the number of rows */
    @Override
    public int getRowCount() {
        return data.length;
    }

    /** @return the name of the column at col index */
    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    /** @return the object a specify cell */
    @Override
    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    /** @return true if the cell is editable */
    @Override
    public boolean isCellEditable(int row, int col) {
        return col == 1;
    }

    /*
     * Don't need to implement this method unless your table's data can change.
     */
    @Override
    public void setValueAt(Object value, int row, int col) {
        data[row][col] = value;
        fireTableCellUpdated(row, col);
    }

    public RoleManager getRoleManager() {
        return myRoleManager;
    }

    public boolean hasChanges() {
        Role[] roles = getRoleManager().getProjectLevelRoles();
        for (int i = 0; i < getRowCount(); i++) {
            String nextRoleName = (String) getValueAt(i, 1);
            if (nextRoleName != null) {
                if (i >= roles.length) {
                    return true;
                }
                if (!nextRoleName.equals(roles[i].getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void applyChanges() {
        getRoleManager().getProjectRoleSet().clear();
        for (int i = 0; i < getRowCount(); i++) {
            String nextRoleName = (String) getValueAt(i, 1);
            if (nextRoleName != null) {
                getRoleManager().add(i, nextRoleName);
            }
        }
    }
}
