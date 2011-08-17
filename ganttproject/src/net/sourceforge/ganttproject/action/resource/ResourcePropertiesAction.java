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
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.GanttDialogPerson;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceContext;

public class ResourcePropertiesAction extends GPAction {
    private final IGanttProject myProject;
    private final UIFacade myUIFacade;
    private HumanResource mySelectedResource;

    public ResourcePropertiesAction(IGanttProject project, UIFacade uiFacade) {
        myProject = project;
        myUIFacade = uiFacade;
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK));
    }
    @Override
    protected String getLocalizedName() {
        return getI18n("propertiesHuman");
    }

    @Override
    protected String getTooltipText() {
        return getI18n("propertiesHuman");
    }

    @Override
    protected String getIconFilePrefix() {
        return "properties_";
    }

    public void actionPerformed(ActionEvent arg0) {
        if (getSelectedResource() != null) {
            GanttDialogPerson dp = new GanttDialogPerson(getUIFacade(), getSelectedResource());
            dp.setVisible(true);
            if (dp.result()) {
                getProject().setModified(true);
            }
        }
    }

    private IGanttProject getProject() {
        return myProject;
    }

    private UIFacade getUIFacade() {
        return myUIFacade;
    }

    private HumanResource getSelectedResource() {
        return mySelectedResource;
    }

    public void setContext(ResourceContext context) {
        HumanResource[] resources = context.getResources();
        if (resources.length == 1) {
            mySelectedResource = resources[0];
            setEnabled(true);
        } else {
            setEnabled(false);
        }
    }
}
