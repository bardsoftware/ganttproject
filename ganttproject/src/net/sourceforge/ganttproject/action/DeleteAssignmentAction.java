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

import java.awt.event.ActionEvent;

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIFacade.Choice;
import net.sourceforge.ganttproject.resource.AssignmentContext;
import net.sourceforge.ganttproject.task.ResourceAssignment;

public class DeleteAssignmentAction extends GPAction {
    private final AssignmentContext myContext;

    private GanttProject myProjectFrame;


    public DeleteAssignmentAction(AssignmentContext context, GanttProject projectFrame) {
        myProjectFrame = projectFrame;
        myContext = context;
    }

    public void actionPerformed(ActionEvent e) {
        myProjectFrame.getTabs().setSelectedIndex(UIFacade.RESOURCES_INDEX);
        final ResourceAssignment[] context = myContext.getResourceAssignments();
        if (context != null && context.length > 0) {
            Choice choice = myProjectFrame.getUIFacade().showConfirmationDialog(getI18n("msg23") + " "
                    + getDisplayName(context) + "?", getI18n("warning"));
            if (choice==Choice.YES) {
                myProjectFrame.getUIFacade().getUndoManager().undoableEdit("Resource removed",
                        new Runnable() {
                            public void run() {
                                deleteAssignments(context);
                                myProjectFrame.setAskForSave(true);
                                myProjectFrame.refreshProjectInfos();
                                myProjectFrame.repaint2();
                            }
                        });
            }
        }
        else {
            myProjectFrame.deleteResources();
        }
    }

    private void deleteAssignments(ResourceAssignment[] context) {
        for (int i = 0; i < context.length; i++) {
            ResourceAssignment ra = context[i];
            ra.delete();
            ra.getTask().getAssignmentCollection().deleteAssignment(
                    ra.getResource());
        }
    }

    private static String getDisplayName(Object[] objs) {
        if (objs.length == 1) {
            return objs[0].toString();
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < objs.length; i++) {
            result.append(objs[i].toString());
            if (i < objs.length - 1) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    @Override
    protected String getIconFilePrefix() {
        return "delete_";
    }

    @Override
    protected String getLocalizedName() {
        return getI18n("deleteAssignment");
    }
}
