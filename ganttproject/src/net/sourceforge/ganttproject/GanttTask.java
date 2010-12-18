/***************************************************************************
 GanttTask.java  -  description
 -------------------
 begin                : dec 2002
 copyright            : (C) 2002 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject;

import java.io.Serializable;
import net.sourceforge.ganttproject.task.TaskImpl;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskMutator;

/**
 * Class that generate a task
 */
public class GanttTask extends TaskImpl

implements Serializable {

    /**
     * @param name of the new Task
     * @param start date of the new Task
     * @param length of the new Task
     * @param taskManager to use when creating the new task
     * @param taskID contains the id to be used for the new task, or -1 to generate a unique one.
     */
    public GanttTask(String name, GanttCalendar start, long length,
            TaskManager taskManager, int taskID) {
        super(taskManager, taskID);
        TaskMutator mutator = createMutator();
        mutator.setName(name);
        mutator.setStart(start);
        mutator.setDuration(taskManager.createLength(length));
        mutator.commit();
        enableEvents(true);
    }

    /**
     * Will make a copy of the given GanttTask
     *
     * @param copy task to copy
     */
    public GanttTask(GanttTask copy) {
        super(copy, false);
        enableEvents(true);
    }

    /** @return a clone of the Task */
    public GanttTask Clone() {
        return new GanttTask(this);
    }

    /** @deprecated Use TimeUnit class instead and method getDuration() */ 
    public int getLength() {
        return (int) getDuration().getLength();
    }

    /** @deprecated Use setDuration() */
    public void setLength(int l) {
        if (l <= 0) {
            throw new IllegalArgumentException(
                    "Length of task must be >=0. You've passed length=" + l
                            + " to task=" + this);
        }
        TaskMutator mutator = createMutator();
        mutator.setDuration(getManager().createLength(
                getDuration().getTimeUnit(), l));
        mutator.commit();
    }

    /**
     * Sets the task ID. The uniqueness of ID needs to be checked before using
     * this method
     * 
     * @param taskID
     */
    public void setTaskID(int taskID) {
        setTaskIDHack(taskID);
    }    
}
