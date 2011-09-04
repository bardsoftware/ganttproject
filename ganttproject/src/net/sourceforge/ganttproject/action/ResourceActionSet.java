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
package net.sourceforge.ganttproject.action;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.ResourceContext;
import net.sourceforge.ganttproject.resource.HumanResourceManager;

public class ResourceActionSet {
    private final DeleteHumanAction myDeleteHumanAction;

    private final HumanResourceManager myManager;

    private final GanttProject myProjectFrame;

    private AbstractAction[] myActions;

    public ResourceActionSet(IGanttProject project, ResourceContext context,
            GanttProject projectFrame, UIFacade uiFacade) {
        myManager = project.getHumanResourceManager();
        myProjectFrame = projectFrame;
        myDeleteHumanAction = new DeleteHumanAction(myManager, context, myProjectFrame, uiFacade);
    }

    public AbstractAction[] getActions() {
        if (myActions == null) {
            myActions = new AbstractAction[] {
                    new NewHumanAction(myManager, myProjectFrame),
                    myDeleteHumanAction };
        }
        return myActions;
    }

    public Action getDeleteHumanAction() {
        return myDeleteHumanAction;
    }
}
