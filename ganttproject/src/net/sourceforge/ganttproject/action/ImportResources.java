/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject team

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

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.gui.OpenFileDialog;
import net.sourceforge.ganttproject.io.GanttXMLOpen;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.DependencyTagHandler;
import net.sourceforge.ganttproject.parser.ResourceTagHandler;
import net.sourceforge.ganttproject.parser.RoleTagHandler;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * Action connected to the menu item for importing some resources
 */
public class ImportResources extends AbstractAction {
    private final TaskManager myTaskManager;

    private final HumanResourceManager myResourceManager;

    private final GanttProject myproject;

    private final RoleManager myRoleManager;

    private File startFile = null;

    public ImportResources(HumanResourceManager resourceManager,
            TaskManager taskManager, RoleManager roleManager,
            GanttProject project) {
        myTaskManager = taskManager;
        myRoleManager = roleManager;
        GanttLanguage language = GanttLanguage.getInstance();

        putValue(AbstractAction.NAME, language.getText("importResources"));
        myResourceManager = resourceManager;

        URL iconUrl = this.getClass().getClassLoader().getResource(
                "icons/impres_16.gif");
        if (iconUrl != null) {
            putValue(Action.SMALL_ICON, new ImageIcon(iconUrl));
        }

        myproject = project;
    }

    public void actionPerformed(ActionEvent event) {
        final File file = getResourcesFile();
        if (file != null) {
            myproject.getUndoManager().undoableEdit("Import Resources",
                    new Runnable() {
                        public void run() {
                            GanttXMLOpen loader = new GanttXMLOpen(
                                    myTaskManager);
                            ResourceTagHandler tagHandler = new ResourceTagHandler(
                                    myResourceManager, myRoleManager, myproject.getResourceCustomPropertyManager());
                            DependencyTagHandler dependencyHandler = new DependencyTagHandler(
                                    loader.getContext(), myTaskManager, myproject.getUIFacade());
                            RoleTagHandler rolesHandler = new RoleTagHandler(
                                    RoleManager.Access.getInstance());
                            loader.addTagHandler(tagHandler);
                            loader.addTagHandler(dependencyHandler);
                            loader.addTagHandler(rolesHandler);
                            loader.load(file);
                            // myproject.setQuickSave (true);
                            // myproject.quickSave ("Import Resources");
                        }
                    });
        }
    }

    private File getResourcesFile() {
        OpenFileDialog openDialog;
        if (startFile != null) {
            openDialog = new OpenFileDialog(startFile.getPath());
        } else {
            openDialog = new OpenFileDialog();
        }
        File result = openDialog.show();
        if (result != null) {
            startFile = result;
        }
        return result;
    }
}
