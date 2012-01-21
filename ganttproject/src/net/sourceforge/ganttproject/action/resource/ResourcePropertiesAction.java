/*
GanttProject is an opensource project management tool. License: GPL3
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

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.GanttDialogPerson;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceContext;

public class ResourcePropertiesAction extends ResourceAction {
    private final IGanttProject myProject;
    private final UIFacade myUIFacade;
    private final ResourceContext myContext;

    public ResourcePropertiesAction(IGanttProject project, ResourceContext context, UIFacade uiFacade) {
        this(project, context, uiFacade, IconSize.MENU);
    }

    private ResourcePropertiesAction(IGanttProject project, ResourceContext context, UIFacade uiFacade, IconSize size) {
        super("resource.properties", null, size);
        myProject = project;
        myUIFacade = uiFacade;
        myContext = context;
    }

    @Override
    public GPAction withIcon(IconSize size) {
        return new ResourcePropertiesAction(myProject, myContext, myUIFacade, size);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        HumanResource[] selectedResources = myContext.getResources();
        if (selectedResources != null) {
            // TODO Allow to edit multiple resources (instead of [0])
            GanttDialogPerson dp = new GanttDialogPerson(myUIFacade, selectedResources[0]);
            dp.setVisible(true);
            if (dp.result()) {
                myProject.setModified(true);
            }
        }
    }
}
