/*
GanttProject is an opensource project management tool.
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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIFacade.Choice;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.ResourceContext;

/**
 * Action connected to the menu item for delete a resource
 */
public class DeleteHumanAction extends ResourceAction {
    private final UIFacade myUIFacade;

    private final ResourceContext myContext;

    private static final String ICON_URL = "icons/delete_16.gif";

    private final int MENU_MASK = Toolkit.getDefaultToolkit()
            .getMenuShortcutKeyMask();

    private GanttProject myProject;

    public DeleteHumanAction(HumanResourceManager hrManager, ResourceContext context, GanttProject project, UIFacade uiFacade) {
        super(hrManager);
        myUIFacade = uiFacade;
        myProject = project;
        putValue(AbstractAction.NAME, language.getCorrectedLabel("deleteHuman"));
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_J, MENU_MASK));
        URL iconUrl = getClass().getClassLoader().getResource(ICON_URL);
        if (iconUrl != null) {
            putValue(Action.SMALL_ICON, new ImageIcon(iconUrl));
        }
        myContext = context;
    }

    public void actionPerformed(ActionEvent event) {
        final HumanResource[] context = getContext().getResources();
        if (context.length > 0) {
            final String message = language.getText("msg6") + " " + getDisplayName(context) + "?";
            final String title = language.getText("question");
            Choice choice = myUIFacade.showConfirmationDialog(message, title);
            if (choice == Choice.YES) {
                myUIFacade.getUndoManager().undoableEdit("Resource removed", new Runnable() {
                    public void run() {
                        deleteResources(context);
                        getProjectFrame().repaint2();
                    }
                });
            }
        }
    }

    private GanttProject getProjectFrame() {
        return myProject;
    }

    private void deleteResources(HumanResource[] resources) {
        for (HumanResource resource : resources) {
            resource.delete();
        }
    }

    private String getDisplayName(HumanResource[] resources) {
        if (resources.length == 1) {
            return resources[0].toString();
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < resources.length; i++) {
            result.append(resources[i].toString());
            if (i < resources.length - 1) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    private ResourceContext getContext() {
        return myContext;
    }
}
