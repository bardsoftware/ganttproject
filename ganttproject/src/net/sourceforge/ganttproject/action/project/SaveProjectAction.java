/*
GanttProject is an opensource project management tool.
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
import net.sourceforge.ganttproject.ProjectEventListener;
import net.sourceforge.ganttproject.action.GPAction;

class SaveProjectAction extends GPAction implements ProjectEventListener {
    private final GanttProject myMainFrame;

    SaveProjectAction(GanttProject mainFrame) {
        super("project.save");
        myMainFrame = mainFrame;
        mainFrame.addProjectEventListener(this);
        setEnabled(false);
    }

    @Override
    protected String getIconFilePrefix() {
        return "save_";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        myMainFrame.saveProject();
    }

    @Override
    public void projectModified() {
        setEnabled(true);
    }

    @Override
    public void projectSaved() {
        setEnabled(false);
    }

    @Override
    public void projectClosed() {
        setEnabled(false);
    }

    public void projectCreated() {
        setEnabled(false);
    }

    public void projectOpened() {
        setEnabled(false);
    }
}
