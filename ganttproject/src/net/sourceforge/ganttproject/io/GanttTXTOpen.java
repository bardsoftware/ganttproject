/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.task.TaskManager;

public class GanttTXTOpen {
  private final TaskManager myTaskManager;

  public GanttTXTOpen(TaskManager taskManager) {
    myTaskManager = taskManager;
  }

  /** Load tasks list from a text file. */
  public boolean load(File f) {
    try {
      // Open a stream
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f)));

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
