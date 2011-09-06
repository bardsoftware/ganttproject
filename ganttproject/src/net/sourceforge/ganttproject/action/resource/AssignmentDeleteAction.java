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

import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade.Choice;
import net.sourceforge.ganttproject.resource.AssignmentContext;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.util.StringUtils;

public class AssignmentDeleteAction extends GPAction {
    private final AssignmentContext myContext;

    private GanttProject myProject;

    public AssignmentDeleteAction(AssignmentContext context, GanttProject project) {
        super("assignment.delete");
        myProject = project;
        myContext = context;
    }

    public void actionPerformed(ActionEvent e) {
        final ResourceAssignment[] context = myContext.getResourceAssignments();
        if (context != null && context.length > 0) {
            Choice choice = myProject.getUIFacade().showConfirmationDialog(
                    getI18n("msg23") + " " + StringUtils.getDisplayNames(context) + "?", getI18n("warning"));
            if (choice == Choice.YES) {
                myProject.getUIFacade().getUndoManager().undoableEdit(getLocalizedDescription(), new Runnable() {
                    public void run() {
                        deleteAssignments(context);
                        myProject.repaint2();
                    }
                });
            }
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

    @Override
    protected String getIconFilePrefix() {
        return "delete_";
    }
}
