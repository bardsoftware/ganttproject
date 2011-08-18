/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 GanttProject team

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
package net.sourceforge.ganttproject.delay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.UndoableEditEvent;
import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.event.TaskDependencyEvent;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent;
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent;
import net.sourceforge.ganttproject.undo.GPUndoListener;
import net.sourceforge.ganttproject.undo.GPUndoManager;

/**
 * The DelayManager manages delays. It has all DelayObservers and notify each of
 * them when a delay has be calculated.
 *
 * @author bbaranne
 *
 */
public class DelayManager implements GPUndoListener {

    private boolean ourCriticProcess = false;

    private Date myToday;

    private List<DelayObserver> myObservers;

    private TaskManager myTaskManager;

    private Task myRootTask;

    private GanttTree2 myTree;

    public DelayManager(TaskManager taskManager, GPUndoManager undoManager, GanttTree2 tree) {
        myObservers = new ArrayList<DelayObserver>();
        myTaskManager = taskManager;
        myRootTask = (Task) ((TaskNode) tree.getRoot()).getUserObject();
        myTree = tree;
        myTaskManager.addTaskListener(new TaskListenerImpl());
        undoManager.addUndoableEditListener(this);
    }

    public void addObserver(DelayObserver observer) {
        myObservers.add(observer);
    }

    public void removeObserver(DelayObserver observer) {
        myObservers.remove(observer);
    }

    public void fireDelayObservation() {
        // System.err.println("fireDelayObservation");
        myToday = new Date();
        if (ourCriticProcess) {
            ourCriticProcess = false;
            myTaskManager.processCriticalPath(myRootTask);
            ArrayList<TaskNode> projectTasks = myTree.getProjectTasks();
            if (projectTasks.size() != 0) {
                for (DefaultMutableTreeNode projectTask: projectTasks) {
                    myTaskManager.processCriticalPath((Task) ((TaskNode) projectTask).getUserObject());
                }
            }

//            System.out.println("critical path processed");
        }
        Iterator<Task> itTasks = Arrays.asList(myTaskManager.getTasks()).iterator();
        while (itTasks.hasNext()) {
            Task task = itTasks.next();
            Delay delay = calculateDelay(task);
            Iterator<DelayObserver> itObservers = myObservers.iterator();
            while (itObservers.hasNext()) {
                DelayObserver observer = itObservers.next();
                observer.setDelay(task, delay);
//                System.out.println("delay " + delay.getType() + " (critical = "+delay.CRITICAL+")");
            }
        }
    }

    /**
     * The delay is calculated as follow : The reference date is today. The
     * check is performed on the end date of the task. There could be a delay
     * only if percentage completion is not equal to 100%. If task end date <
     * today && completion < 100 there is a delay. The delay is critical is the
     * task is critical.
     *
     * @param t
     *            The task.
     * @return The calculated delay
     */
    private Delay calculateDelay(Task t) {
        Delay res = Delay.getDelay(Delay.NONE);
        int completionPercentage = t.getCompletionPercentage();
        if (t.isMilestone() || completionPercentage == 100)
            return res;
        Date endDate = t.getEnd().getTime();
        if (endDate.before(myToday))
            if (t.isCritical())
                res.setType(Delay.CRITICAL);
            else
                res.setType(Delay.NORMAL);
        return res;
    }

    private class TaskListenerImpl extends TaskListenerAdapter {
        @Override
        public void taskScheduleChanged(TaskScheduleEvent e) {
            if (!e.getNewFinishDate().equals(e.getOldFinishDate())) {
                fireDelayObservation();
            }
        }
        @Override
        public void dependencyAdded(TaskDependencyEvent e) {
            fireDelayObservation();
        }
        @Override
        public void dependencyRemoved(TaskDependencyEvent e) {
            fireDelayObservation();
        }
        @Override
        public void taskAdded(TaskHierarchyEvent e) {
            fireDelayObservation();
        }
        @Override
        public void taskRemoved(TaskHierarchyEvent e) {
            fireDelayObservation();
        }
        @Override
        public void taskProgressChanged(TaskPropertyEvent e) {
            fireDelayObservation();
        }
    }
    public void undoOrRedoHappened() {
//        System.out.println("undoOrRedoHappened");
        ourCriticProcess = true;
        fireDelayObservation();

    }

    public void undoableEditHappened(UndoableEditEvent arg0) {
        // TODO Auto-generated method stub
    }
}
