/***************************************************************************
 *
 * GanttDialogProperties.java  -  description
 *
 * -------------------
 *
 * begin                : dec 2002
 *
 * copyright            : (C) 2002 by Thomas Alexandre
 *
 * email                : alexthomas(at)ganttproject.org
 *
 ***************************************************************************/

/***************************************************************************
 *
 *                                                                         *
 *
 *   This program is free software; you can redistribute it and/or modify  *
 *
 *   it under the terms of the GNU General Public License as published by  *
 *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *
 *   (at your option) any later version.                                   *
 *
 *                                                                         *
 *
 ***************************************************************************/

package net.sourceforge.ganttproject.gui;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JColorChooser;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

/**
 * Dialog to edit the properties of a task
 */

public class GanttDialogProperties {
	public boolean change = false;

    static final JColorChooser colorChooser = new JColorChooser();

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
        for (int i=0; i<myTasks.length; i++) {
        	if (i>0) {
        		taskNames.append(',');
        	}
        	taskNames.append(myTasks[i].getName());
        }
        final String title = GanttLanguage.getInstance().getText("propertiesFor")+" '"+taskNames+"'";
        uiFacade.showDialog(taskPropertiesBean, actions, title);
    }
}
