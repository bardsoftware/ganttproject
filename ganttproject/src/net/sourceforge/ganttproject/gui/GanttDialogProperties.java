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
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;

/**
 * Dialog to edit the properties of a task
 */

public class GanttDialogProperties {
    /** Boolean to say if the task has child */

	public boolean change = false;
	
    /** true if the ok button was pressed */

    static JColorChooser colorChooser = new JColorChooser();

    //private GanttTaskPropertiesBean taskPropertiesBean;

    private GanttTask[] myTasks;


    /** Constructor */

    public GanttDialogProperties(GanttTask[] tasks) {
    	myTasks = tasks;
    }
    public void show(IGanttProject project, final UIFacade uiFacade) {

//        super(parent, GanttLanguage.getInstance().getText("propertiesFor")
//                + " '" + tasksNames + "'", true);

        final GanttTaskPropertiesBean taskPropertiesBean = new GanttTaskPropertiesBean(myTasks, project, uiFacade);
        final Action[] actions = new Action[] {
        	new OkAction() {
				public void actionPerformed(ActionEvent arg0) {
					uiFacade.getUndoManager().undoableEdit("Properties changed",
	                        new Runnable() {
	                            public void run() {
	                                Task[] returnTask = taskPropertiesBean
	                                        .getReturnTask();
	                                // System.err.println("[GanttDialogProperties]
	                                // returnTask="+returnTask);
	                                // returnTask.setTaskID(this.task.getTaskID());
	                                // getTaskManager().setTask(returnTask);

//	                                DefaultMutableTreeNode father;
//	                                for (int i = 0; i < returnTask.length; i++) {
//	                                    tree.getNode(myTasks[i].getTaskID())
//	                                            .setUserObject(returnTask[i]);
//	                                    // Refresh all father
//	                                    father = tree.getFatherNode(tree
//	                                            .getNode(myTasks[i].getTaskID()));
//	                                    while (father != null) {
//	                                        tree.forwardScheduling();
//	                                        father = tree.getFatherNode(father);
//	                                    }
//	                                }
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

    /** When click on date button, it open the dialog to select date. */
//    public void actionPerformed(ActionEvent evt) {
//        if (evt.getSource() instanceof JButton) {
//
//            JButton button = (JButton) evt.getSource();
//
//            if (button.getName().equals("ok")) {
//
//                this.setVisible(false);
//                dispose();
//
//                tree.getJTree().repaint();
//
//                tree.getJTree().updateUI();
//                tree.getTable().setRowHeight(20);
//                area.repaint();
//                change = true;
//            }
//
//            else if (button.getName().equals("cancel")) {
//
//                this.setVisible(false);
//                dispose();
//
//            }
//
//        }
//
//    
//    }
//
//    private TaskManager getTaskManager() {
//        return this.task.getManager();
//    }
//
}
