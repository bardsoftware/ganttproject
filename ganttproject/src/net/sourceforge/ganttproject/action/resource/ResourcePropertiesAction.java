/*
GanttProject is an opensource project management tool. License: GPL2
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

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.GanttDialogPerson;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceContext;

public class ResourcePropertiesAction extends GPAction {
    private final IGanttProject myProject;
    private final UIFacade myUIFacade;
    private final ResourceContext myContext;

    public ResourcePropertiesAction(IGanttProject project, ResourceContext context, UIFacade uiFacade) {
        super("resource.properties");
        myProject = project;
        myUIFacade = uiFacade;
        myContext = context;
    }

    @Override
    protected String getIconFilePrefix() {
        return "properties_";
    }

    public void actionPerformed(ActionEvent arg0) {
        HumanResource[] selectedResources = myContext.getResources();
        if (selectedResources != null) {
            // TODO Allow to edit multiple resources (instead of [0])
            GanttDialogPerson dp = new GanttDialogPerson(myUIFacade, GanttLanguage.getInstance(), selectedResources[0]);
            dp.setVisible(true);
            if (dp.result()) {
                myProject.setModified(true);
            }
        }
    }
}
