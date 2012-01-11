/***************************************************************************
 GanttTXTOpen  -  A class for open a list of task from a txt file
 -------------------
 begin                : jun 2004
 copyright            : (C) 2004 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 3 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * @author athomas This class allows to open a list of task from a simple txt
 *         file. Each line of the file are insert as a new task with the name as
 *         text of the line.
 */
public class GanttTXTOpen {
    private final TaskManager myTaskManager;

    public GanttTXTOpen(TaskManager taskManager) {
        myTaskManager = taskManager;
    }

    /** Load tasks list from a text file. */
    public boolean load(File f) {
        try {
            // Open a stream
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f)));

            while (br.ready()) {
                // Read each lines
                String sTaskName = br.readLine();

                // The test is used to skip the white line (with no text)
                if (!sTaskName.equals("")) {
                    // Create the task
                    GanttTask task = myTaskManager.createTask();
                    task.setName(sTaskName);
                    task.setLength(1);
                    myTaskManager.registerTask(task);
                    myTaskManager.getTaskHierarchy().move(task, myTaskManager.getRootTask());
                }
            }

        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
