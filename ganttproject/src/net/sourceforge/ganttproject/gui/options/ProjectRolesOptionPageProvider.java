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

import java.awt.Component;

import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class ProjectRolesOptionPageProvider extends OptionPageProviderBase {
    private RolesSettingsPanel myRolesPanel;
    public ProjectRolesOptionPageProvider() {
        super("project.roles");
    }
    @Override
    public GPOptionGroup[] getOptionGroups() {
        return new GPOptionGroup[0];
    }
    @Override
    public boolean hasCustomComponent() {
        return true;
    }
    @Override
    public Component buildPageComponent() {
        myRolesPanel = new RolesSettingsPanel(getProject());
        myRolesPanel.initialize();
        return OptionPageProviderBase.wrapContentComponent(
            myRolesPanel, 
            GanttLanguage.getInstance().getText("resourceRole"), 
            GanttLanguage.getInstance().getText("settingsRoles"));
        
        
    }
    @Override
    public void commit() {
        myRolesPanel.applyChanges(false);
    }
}
