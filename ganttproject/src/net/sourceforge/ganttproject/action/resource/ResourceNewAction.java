/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.resource;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.gui.GanttDialogPerson;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;

/**
 * Action connected to the menu item for insert a new resource
 */
public class ResourceNewAction extends ResourceAction {
    private final UIFacade myUIFacade;

    private final RoleManager myRoleManager;

    public ResourceNewAction(HumanResourceManager hrManager, RoleManager roleManager,
            UIFacade uiFacade) {
        super("resource.new", hrManager);
        myUIFacade = uiFacade;
        myRoleManager = roleManager;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final HumanResource resource = getManager().newHumanResource();
        resource.setRole(myRoleManager.getDefaultRole());
        GanttDialogPerson dp = new GanttDialogPerson(myUIFacade, GanttLanguage.getInstance(), resource);
        dp.setVisible(true);
        if (dp.result()) {
            myUIFacade.getUndoManager().undoableEdit("new Resource", new Runnable() {
                public void run() {
                    getManager().add(resource);
                }
            });
        }
    }

    @Override
    protected String getIconFilePrefix() {
        return "insert_";
    }
}
