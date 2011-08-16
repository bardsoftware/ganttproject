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
package net.sourceforge.ganttproject.gui;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class GanttDialogProperties {
    private final GanttTask[] myTasks;

    public GanttDialogProperties(GanttTask[] tasks) {
        myTasks = tasks;
    }

    public void show(final IGanttProject project, final UIFacade uiFacade) {
        final GanttTaskPropertiesBean taskPropertiesBean = new GanttTaskPropertiesBean(myTasks, project, uiFacade);
        final Action[] actions = new Action[] {
            new OkAction() {
                public void actionPerformed(ActionEvent arg0) {
                    uiFacade.getUndoManager().undoableEdit("Properties changed",
                            new Runnable() {
                                public void run() {
                                    taskPropertiesBean.getReturnTask();
                                    try {
                                        project.getTaskManager().getAlgorithmCollection()
                                        .getRecalculateTaskScheduleAlgorithm().run();
                                    } catch (TaskDependencyException e) {
                                        if (!GPLogger.log(e)) {
                                            e.printStackTrace();
                                        }
                                    }
                                    uiFacade.refresh();
                                }
                            });
                }
            },
            new CancelAction() {
                public void actionPerformed(ActionEvent arg0) {
                }
            }
        };
        StringBuffer taskNames = new StringBuffer();
        for (int i = 0; i < myTasks.length; i++) {
            if (i > 0) {
                taskNames.append(',');
            }
            taskNames.append(myTasks[i].getName());
        }
        String title = GanttLanguage.getInstance().getText("propertiesFor")+" '"+taskNames+"'";
        uiFacade.createDialog(taskPropertiesBean, actions, title).show();
    }
}
