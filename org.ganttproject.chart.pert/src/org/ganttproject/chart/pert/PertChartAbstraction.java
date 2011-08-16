/***************************************************************************
PertChartAbstraction.java - description
Copyright [2005 - ADAE]
This file is part of GanttProject].
***************************************************************************/

/***************************************************************************
 * GanttProject is free software; you can redistribute it and/or modify    *
 * it under the terms of the GNU General Public License as published by    *
 * the Free Software Foundation; either version 2 of the License, or       *
 * (at your option) any later version.                                     *
 *                                                                         *
 * GanttProject is distributed in the hope that it will be useful,         *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of          *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the           *
 * GNU General Public License for more details.                            *

***************************************************************************/

package org.ganttproject.chart.pert;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencySlice;

/**
 * This class is the abstract representation of PERT chart There are
 * TaskGraphNodes that describe a PERT chart.
 *
 * @author bbaranne
 *
 */
public class PertChartAbstraction {

    private TaskManager myTaskManager;

    private List<TaskGraphNode> myTaskGraph;

    /**
     * Creates a PertChartAbstraction, then load the data with data found in
     * <code>taskManager</code>.
     *
     * @param taskManager
     *            The task manager containing all data to build PERT chart.
     */
    public PertChartAbstraction(TaskManager taskManager) {
        myTaskManager = taskManager;
        myTaskGraph = new ArrayList<TaskGraphNode>();
        load();
    }

    /**
     * Loads data from task manager into pert chart abstraction. It creates all
     * TaskGraphNodes.
     */
    private void load() {
        Task[] tasks = myTaskManager.getTasks();
        for (int i = 0; i < tasks.length; i++) {
            Task task = tasks[i];
            TaskGraphNode tgn = getTaskGraphNode(task);
            TaskDependencySlice dependencies = task.getDependenciesAsDependee();
            TaskDependency[] relationship = dependencies.toArray();
            for (int j = 0; j < relationship.length; j++) {
                Task successor = relationship[j].getDependant();
                tgn.addSuccessor(getTaskGraphNode(successor));
            }
        }
    }

    /**
     * @param task
     *            The task from which we want the <code>TaskGraphNode</code>
     * @return The <code>TaskGraphNode</code> corresponding of the given
     *         <code>task</code>.
     */
    private TaskGraphNode getTaskGraphNode(Task task) {
        TaskGraphNode res = getTaskGraphNodeByID(task.getTaskID());
        if (res == null) {
            res = new TaskGraphNode(task);
            if (task.isMilestone()) {
                res.setType(Type.MILESTONE);
            } else if (myTaskManager.getTaskHierarchy().getNestedTasks(task).length == 0) {
                res.setType(Type.NORMAL);
            } else {
                res.setType(Type.SUPER);
            }
            myTaskGraph.add(res);
        }
        return res;
    }

    /**
     * @return The list of <code>TaskGraphNodes</code>.
     */
    public List<TaskGraphNode> getTaskGraphNodes() {
        return myTaskGraph;
    }

    /**
     * Returns the <code>TaskGraphNode</code> corresponding to the given task
     * ID.
     *
     * @param id
     *            The task ID from which we want the <code>TaskGraphNode</code>
     * @return The <code>TaskGraphNode</code> corresponding to the given task
     *         ID.
     */
    public TaskGraphNode getTaskGraphNodeByID(int id) {
        TaskGraphNode res = null;
        Iterator<TaskGraphNode> it = myTaskGraph.iterator();
        while (it.hasNext()) {
            TaskGraphNode tgn = it.next();
            if (tgn.getID() == id) {
                res = tgn;
                break;
            }
        }
        return res;
    }

    /**
     * PERT graph node abstraction
     *
     * @author bbaranne
     *
     */
    static class TaskGraphNode {

        private List<TaskGraphNode> successors;

        private int type;

        private Task myTask;

        TaskGraphNode(Task task) {
            successors = new ArrayList<TaskGraphNode>();
            myTask = task;
        }

        void setType(int type) {
            this.type = type;
        }

        int getType() {
            return this.type;
        }

        void addSuccessor(TaskGraphNode successor) {
            this.successors.add(successor);
        }

        List<TaskGraphNode> getSuccessors() {
            return this.successors;
        }

        String getName() {
            return myTask.getName();
        }

        TaskLength getDuration() {
            return myTask.getDuration();
        }

        int getID() {
            return myTask.getTaskID();
        }

        boolean isCritical() {
            return myTask.isCritical();
        }

        public String toString() {
            return "{" + getName() + ", " + getDuration()
                    + /* ", " + successors + */"}";
        }

        GregorianCalendar getEndDate() {
            return myTask.getEnd();
        }

        GregorianCalendar getStartDate() {
            return myTask.getStart();
        }
    }

    /**
     * Type of the node: NORMAL, SUPER (for super tasks) and MILESTONE.
     * @author bbaranne
     *
     */
    static class Type {
        public static final int NORMAL = 0;

        public static final int SUPER = 1;

        public static final int MILESTONE = 2;
    }
}