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

import net.sourceforge.ganttproject.GanttProject;

/**
 * Collection of actions present in the project menu
 */
public class ProjectMenu {
    private final NewProjectAction myNewProjectAction;
    private final OpenProjectAction myOpenProjectAction;
    private final SaveProjectAction mySaveProjectAction;
    private final SaveProjectAsAction mySaveProjectAsAction;
    private final OpenURLAction myOpenURLAction;
    private final ExitAction myExitAction;
    private final SaveURLAction mySaveURLAction;
    private final PrintAction myPrintAction;
    private final ProjectImportAction myProjectImportAction;
    private final ProjectExportAction myProjectExportAction;
    private final ProjectPropertiesAction myProjectSettingsAction;

    public ProjectMenu(final GanttProject mainFrame) {
        myProjectSettingsAction = new ProjectPropertiesAction(mainFrame);
        myNewProjectAction = new NewProjectAction(mainFrame);
        myOpenProjectAction = new OpenProjectAction(mainFrame);
        mySaveProjectAction =new SaveProjectAction(mainFrame);
        mySaveProjectAsAction =new SaveProjectAsAction(mainFrame);
        myOpenURLAction = new OpenURLAction(mainFrame);
        mySaveURLAction = new SaveURLAction(mainFrame);
        myPrintAction = new PrintAction(mainFrame);
        myExitAction = new ExitAction(mainFrame);
        myProjectImportAction = new ProjectImportAction(mainFrame.getUIFacade(), mainFrame);
        myProjectExportAction = new ProjectExportAction(mainFrame.getUIFacade(), mainFrame, mainFrame.getGanttOptions());
    }
    public AbstractAction getNewProjectAction() {
        return myNewProjectAction;
    }
    public AbstractAction getOpenProjectAction() {
        return myOpenProjectAction;
    }
    public AbstractAction getSaveProjectAction() {
        return mySaveProjectAction;
    }
    public AbstractAction getSaveProjectAsAction() {
        return mySaveProjectAsAction;
    }
    public AbstractAction getOpenURLAction() {
        return myOpenURLAction;
    }
    public AbstractAction getExitAction() {
        return myExitAction;
    }
    public AbstractAction getSaveURLAction() {
        return mySaveURLAction;
    }
    public AbstractAction getPrintAction() {
        return myPrintAction;
    }
    public AbstractAction getProjectImportAction() {
        return myProjectImportAction;
    }
    public AbstractAction getProjectExportAction() {
        return myProjectExportAction;
    }
    public AbstractAction getProjectSettingsAction() {
        return myProjectSettingsAction;
    }
}
