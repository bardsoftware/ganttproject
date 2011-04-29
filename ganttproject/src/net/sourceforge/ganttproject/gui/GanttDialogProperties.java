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
        String title = GanttLanguage.getInstance().getText("propertiesFor")+" '"+taskNames+"'";
        uiFacade.createDialog(taskPropertiesBean, actions, title).show();
    }
}
