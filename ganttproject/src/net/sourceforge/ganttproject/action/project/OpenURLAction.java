/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2005-2011 GanttProject Team

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
package net.sourceforge.ganttproject.action.project;

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

public class OpenURLAction extends GPAction {
    private final GanttProject myMainFrame;

    OpenURLAction(GanttProject mainFrame) {
        myMainFrame = mainFrame;
    }
    @Override
    protected String getIconFilePrefix() {
        return null;
    }

    public void actionPerformed(ActionEvent e) {
        if (myMainFrame.getProjectUIFacade().ensureProjectSaved(myMainFrame)) {
            myMainFrame.openURL();
        }
    }

    @Override
    protected String getLocalizedName() {
        return GanttLanguage.getInstance().correctLabel(getI18n("openFromServer"));
    }

    @Override
    protected String getTooltipText() {
        return getLocalizedName();
    }


}
