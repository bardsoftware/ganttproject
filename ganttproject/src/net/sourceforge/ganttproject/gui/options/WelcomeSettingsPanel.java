/*
GanttProject is an opensource project management tool.
Copyright (C) 200242011 Alexandre Thomas, GanttProject Team

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

import net.sourceforge.ganttproject.GanttProject;

/**
 * Simple class for welcome panel.
 * 
 * @author athomas
 */
public class WelcomeSettingsPanel extends GeneralOptionPanel {
    public WelcomeSettingsPanel(GanttProject parent) {
        super(language.getCorrectedLabel("settings"), language
                .getText("settingsWelcome"));

        applyComponentOrientation(language.getComponentOrientation());
    }

    /** This method checks if the value has changed, and asks for commit changes. */
    public boolean applyChanges(boolean askForApply) {
        return false;
    }

    /** Initialize the component. */
    public void initialize() {
    }
}
