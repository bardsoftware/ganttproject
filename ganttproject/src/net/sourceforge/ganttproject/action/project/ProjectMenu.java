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

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;

/**
 * Collection of actions present in the project menu
 */
public class ProjectMenu extends JMenu {

    private final NewProjectAction myNewProjectAction;
    private final SaveProjectAction mySaveProjectAction;
    private final PrintAction myPrintAction;
    private OpenProjectAction myOpenProjectAction;

    public ProjectMenu(final GanttProject project, JMenu mru, String key) {
        super(GPAction.createVoidAction(key));
        myNewProjectAction = new NewProjectAction(project);
        mySaveProjectAction = new SaveProjectAction(project);
        myPrintAction = new PrintAction(project);

        ProjectPropertiesAction projectSettingsAction = new ProjectPropertiesAction(project);
        myOpenProjectAction = new OpenProjectAction(project);
        SaveProjectAsAction saveProjectAsAction = new SaveProjectAsAction(project);
        OpenURLAction openURLAction = new OpenURLAction(project);
        SaveURLAction saveURLAction = new SaveURLAction(project);
        ExitAction exitAction = new ExitAction(project);
        ProjectImportAction projectImportAction = new ProjectImportAction(project.getUIFacade(), project);
        ProjectExportAction projectExportAction = new ProjectExportAction(project.getUIFacade(), project, project
                .getGanttOptions());

        add(projectSettingsAction);
        add(myNewProjectAction);
        add(myOpenProjectAction);
        add(mru);

        addSeparator();
        add(mySaveProjectAction);
        add(saveProjectAsAction);
        addSeparator();

        add(projectImportAction);
        add(projectExportAction);
        addSeparator();

        JMenu mServer = new JMenu(GPAction.createVoidAction("webServer"));
        mServer.add(openURLAction);
        mServer.add(saveURLAction);
        add(mServer);

        addSeparator();
        add(myPrintAction);
        add(new ProjectPreviewAction(project));
        addSeparator();
        add(exitAction);
    }

    public AbstractAction getNewProjectAction() {
        return myNewProjectAction;
    }

    public GPAction getSaveProjectAction() {
        return mySaveProjectAction;
    }

    public AbstractAction getPrintAction() {
        return myPrintAction;
    }

    public OpenProjectAction getOpenProjectAction() {
        return myOpenProjectAction;
    }

}
